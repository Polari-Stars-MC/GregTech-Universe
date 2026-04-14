package org.polaris2023.gtu.physics.world;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.polaris2023.gtu.physics.collision.EntityCollisionManager;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.polaris2023.gtu.physics.rotation.RotationalPhysicsCalculator;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bullet 物理世界管理器
 * <p>
 * 管理单个维度/世界的物理模拟
 * <p>
 * <b>线程安全：</b>
 * <ul>
 *   <li>使用 ConcurrentHashMap 存储刚体</li>
 *   <li>使用 ReentrantLock 保护物理操作</li>
 *   <li>所有物理操作必须在主线程进行</li>
 * </ul>
 */
public class PhysicsWorld implements PhysicsTickListener {

    private final CustomPhysicsSpace physicsSpace;
    private final Map<Integer, PhysicsRigidBody> entityBodies = new ConcurrentHashMap<>();
    private final Map<Long, PhysicsRigidBody> blockBodies = new ConcurrentHashMap<>();

    /**
     * 操作锁（保护 add/remove 操作）
     */
    private final ReentrantLock operationLock = new ReentrantLock();

    /**
     * 当前维度的物理配置
     */
    private final DimensionPhysics dimensionPhysics;

    /**
     * 是否已销毁
     */
    private volatile boolean destroyed = false;

    /**
     * 创建物理世界
     */
    public PhysicsWorld(DimensionPhysics dimensionPhysics) {
        this.dimensionPhysics = dimensionPhysics;
        this.physicsSpace = new CustomPhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        this.physicsSpace.setGravity(new Vector3f(0, -dimensionPhysics.gravity, 0));
        this.physicsSpace.addTickListener(this);
    }

    /**
     * 创建默认物理世界
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

    // ==================== 实体刚体管理 ====================

    /**
     * 添加实体刚体
     */
    public PhysicsRigidBody addEntityBody(int entityId, CollisionShape shape, float mass,
                                          Vector3f position, float yRot) {
        if (destroyed) return null;

        operationLock.lock();
        try {
            // 检查是否已存在
            PhysicsRigidBody existing = entityBodies.get(entityId);
            if (existing != null) {
                // 更新位置
                existing.setPhysicsLocation(position);
                return existing;
            }

            PhysicsRigidBody body = new PhysicsRigidBody(shape, mass);
            body.setPhysicsLocation(position);
            body.setPhysicsRotation(new Quaternion().fromAngles(0, yRot, 0));
            body.setUserObject(entityId);
            body.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
            body.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_01);
            body.setGravity(Vector3f.ZERO);  // MC 自己处理重力，Bullet 不需要

            physicsSpace.addCollisionObject(body);
            entityBodies.put(entityId, body);
            return body;
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 移除实体刚体
     */
    public void removeEntityBody(int entityId) {
        if (destroyed) return;

        operationLock.lock();
        try {
            PhysicsRigidBody body = entityBodies.remove(entityId);
            if (body != null) {
                physicsSpace.removeCollisionObject(body);
            }
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 获取实体刚体
     */
    public PhysicsRigidBody getEntityBody(int entityId) {
        return entityBodies.get(entityId);
    }

    /**
     * 获取所有实体刚体映射
     */
    public Map<Integer, PhysicsRigidBody> getEntityBodies() {
        return entityBodies;
    }

    /**
     * 检查实体刚体是否存在
     */
    public boolean hasEntityBody(int entityId) {
        return entityBodies.containsKey(entityId);
    }

    // ==================== 方块碰撞管理 ====================

    /**
     * 添加静态方块碰撞体
     */
    public PhysicsRigidBody addBlockBody(long blockKey, CollisionShape shape, Vector3f position) {
        if (destroyed) return null;

        operationLock.lock();
        try {
            // 检查是否已存在
            PhysicsRigidBody existing = blockBodies.get(blockKey);
            if (existing != null) {
                return existing;
            }

            PhysicsRigidBody body = new PhysicsRigidBody(shape, 0.0f);
            body.setPhysicsLocation(position);
            body.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_01);
            body.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);

            physicsSpace.addCollisionObject(body);
            blockBodies.put(blockKey, body);
            return body;
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 移除方块碰撞体
     */
    public void removeBlockBody(long blockKey) {
        if (destroyed) return;

        operationLock.lock();
        try {
            PhysicsRigidBody body = blockBodies.remove(blockKey);
            if (body != null) {
                physicsSpace.removeCollisionObject(body);
            }
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 批量移除指定范围内的方块碰撞体
     */
    public void removeBlockBodiesInRange(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (destroyed) return;

        operationLock.lock();
        try {
            Iterator<Map.Entry<Long, PhysicsRigidBody>> iterator = blockBodies.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, PhysicsRigidBody> entry = iterator.next();
                long key = entry.getKey();

                int x = BlockPos.getX(key);
                int y = BlockPos.getY(key);
                int z = BlockPos.getZ(key);

                if (x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ) {
                    physicsSpace.removeCollisionObject(entry.getValue());
                    iterator.remove();
                }
            }
        } finally {
            operationLock.unlock();
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

    // ==================== 物理步进 ====================

    /**
     * 步进物理模拟
     */
    public void step(float timeStep) {
        if (destroyed) return;

        // 检查是否暂停
        if (PhysicsPauseManager.getInstance().isPaused()) {
            return;
        }

        operationLock.lock();
        try {
            physicsSpace.update(timeStep, 1, true, true, true);
        } finally {
            operationLock.unlock();
        }
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
        if (destroyed) return;
        physicsSpace.setGravity(new Vector3f(x, y, z));
    }

    // ==================== 生命周期 ====================

    /**
     * 清理所有物理对象
     */
    public void destroy() {
        if (destroyed) return;
        destroyed = true;

        operationLock.lock();
        try {
            for (PhysicsRigidBody body : entityBodies.values()) {
                physicsSpace.removeCollisionObject(body);
            }
            for (PhysicsRigidBody body : blockBodies.values()) {
                physicsSpace.removeCollisionObject(body);
            }
            entityBodies.clear();
            blockBodies.clear();
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 是否已销毁
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    // ==================== 统计 ====================

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

    // ==================== 旋转物理同步 ====================

    /**
     * 同步实体的旋转状态到 Bullet 刚体
     *
     * @param entityId 实体ID
     * @param rp       旋转物理状态
     */
    public void syncRotationToBody(int entityId, RotationalPhysics rp) {
        if (destroyed || rp == null) return;

        PhysicsRigidBody body = entityBodies.get(entityId);
        if (body == null) return;

        // 将角速度转换为 Bullet 坐标系
        body.setAngularVelocity(new Vector3f(rp.omegaX, rp.omegaY, rp.omegaZ));

        // 更新旋转角度
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(rp.rotationX, rp.rotationY, rp.rotationZ);
        body.setPhysicsRotation(rotation);
    }

    /**
     * 从 Bullet 刚体同步旋转状态到实体
     *
     * @param entityId 实体ID
     * @param rp       旋转物理状态（输出）
     */
    public void syncBodyToRotation(int entityId, RotationalPhysics rp) {
        if (destroyed || rp == null) return;

        PhysicsRigidBody body = entityBodies.get(entityId);
        if (body == null) return;

        // 获取角速度
        Vector3f omega = body.getAngularVelocity(null);
        rp.omegaX = omega.x;
        rp.omegaY = omega.y;
        rp.omegaZ = omega.z;

        // 获取旋转角度
        Quaternion rot = body.getPhysicsRotation(null);
        // 使用 getRotationColumn 获取欧拉角
        // 简化处理：直接使用四元数的 x, y, z, w 分量
        // 对于简单的 Y 轴旋转，可以直接从四元数提取
        rp.rotationX = (float) Math.atan2(2 * (rot.getW() * rot.getX() + rot.getY() * rot.getZ()),
                1 - 2 * (rot.getX() * rot.getX() + rot.getY() * rot.getY()));
        rp.rotationY = (float) Math.asin(2 * (rot.getW() * rot.getY() - rot.getZ() * rot.getX()));
        rp.rotationZ = (float) Math.atan2(2 * (rot.getW() * rot.getZ() + rot.getX() * rot.getY()),
                1 - 2 * (rot.getY() * rot.getY() + rot.getZ() * rot.getZ()));
    }

    /**
     * 应用力矩到刚体
     *
     * @param entityId 实体ID
     * @param torqueX  x轴力矩
     * @param torqueY  y轴力矩
     * @param torqueZ  z轴力矩
     */
    public void applyTorqueToBody(int entityId, float torqueX, float torqueY, float torqueZ) {
        if (destroyed) return;

        PhysicsRigidBody body = entityBodies.get(entityId);
        if (body == null) return;

        body.applyTorque(new Vector3f(torqueX, torqueY, torqueZ));
    }

    /**
     * 应用冲量矩到刚体
     *
     * @param entityId 实体ID
     * @param impulseX x轴冲量矩
     * @param impulseY y轴冲量矩
     * @param impulseZ z轴冲量矩
     */
    public void applyAngularImpulseToBody(int entityId, float impulseX, float impulseY, float impulseZ) {
        if (destroyed) return;

        PhysicsRigidBody body = entityBodies.get(entityId);
        if (body == null) return;

        body.applyTorqueImpulse(new Vector3f(impulseX, impulseY, impulseZ));
    }

    // ==================== PhysicsTickListener ====================

    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        EntityCollisionManager.getInstance().clearFrameCollisions();
    }

    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        EntityCollisionManager.getInstance().endFrame(space);
    }
}
