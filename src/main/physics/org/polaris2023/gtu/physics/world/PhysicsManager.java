package org.polaris2023.gtu.physics.world;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.polaris2023.gtu.physics.collision.VoxelShapeConverter;
import org.polaris2023.gtu.physics.init.DataAttachments;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 物理管理器
 * <p>
 * 管理所有维度的物理世界，同步 Minecraft 实体与 Bullet 物理引擎
 * <p>
 * <b>线程安全优化：</b>
 * <ul>
 *   <li>使用 ConcurrentHashMap 存储物理世界</li>
 *   <li>实体刚体创建使用延迟队列</li>
 *   <li>形状缓存避免重复创建</li>
 * </ul>
 */
public class PhysicsManager {

    private static final Map<Level, PhysicsWorld> worldMap = new ConcurrentHashMap<>();

    // ==================== 形状缓存 ====================

    /**
     * 玩家胶囊形状缓存（半径, 高度 -> 形状）
     */
    private static final Map<Long, CapsuleCollisionShape> playerShapeCache = new ConcurrentHashMap<>();

    /**
     * 实体盒子形状缓存（半宽, 半高, 半深 -> 形状）
     */
    private static final Map<Long, BoxCollisionShape> boxShapeCache = new ConcurrentHashMap<>();

    /**
     * 形状缓存最大大小
     */
    private static final int MAX_SHAPE_CACHE = 256;

    // ==================== 延迟创建队列 ====================

    /**
     * 待创建刚体的实体队列
     */
    private static final ConcurrentLinkedQueue<Entity> pendingEntityCreations = new ConcurrentLinkedQueue<>();

    /**
     * 每帧最大创建数
     */
    private static final int MAX_CREATIONS_PER_FRAME = 32;

    // ==================== 世界管理 ====================

    /**
     * 获取或创建维度的物理世界
     */
    public static PhysicsWorld getOrCreatePhysicsWorld(Level level) {
        return worldMap.computeIfAbsent(level, PhysicsManager::createPhysicsWorld);
    }

    /**
     * 创建物理世界
     */
    private static PhysicsWorld createPhysicsWorld(Level level) {
        DimensionPhysics config = level.getData(DataAttachments.DIMENSION_PHYSICS.get());
        return new PhysicsWorld(config);
    }

    /**
     * 获取维度的物理配置
     */
    public static DimensionPhysics getDimensionPhysics(Level level) {
        PhysicsWorld world = worldMap.get(level);
        if (world != null) {
            return world.getDimensionPhysics();
        }
        return level.getData(DataAttachments.DIMENSION_PHYSICS.get());
    }

    // ==================== 实体刚体管理 ====================

    /**
     * 为实体创建刚体（延迟）
     * <p>
     * 将实体加入延迟队列，在下一帧批量创建
     */
    public static void createEntityBodyDeferred(Entity entity) {
        pendingEntityCreations.add(entity);
    }

    /**
     * 处理待创建的实体刚体（每帧调用）
     */
    public static void processPendingCreations() {
        int created = 0;
        Entity entity;

        while ((entity = pendingEntityCreations.poll()) != null && created < MAX_CREATIONS_PER_FRAME) {
            createEntityBodyImmediate(entity);
            created++;
        }
    }

    /**
     * 立即为实体创建刚体
     */
    public static void createEntityBody(Entity entity) {
        createEntityBodyImmediate(entity);
    }

    /**
     * 内部方法：立即创建刚体
     */
    private static void createEntityBodyImmediate(Entity entity) {
        Level level = entity.level();
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        CollisionShape shape = getOrCreateEntityShape(entity);
        float mass = calculateEntityMass(entity);

        Vector3f position = new Vector3f(
                (float) entity.getX(),
                (float) entity.getY(),
                (float) entity.getZ()
        );

        float yRot = (float) Math.toRadians(entity.getYRot());
        physicsWorld.addEntityBody(entity.getId(), shape, mass, position, yRot);
    }

    // ==================== 形状缓存 ====================

    /**
     * 获取或创建实体碰撞形状（带缓存）
     */
    private static CollisionShape getOrCreateEntityShape(Entity entity) {
        AABB bb = entity.getBoundingBox();
        float width = (float) bb.getXsize();
        float height = (float) bb.getYsize();
        float depth = (float) bb.getZsize();

        if (entity instanceof Player) {
            // 玩家使用胶囊形状
            float radius = width / 2.0f;
            float heightWithoutRadius = height - width;

            // 缓存键
            long key = Float.floatToRawIntBits(radius) * 31L + Float.floatToRawIntBits(heightWithoutRadius);

            return playerShapeCache.computeIfAbsent(key, k -> {
                if (playerShapeCache.size() >= MAX_SHAPE_CACHE) {
                    playerShapeCache.clear();
                }
                return new CapsuleCollisionShape(radius, heightWithoutRadius);
            });
        } else {
            // 其他实体使用盒子形状
            float halfWidth = width / 2.0f;
            float halfHeight = height / 2.0f;
            float halfDepth = depth / 2.0f;

            // 缓存键
            long key = Float.floatToRawIntBits(halfWidth) * 31L * 31L
                     + Float.floatToRawIntBits(halfHeight) * 31L
                     + Float.floatToRawIntBits(halfDepth);

            return boxShapeCache.computeIfAbsent(key, k -> {
                if (boxShapeCache.size() >= MAX_SHAPE_CACHE) {
                    boxShapeCache.clear();
                }
                return new BoxCollisionShape(halfWidth, halfHeight, halfDepth);
            });
        }
    }

    /**
     * 计算实体质量
     */
    public static float calculateEntityMass(Entity entity) {
        float baseMass = 70.0f;

        if (entity instanceof Player) {
            return baseMass;
        }

        AABB bb = entity.getBoundingBox();
        double volume = bb.getXsize() * bb.getYsize() * bb.getZsize();
        return (float) (volume * PhysicsConstants.KG_PER_MASS_UNIT);
    }

    // ==================== 位置同步 ====================

    /**
     * 批量同步：将 MC 实体位置/旋转/速度写入 Bullet 刚体
     */
    public static void syncAllEntitiesToPhysics(ServerLevel level) {
        PhysicsWorld physicsWorld = worldMap.get(level);
        if (physicsWorld == null || physicsWorld.isDestroyed()) return;

        for (Entity entity : level.getAllEntities()) {
            if (entity.level().isClientSide()) continue;

            PhysicsRigidBody body = physicsWorld.getEntityBody(entity.getId());
            if (body == null) continue;
            if (body.getMass() <= 0) continue;  // 静态体不更新

            // 位置
            body.setPhysicsLocation(new Vector3f(
                    (float) entity.getX(),
                    (float) entity.getY(),
                    (float) entity.getZ()
            ));

            // 旋转（Y 轴朝向）
            float yRotRad = (float) Math.toRadians(entity.getYRot());
            body.setPhysicsRotation(new com.jme3.math.Quaternion().fromAngles(0, yRotRad, 0));

            // 速度
            net.minecraft.world.phys.Vec3 mcVel = entity.getDeltaMovement();
            body.setLinearVelocity(new Vector3f(
                    (float) mcVel.x,
                    (float) mcVel.y,
                    (float) mcVel.z
            ));
        }
    }

    /**
     * 批量同步：将 Bullet 碰撞产生的速度变化写回 MC 实体
     * <p>
     * 不同步位置（MC 自己处理重力和方块碰撞），
     * 只同步 Bullet 碰撞冲量导致的速度变化。
     */
    public static void syncAllPhysicsToEntities(ServerLevel level) {
        PhysicsWorld physicsWorld = worldMap.get(level);
        if (physicsWorld == null || physicsWorld.isDestroyed()) return;

        for (Entity entity : level.getAllEntities()) {
            if (entity.level().isClientSide()) continue;

            PhysicsRigidBody body = physicsWorld.getEntityBody(entity.getId());
            if (body == null) continue;
            if (body.getMass() <= 0) continue;

            Vector3f bulletVel = body.getLinearVelocity(null);
            net.minecraft.world.phys.Vec3 mcVel = entity.getDeltaMovement();

            // 计算碰撞冲量导致的速度差
            float dx = bulletVel.x - (float) mcVel.x;
            float dy = bulletVel.y - (float) mcVel.y;
            float dz = bulletVel.z - (float) mcVel.z;

            // 只有速度差足够大（说明发生了碰撞）才应用
            double deltaSq = dx * dx + dy * dy + dz * dz;
            if (deltaSq > 0.001) {
                entity.setDeltaMovement(
                        mcVel.x + dx,
                        mcVel.y + dy,
                        mcVel.z + dz
                );
            }
        }
    }

    /**
     * 同步单个实体位置到物理世界
     */
    public static void syncEntityToPhysics(Entity entity) {
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(entity.level());
        if (physicsWorld == null) return;

        var body = physicsWorld.getEntityBody(entity.getId());
        if (body != null) {
            body.setPhysicsLocation(new Vector3f(
                    (float) entity.getX(),
                    (float) entity.getY(),
                    (float) entity.getZ()
            ));
            float yRotRad = (float) Math.toRadians(entity.getYRot());
            body.setPhysicsRotation(new com.jme3.math.Quaternion().fromAngles(0, yRotRad, 0));

            net.minecraft.world.phys.Vec3 mcVel = entity.getDeltaMovement();
            body.setLinearVelocity(new Vector3f((float) mcVel.x, (float) mcVel.y, (float) mcVel.z));
        }
    }

    /**
     * 同步物理世界到实体
     */
    public static void syncPhysicsToEntity(Entity entity) {
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(entity.level());
        if (physicsWorld == null) return;

        var body = physicsWorld.getEntityBody(entity.getId());
        if (body != null) {
            Vector3f pos = body.getPhysicsLocation(null);
            Vector3f vel = body.getLinearVelocity(null);

            entity.setPos(pos.x, pos.y, pos.z);
            entity.setDeltaMovement(vel.x, vel.y, vel.z);
        }
    }

    // ==================== 方块碰撞 ====================

    /**
     * 添加方块碰撞体
     */
    public static void addBlockCollision(Level level, BlockPos pos) {
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        BlockState state = level.getBlockState(pos);
        CollisionShape shape = VoxelShapeConverter.convert(level, pos, state);
        if (shape == null) return;

        physicsWorld.addBlockBody(pos.asLong(), shape, new Vector3f(
                pos.getX() + 0.5f,
                pos.getY() + 0.5f,
                pos.getZ() + 0.5f
        ));
    }

    /**
     * 更新方块碰撞体
     */
    public static void updateBlockCollision(Level level, BlockPos pos, BlockState state) {
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        physicsWorld.removeBlockBody(pos.asLong());

        CollisionShape shape = VoxelShapeConverter.convert(level, pos, state);
        if (shape == null) return;

        physicsWorld.addBlockBody(pos.asLong(), shape, new Vector3f(
                pos.getX() + 0.5f,
                pos.getY() + 0.5f,
                pos.getZ() + 0.5f
        ));
    }

    /**
     * 移除方块碰撞体
     */
    public static void removeBlockCollision(Level level, BlockPos pos) {
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        physicsWorld.removeBlockBody(pos.asLong());
    }

    // ==================== 物理更新 ====================

    /**
     * 更新物理世界 (每 tick 调用)
     */
    public static void tickPhysics(Level level) {
        PhysicsWorld physicsWorld = worldMap.get(level);
        if (physicsWorld != null) {
            physicsWorld.step(PhysicsConstants.SECONDS_PER_TICK);
        }
    }

    /**
     * 清理维度的物理世界
     */
    public static void unloadPhysicsWorld(Level level) {
        PhysicsWorld physicsWorld = worldMap.remove(level);
        if (physicsWorld != null) {
            physicsWorld.destroy();
        }
    }

    /**
     * 初始化服务器物理系统
     */
    public static void initServerPhysics() {
        // 清理缓存
        playerShapeCache.clear();
        boxShapeCache.clear();
        pendingEntityCreations.clear();
    }

    /**
     * 关闭物理系统
     */
    public static void shutdown() {
        // 清理所有世界
        for (PhysicsWorld world : worldMap.values()) {
            world.destroy();
        }
        worldMap.clear();

        // 清理缓存
        playerShapeCache.clear();
        boxShapeCache.clear();
        pendingEntityCreations.clear();
    }

    // ==================== 统计 ====================

    /**
     * 获取物理世界数量
     */
    public static int getWorldCount() {
        return worldMap.size();
    }

    /**
     * 获取待创建实体数量
     */
    public static int getPendingCreationCount() {
        return pendingEntityCreations.size();
    }

    /**
     * 获取形状缓存大小
     */
    public static int getShapeCacheSize() {
        return playerShapeCache.size() + boxShapeCache.size();
    }
}
