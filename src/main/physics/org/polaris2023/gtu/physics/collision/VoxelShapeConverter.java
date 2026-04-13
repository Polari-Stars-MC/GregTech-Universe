package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VoxelShape 转换为 Bullet CollisionShape
 * <p>
 * 兼容所有 mod 的方块碰撞箱
 * <p>
 * <b>优化策略：</b>
 * <ul>
 *   <li>缓存常用形状（完整方块、半砖等）</li>
 *   <li>形状创建可以在任意线程进行（不依赖 PhysicsSpace）</li>
 *   <li>支持异步预计算</li>
 * </ul>
 */
public final class VoxelShapeConverter {

    private VoxelShapeConverter() {
    }

    // ==================== 形状缓存 ====================

    /**
     * 完整方块形状（最常用，单例）
     */
    private static final BoxCollisionShape FULL_BLOCK_SHAPE = new BoxCollisionShape(0.5f, 0.5f, 0.5f);

    /**
     * 形状缓存 Key: 方块状态 ID, Value: 碰撞形状
     * <p>
     * 使用 ConcurrentHashMap 支持并发访问
     */
    private static final Map<Integer, CollisionShape> SHAPE_CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存大小限制
     */
    private static final int MAX_CACHE_SIZE = 4096;

    /**
     * 缓存命中计数（用于调试）
     */
    private static long cacheHits = 0;

    /**
     * 缓存未命中计数（用于调试）
     */
    private static long cacheMisses = 0;

    // ==================== 预定义形状 ====================

    /**
     * 常用半尺寸形状
     */
    private static final BoxCollisionShape HALF_BLOCK_SHAPE = new BoxCollisionShape(0.5f, 0.25f, 0.5f);

    /**
     * 小型形状（按钮、压力板等）
     */
    private static final BoxCollisionShape SMALL_SHAPE = new BoxCollisionShape(0.25f, 0.125f, 0.25f);

    // ==================== 主转换方法 ====================

    /**
     * 将方块的 VoxelShape 转换为 Bullet CollisionShape
     * <p>
     * 使用标准 API 获取碰撞箱，兼容所有 mod
     * <p>
     * <b>线程安全：</b>此方法可以在任意线程调用
     *
     * @param level    世界
     * @param pos      方块位置
     * @param state    方块状态
     * @return CollisionShape，如果方块无碰撞返回 null
     */
    public static CollisionShape convert(BlockGetter level, BlockPos pos, BlockState state) {
        // 使用标准 API 获取碰撞箱，兼容所有 mod
        VoxelShape voxelShape = state.getCollisionShape(level, pos);

        // 空碰撞箱
        if (voxelShape.isEmpty()) {
            return null;
        }

        // 完整方块优化（最常见情况）
        if (voxelShape.equals(Shapes.block())) {
            return FULL_BLOCK_SHAPE;
        }

        // 尝试从缓存获取
        int stateId = getStateId(state);
        CollisionShape cached = SHAPE_CACHE.get(stateId);
        if (cached != null) {
            cacheHits++;
            return cached;
        }

        cacheMisses++;

        // 创建新形状
        CollisionShape shape = createShape(voxelShape);
        if (shape != null && SHAPE_CACHE.size() < MAX_CACHE_SIZE) {
            SHAPE_CACHE.put(stateId, shape);
        }

        return shape;
    }

    /**
     * 异步预计算方块碰撞形状
     * <p>
     * 可以在工作线程调用，预先计算形状并缓存
     *
     * @param state 方块状态
     * @return 碰撞形状，如果已缓存返回缓存的形状
     */
    public static CollisionShape precomputeShape(BlockState state) {
        int stateId = getStateId(state);

        // 检查缓存
        CollisionShape cached = SHAPE_CACHE.get(stateId);
        if (cached != null) {
            return cached;
        }

        // 获取 VoxelShape（注意：这需要访问世界，在异步线程可能不安全）
        // 所以这个方法主要用于已知 VoxelShape 的情况
        return null;
    }

    /**
     * 从 VoxelShape 创建碰撞形状
     * <p>
     * <b>线程安全：</b>此方法可以在任意线程调用
     *
     * @param voxelShape VoxelShape
     * @return CollisionShape
     */
    public static CollisionShape createShapeFromVoxel(VoxelShape voxelShape) {
        if (voxelShape.isEmpty()) {
            return null;
        }

        // 完整方块优化
        if (voxelShape.equals(Shapes.block())) {
            return FULL_BLOCK_SHAPE;
        }

        return createShape(voxelShape);
    }

    // ==================== 内部方法 ====================

    /**
     * 创建碰撞形状
     */
    private static CollisionShape createShape(VoxelShape voxelShape) {
        // 提取所有 AABB
        List<AABBInfo> boxes = extractAABBs(voxelShape);

        if (boxes.isEmpty()) {
            return null;
        }

        // 单个 AABB
        if (boxes.size() == 1) {
            return createBoxShape(boxes.getFirst());
        }

        // 多个 AABB：创建复合碰撞形状
        return createCompoundShape(boxes);
    }

    /**
     * 从 VoxelShape 提取所有 AABB
     */
    private static List<AABBInfo> extractAABBs(VoxelShape voxelShape) {
        List<AABBInfo> boxes = new ArrayList<>();

        voxelShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                boxes.add(new AABBInfo(minX, minY, minZ, maxX, maxY, maxZ)));

        return boxes;
    }

    /**
     * 创建单个 BoxCollisionShape
     */
    private static BoxCollisionShape createBoxShape(AABBInfo box) {
        float halfWidth = (float) ((box.maxX - box.minX) / 2.0);
        float halfHeight = (float) ((box.maxY - box.minY) / 2.0);
        float halfDepth = (float) ((box.maxZ - box.minZ) / 2.0);
        return new BoxCollisionShape(halfWidth, halfHeight, halfDepth);
    }

    /**
     * 创建指定尺寸的 BoxCollisionShape
     *
     * @param halfWidth  半宽
     * @param halfHeight 半高
     * @param halfDepth  半深
     * @return BoxCollisionShape
     */
    public static BoxCollisionShape createBoxShape(float halfWidth, float halfHeight, float halfDepth) {
        return new BoxCollisionShape(halfWidth, halfHeight, halfDepth);
    }

    /**
     * 创建复合碰撞形状
     */
    private static CollisionShape createCompoundShape(List<AABBInfo> boxes) {
        CompoundCollisionShape compound = new CompoundCollisionShape(boxes.size());

        for (AABBInfo box : boxes) {
            BoxCollisionShape boxShape = createBoxShape(box);

            // 计算中心偏移（相对于方块中心）
            float centerX = (float) ((box.minX + box.maxX) / 2.0 - 0.5);
            float centerY = (float) ((box.minY + box.maxY) / 2.0 - 0.5);
            float centerZ = (float) ((box.minZ + box.maxZ) / 2.0 - 0.5);

            compound.addChildShape(boxShape, new Vector3f(centerX, centerY, centerZ));
        }

        return compound;
    }

    /**
     * 获取方块状态 ID（用于缓存键）
     */
    private static int getStateId(BlockState state) {
        // 使用 BlockState 的内部 ID（如果可用）
        // 否则使用 hashCode
        return state.hashCode();
    }

    // ==================== 缓存管理 ====================

    /**
     * 清理形状缓存
     */
    public static void clearCache() {
        SHAPE_CACHE.clear();
    }

    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return SHAPE_CACHE.size();
    }

    /**
     * 获取缓存命中率
     */
    public static float getCacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total > 0 ? (float) cacheHits / total : 0;
    }

    /**
     * 重置统计
     */
    public static void resetStats() {
        cacheHits = 0;
        cacheMisses = 0;
    }

    // ==================== 数据类 ====================

    /**
     * AABB 信息
     */
    private record AABBInfo(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }
}
