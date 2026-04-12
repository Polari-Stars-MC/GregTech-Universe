package org.polaris2023.gtu.physics.world;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bullet 物理世界管理器
 * <p>
 * 管理单个维度/世界的物理模拟
 */
public class PhysicsWorld implements PhysicsTickListener {

    private final PhysicsSpace physicsSpace;
    private final Map<Integer, PhysicsRigidBody> entityBodies = new ConcurrentHashMap<>();
    private final Map<Long, PhysicsRigidBody> blockBodies = new ConcurrentHashMap<>();

    /**
     * 当前维度的物理配置
     */
    private final DimensionPhysics dimensionPhysics;

//    /**
//     * 初始化 Bullet 原生库
//     */
//    public static synchronized void initNative() {
//        if (!nativeInitialized) {
//            NativeLibrary.initialize();
//            nativeInitialized = true;
//        }
//    } //我们已经初始化过了

    /**
     * 创建物理世界
     *
     * @param dimensionPhysics 维度物理配置
     */
    public PhysicsWorld(DimensionPhysics dimensionPhysics) {
        this.dimensionPhysics = dimensionPhysics;

        // 创建物理空间，使用动态 AABB 检测
        this.physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);

        // 设置维度重力
        this.physicsSpace.setGravity(new Vector3f(0, -dimensionPhysics.gravity, 0));

        // 添加 tick 监听器
        this.physicsSpace.addTickListener(this);
    }

    /**
     * 创建默认物理世界 (使用默认配置)
     */
    public PhysicsWorld() {
        this(DimensionPhysics.DEFAULT);
    }

    /**
     * 获取维度物理配置
     */
    public DimensionPhysics getDimensionPhysics() {
        return dimensionPhysics;
    }

    /**
     * 添加实体刚体
     *
     * @param entityId    实体 ID
     * @param shape       碰撞形状
     * @param mass        质量 (kg)
     * @param position    初始位置
     * @param yRot        Y轴旋转角度 (弧度)
     */
    public PhysicsRigidBody addEntityBody(int entityId, CollisionShape shape, float mass,
                                          Vector3f position, float yRot) {
        PhysicsRigidBody body = new PhysicsRigidBody(shape, mass);
        body.setPhysicsLocation(position);
        // 使用四元数设置Y轴旋转
        body.setPhysicsRotation(new Quaternion().fromAngles(0, yRot, 0));
        physicsSpace.addCollisionObject(body);
        entityBodies.put(entityId, body);
        return body;
    }

    /**
     * 移除实体刚体
     */
    public void removeEntityBody(int entityId) {
        PhysicsRigidBody body = entityBodies.remove(entityId);
        if (body != null) {
            physicsSpace.removeCollisionObject(body);
            // PhysicsRigidBody 由 Java GC 自动回收
        }
    }

    /**
     * 获取实体刚体
     */
    public PhysicsRigidBody getEntityBody(int entityId) {
        return entityBodies.get(entityId);
    }

    /**
     * 添加静态方块碰撞体
     */
    public PhysicsRigidBody addBlockBody(long blockKey, CollisionShape shape, Vector3f position) {
        PhysicsRigidBody body = new PhysicsRigidBody(shape, 0.0f); // 静态物体质量为0
        body.setPhysicsLocation(position);
        physicsSpace.addCollisionObject(body);
        blockBodies.put(blockKey, body);
        return body;
    }

    /**
     * 移除方块碰撞体
     */
    public void removeBlockBody(long blockKey) {
        PhysicsRigidBody body = blockBodies.remove(blockKey);
        if (body != null) {
            physicsSpace.removeCollisionObject(body);
            // PhysicsRigidBody 由 Java GC 自动回收
        }
    }

    /**
     * 批量移除指定范围内的方块碰撞体
     *
     * @param minX 最小 X
     * @param minY 最小 Y
     * @param minZ 最小 Z
     * @param maxX 最大 X
     * @param maxY 最大 Y
     * @param maxZ 最大 Z
     */
    public void removeBlockBodiesInRange(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Iterator<Map.Entry<Long, PhysicsRigidBody>> iterator = blockBodies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PhysicsRigidBody> entry = iterator.next();
            long key = entry.getKey();

            // 解码方块坐标
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);

            // 检查是否在范围内
            if (x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ) {
                physicsSpace.removeCollisionObject(entry.getValue());
                iterator.remove();
            }
        }
    }

    /**
     * 方块位置解码辅助类
     */
    private static class BlockPos {
        static int getX(long packedPos) {
            return (int) (packedPos >> 38);
        }

        static int getY(long packedPos) {
            return (int) (packedPos << 52 >> 52);
        }

        static int getZ(long packedPos) {
            return (int) (packedPos << 26 >> 38);
        }
    }

    /**
     * 步进物理模拟
     *
     * @param timeStep 时间步长 (秒)
     */
    public void step(float timeStep) {
        physicsSpace.update(timeStep, 1);
    }

    /**
     * 获取 Bullet 物理空间
     */
    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    /**
     * 设置重力
     */
    public void setGravity(float x, float y, float z) {
        physicsSpace.setGravity(new Vector3f(x, y, z));
    }

    /**
     * 清理所有物理对象
     */
    public void destroy() {
        for (PhysicsRigidBody body : entityBodies.values()) {
            physicsSpace.removeCollisionObject(body);
        }
        for (PhysicsRigidBody body : blockBodies.values()) {
            physicsSpace.removeCollisionObject(body);
        }
        entityBodies.clear();
        blockBodies.clear();
        // PhysicsSpace 由 Java GC 自动回收
    }

    /**
     * 获取方块碰撞体数量
     */
    public int getBlockBodyCount() {
        return blockBodies.size();
    }

    /**
     * 获取实体刚体数量
     */
    public int getEntityBodyCount() {
        return entityBodies.size();
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        // 物理步进前的回调，用于施加力、重置位置等
    }

    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        // 物理步进后的回调，用于处理碰撞事件等
    }
}
