package org.polaris2023.gtu.physics.world;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.polaris2023.gtu.physics.collision.VoxelShapeConverter;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.init.DataComponents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物理管理器
 * <p>
 * 管理所有维度的物理世界，同步 Minecraft 实体与 Bullet 物理引擎
 */
public class PhysicsManager {

    private static final Map<Level, PhysicsWorld> worldMap = new ConcurrentHashMap<>();

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
        // 从 Level attachment 获取物理配置
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

    /**
     * 为实体创建刚体
     */
    public static void createEntityBody(Entity entity) {
        Level level = entity.level();
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        CollisionShape shape = createEntityShape(entity);
        float mass = calculateEntityMass(entity);

        Vector3f position = new Vector3f(
                (float) entity.getX(),
                (float) entity.getY(),
                (float) entity.getZ()
        );

        // Y轴旋转角度转换为弧度
        float yRot = (float) Math.toRadians(entity.getYRot());

        physicsWorld.addEntityBody(entity.getId(), shape, mass, position, yRot);
    }

    /**
     * 为实体创建碰撞形状
     */
    public static CollisionShape createEntityShape(Entity entity) {
        AABB bb = entity.getBoundingBox();
        double width = bb.getXsize();
        double height = bb.getYsize();

        if (entity instanceof Player) {
            // 玩家使用胶囊形状
            float radius = (float) (width / 2.0);
            float heightWithoutRadius = (float) (height - width);
            return new CapsuleCollisionShape(radius, heightWithoutRadius);
        } else {
            // 其他实体使用盒子形状
            return VoxelShapeConverter.createBoxShape(
                    (float) (width / 2.0),
                    (float) (height / 2.0),
                    (float) (bb.getZsize() / 2.0)
            );
        }
    }

    /**
     * 计算实体质量
     */
    public static float calculateEntityMass(Entity entity) {
        // 基础质量计算，可以根据实体类型调整
        float baseMass = 70.0f; // 默认 70kg (成人平均体重)

        if (entity instanceof Player) {
            return baseMass;
        }

        // 根据实体碰撞箱体积估算质量
        AABB bb = entity.getBoundingBox();
        double volume = bb.getXsize() * bb.getYsize() * bb.getZsize();
        return (float) (volume * PhysicsConstants.KG_PER_MASS_UNIT);
    }

    /**
     * 同步实体位置到物理世界
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

    /**
     * 添加方块碰撞体（使用 VoxelShape 转换，兼容所有 mod）
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
     * 更新方块碰撞体（方块放置/改变时调用）
     */
    public static void updateBlockCollision(Level level, BlockPos pos, BlockState state) {
        PhysicsWorld physicsWorld = getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        // 先移除旧的碰撞体
        physicsWorld.removeBlockBody(pos.asLong());

        // 添加新的碰撞体
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
        // 初始化 Bullet 物理引擎
        // Native 库会在第一次使用时自动加载
    }
}
