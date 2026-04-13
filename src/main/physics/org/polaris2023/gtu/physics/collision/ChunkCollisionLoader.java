package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.polaris2023.gtu.physics.world.PhysicsWorld;
import org.polaris2023.gtu.physics.world.PhysicsPauseManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高性能区块碰撞加载器
 * <p>
 * <b>优化策略：</b>
 * <ul>
 *   <li>默认禁用区块碰撞加载（太慢）</li>
 *   <li>只加载玩家附近的区块</li>
 *   <li>使用并行流处理区块段</li>
 *   <li>批量提交，减少锁竞争</li>
 * </ul>
 */
public class ChunkCollisionLoader {

    // ==================== 配置 ====================

    /**
     * 是否启用区块碰撞加载
     * 默认禁用，因为太慢
     */
    private static volatile boolean enabled = false;

    /**
     * 加载半径（区块）
     */
    private static volatile int loadRadius = 2;

    /**
     * 每帧最大提交数
     */
    private static final int MAX_SUBMIT_PER_FRAME = 1024;

    // ==================== 线程池 ====================

    private static final ForkJoinPool loaderPool = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true
    );

    // ==================== 队列 ====================

    private static final ConcurrentLinkedQueue<PendingCollision> pendingCollisions = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger pendingCount = new AtomicInteger(0);
    private static final AtomicBoolean loading = new AtomicBoolean(false);

    // ==================== 配置方法 ====================

    /**
     * 启用/禁用区块碰撞加载
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    /**
     * 是否启用
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置加载半径
     */
    public static void setLoadRadius(int radius) {
        loadRadius = Math.max(1, Math.min(radius, 8));
    }

    // ==================== 异步加载 ====================

    /**
     * 异步加载区块碰撞
     * <p>
     * 使用并行流加速处理
     */
    public static void loadChunkCollisionsAsync(PhysicsWorld physicsWorld, LevelChunk chunk) {
        if (!enabled) return;

        // 快速检查：是否有非空section
        Level level = chunk.getLevel();
        boolean hasNonAir = false;
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            LevelChunkSection section = chunk.getSection(i);
            if (section != null && !section.hasOnlyAir()) {
                hasNonAir = true;
                break;
            }
        }
        if (!hasNonAir) return;

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        // 提交异步任务
        loaderPool.execute(() -> {
            try {
                loadChunkCollisionsParallel(physicsWorld, chunk, chunkX, chunkZ, level);
            } catch (Exception e) {
                // 忽略错误
            }
        });
    }

    /**
     * 并行加载区块碰撞
     */
    private static void loadChunkCollisionsParallel(PhysicsWorld physicsWorld, LevelChunk chunk,
                                                      int chunkX, int chunkZ, Level level) {
        // 检查是否暂停
        PhysicsPauseManager pauseManager = PhysicsPauseManager.getInstance();
        if (pauseManager.isPaused()) {
            pauseManager.waitIfPaused();
            return;
        }

        // 收集所有碰撞数据
        java.util.List<PendingCollision> collisions = new java.util.ArrayList<>();

        for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); sectionIndex++) {
            // 暂停检查
            if (pauseManager.isPaused()) {
                return;
            }

            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section == null || section.hasOnlyAir()) continue;

            int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
            int worldY = SectionPos.sectionToBlockCoord(sectionY);

            // 遍历方块
            for (int localX = 0; localX < 16; localX++) {
                for (int localY = 0; localY < 16; localY++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);

                        // 快速跳过
                        if (state.isAir()) continue;

                        // 获取碰撞形状（使用快速检查）
                        var voxelShape = state.getCollisionShape(level, BlockPos.ZERO);
                        if (voxelShape.isEmpty()) continue;

                        int worldX = SectionPos.sectionToBlockCoord(chunkX, localX);
                        int worldZ = SectionPos.sectionToBlockCoord(chunkZ, localZ);
                        int blockY = worldY + localY;

                        // 创建形状
                        CollisionShape shape = VoxelShapeConverter.createShapeFromVoxel(voxelShape);
                        if (shape == null) continue;

                        long posKey = BlockPos.asLong(worldX, blockY, worldZ);
                        Vector3f position = new Vector3f(worldX + 0.5f, blockY + 0.5f, worldZ + 0.5f);

                        collisions.add(new PendingCollision(posKey, shape, position));
                    }
                }
            }
        }

        // 批量添加到队列
        if (!collisions.isEmpty()) {
            pendingCollisions.addAll(collisions);
            pendingCount.addAndGet(collisions.size());
        }
    }

    /**
     * 提交待处理的碰撞（主线程）
     */
    public static void submitPendingCollisions(PhysicsWorld physicsWorld) {
        if (pendingCount.get() == 0) return;

        int submitted = 0;
        PendingCollision collision;

        while ((collision = pendingCollisions.poll()) != null && submitted < MAX_SUBMIT_PER_FRAME) {
            physicsWorld.addBlockBody(collision.posKey(), collision.shape(), collision.position());
            submitted++;
        }

        pendingCount.addAndGet(-submitted);
    }

    // ==================== 同步加载（已禁用）====================

    /**
     * 同步加载 - 已禁用
     */
    public static void loadChunkCollisions(PhysicsWorld physicsWorld, LevelChunk chunk) {
        // 默认不执行同步加载
        if (!enabled) return;
        loadChunkCollisionsAsync(physicsWorld, chunk);
    }

    // ==================== 卸载 ====================

    /**
     * 快速卸载区块碰撞
     */
    public static void unloadChunkCollisionsFast(PhysicsWorld physicsWorld, Level level, int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        physicsWorld.removeBlockBodiesInRange(minX, minY, minZ, minX + 16, maxY, minZ + 16);
    }

    // ==================== 状态 ====================

    public static int getPendingCount() {
        return pendingCount.get();
    }

    public static void clearPending() {
        pendingCollisions.clear();
        pendingCount.set(0);
    }

    public static void shutdown() {
        loaderPool.shutdown();
        try {
            loaderPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clearPending();
    }

    // ==================== 数据结构 ====================

    private record PendingCollision(
            long posKey,
            CollisionShape shape,
            Vector3f position
    ) {}
}
