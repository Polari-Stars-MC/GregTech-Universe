package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.world.PhysicsManager;
import org.polaris2023.gtu.physics.world.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刚体碰撞检测器
 * <p>
 * 专门处理实体间的刚体碰撞检测，用于：
 * <ul>
 *   <li>实体推挤 - 当实体移动时推开其他实体</li>
 *   <li>实体堆叠 - 检测实体是否站在另一个实体上</li>
 *   <li>碰撞响应 - 计算碰撞后的物理响应</li>
 * </ul>
 * <p>
 * <b>物理原理：</b>
 * <ul>
 *   <li>动量守恒：m1*v1 + m2*v2 = m1*v1' + m2*v2'</li>
 *   <li>能量守恒（弹性碰撞）：0.5*m1*v1² + 0.5*m2*v2² = 0.5*m1*v1'² + 0.5*m2*v2'²</li>
 *   <li>冲量：J = Δp = F*Δt</li>
 * </ul>
 */
public class RigidBodyCollisionDetector {

    /**
     * 推挤力系数
     */
    private static final float PUSH_COEFFICIENT = 15.0f;

    /**
     * 堆叠检测距离
     */
    private static final float STACKING_DETECT_DISTANCE = 0.3f;

    /**
     * 最大推挤距离
     */
    private static final float MAX_PUSH_DISTANCE = 3.0f;

    /**
     * 最小推挤速度阈值
     */
    private static final float MIN_PUSH_SPEED = 0.1f;

    /**
     * 实体堆叠状态缓存
     * Key: 上方实体ID, Value: 下方实体ID
     */
    private final Map<Integer, Integer> stackingMap = new ConcurrentHashMap<>();

    /**
     * 单例实例
     */
    private static final RigidBodyCollisionDetector INSTANCE = new RigidBodyCollisionDetector();

    public static RigidBodyCollisionDetector getInstance() {
        return INSTANCE;
    }

    private RigidBodyCollisionDetector() {
    }

    // ==================== 实体推挤 ====================

    /**
     * 计算并应用实体推挤力
     * <p>
     * 当实体移动时，根据其速度和质量推开附近的实体
     *
     * @param entity 移动的实体
     * @param level  世界
     */
    public void applyEntityPushForce(Entity entity, Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        PhysicsRigidBody entityBody = physicsWorld.getEntityBody(entity.getId());
        if (entityBody == null) return;

        // 获取实体速度
        Vector3f velocity = entityBody.getLinearVelocity(null);
        float speed = velocity.length();

        // 速度太低不推挤
        if (speed < MIN_PUSH_SPEED) return;

        // 获取实体位置和碰撞箱
        Vector3f position = entityBody.getPhysicsLocation(null);
        AABB entityBB = entity.getBoundingBox();

        // 计算推挤范围
        float pushRange = Math.min(MAX_PUSH_DISTANCE, (float)(entityBB.getSize() + speed * 2));

        // 获取附近实体
        List<Entity> nearbyEntities = getNearbyEntities(serverLevel, entity, pushRange);

        // 计算推挤力
        float entityMass = entityBody.getMass();
        Vector3f pushDirection = velocity.normalize();

        for (Entity other : nearbyEntities) {
            if (other == entity) continue;

            PhysicsRigidBody otherBody = physicsWorld.getEntityBody(other.getId());
            if (otherBody == null) continue;

            // 计算方向和距离
            Vector3f otherPos = otherBody.getPhysicsLocation(null);
            Vector3f toOther = otherPos.subtract(position);
            float distance = toOther.length();

            if (distance < 0.001f || distance > pushRange) continue;

            // 计算推挤力方向
            Vector3f pushDir = toOther.normalize();

            // 只推挤前方的实体
            float dot = pushDir.dot(pushDirection);
            if (dot < 0.3f) continue; // 大约 70 度锥形范围

            // 计算推挤力大小
            // F = k * (m1 / (m1 + m2)) * speed * (1 - distance / maxDistance)
            float otherMass = otherBody.getMass();
            float totalMass = entityMass + otherMass;

            // 防止除零
            if (totalMass <= 0.001f) continue;

            float massRatio = entityMass / totalMass;
            float distanceFactor = 1.0f - (distance / pushRange);
            float forceMagnitude = PUSH_COEFFICIENT * massRatio * speed * distanceFactor * dot;

            // 应用推挤力
            Vector3f pushForce = pushDir.mult(forceMagnitude);
            otherBody.applyCentralForce(pushForce);

            // 同时施加一个小的反向力给推挤者（牛顿第三定律）
            if (entityMass > 0) {
                Vector3f reactionForce = pushDir.mult(-forceMagnitude * 0.3f);
                entityBody.applyCentralForce(reactionForce);
            }
        }
    }

    /**
     * 获取附近的实体
     */
    private List<Entity> getNearbyEntities(ServerLevel level, Entity source, float range) {
        List<Entity> result = new ArrayList<>();
        AABB searchBox = source.getBoundingBox().inflate(range);

        for (Entity entity : level.getAllEntities()) {
            if (entity == source) continue;
            if (entity.getBoundingBox().intersects(searchBox)) {
                result.add(entity);
            }
        }

        return result;
    }

    // ==================== 实体堆叠检测 ====================

    /**
     * 检测实体堆叠状态
     * <p>
     * 检查一个实体是否站在另一个实体上方
     *
     * @param level 世界
     */
    public void detectStacking(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        PhysicsSpace physicsSpace = physicsWorld.getPhysicsSpace();

        // 清理旧的堆叠状态
        stackingMap.clear();

        // 遍历所有实体
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity.level().isClientSide()) continue;

            PhysicsRigidBody body = physicsWorld.getEntityBody(entity.getId());
            if (body == null) continue;

            // 检测下方是否有实体
            Integer entityBelow = detectEntityBelow(entity, body, physicsWorld, physicsSpace);
            if (entityBelow != null) {
                stackingMap.put(entity.getId(), entityBelow);
            }
        }
    }

    /**
     * 检测实体下方是否有其他实体
     *
     * @return 下方实体ID，如果没有返回 null
     */
    private Integer detectEntityBelow(Entity entity, PhysicsRigidBody body,
                                       PhysicsWorld physicsWorld, PhysicsSpace physicsSpace) {
        Vector3f position = body.getPhysicsLocation(null);
        Vector3f velocity = body.getLinearVelocity(null);

        // 如果实体在上升，不需要检测
        if (velocity.y > 0.01f) return null;

        // 向下发射射线
        float entityHeight = entity.getBbHeight();
        Vector3f rayStart = new Vector3f(position.x, position.y - entityHeight / 2, position.z);
        Vector3f rayEnd = new Vector3f(position.x, position.y - entityHeight / 2 - STACKING_DETECT_DISTANCE, position.z);

        // 使用 Bullet 射线检测
        List<PhysicsRayTestResult> results = physicsSpace.rayTest(rayStart, rayEnd);

        for (PhysicsRayTestResult result : results) {
            PhysicsCollisionObject hitObject = result.getCollisionObject();

            // 跳过自己
            if (hitObject instanceof PhysicsRigidBody hitBody) {
                Object userData = hitBody.getUserObject();
                if (userData instanceof Integer hitEntityId) {
                    // 确保不是自己
                    if (hitEntityId != entity.getId()) {
                        return hitEntityId;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 检查实体是否站在另一个实体上
     *
     * @param entityId 实体ID
     * @return 如果站在另一个实体上，返回该实体ID；否则返回 null
     */
    public Integer getEntityBelow(int entityId) {
        return stackingMap.get(entityId);
    }

    /**
     * 检查实体是否被其他实体踩着
     *
     * @param entityId 实体ID
     * @return 站在该实体上的实体ID列表
     */
    public List<Integer> getEntitiesAbove(int entityId) {
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : stackingMap.entrySet()) {
            if (entry.getValue() == entityId) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ==================== 碰撞响应 ====================

    /**
     * 计算两个刚体之间的碰撞响应
     * <p>
     * 使用动量守恒和能量守恒计算碰撞后的速度
     *
     * @param bodyA       刚体A
     * @param bodyB       刚体B
     * @param normal      碰撞法线
     * @param restitution 恢复系数
     */
    public void calculateCollisionResponse(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
                                            Vector3f normal, float restitution) {
        float massA = bodyA.getMass();
        float massB = bodyB.getMass();

        // 静态物体不参与计算
        if (massA <= 0 && massB <= 0) return;

        Vector3f velA = bodyA.getLinearVelocity(null);
        Vector3f velB = bodyB.getLinearVelocity(null);

        // 相对速度
        Vector3f relativeVel = velA.subtract(velB);
        float normalVel = relativeVel.dot(normal);

        // 只处理接近的碰撞
        if (normalVel >= 0) return;

        // 计算冲量
        // J = -(1 + e) * v_n / (1/m1 + 1/m2)
        float invMassA = massA > 0 ? 1.0f / massA : 0;
        float invMassB = massB > 0 ? 1.0f / massB : 0;
        float invMassSum = invMassA + invMassB;

        if (invMassSum == 0) return;

        float impulseMagnitude = -(1.0f + restitution) * normalVel / invMassSum;
        Vector3f impulse = normal.mult(impulseMagnitude);

        // 应用冲量
        if (massA > 0) {
            bodyA.applyCentralImpulse(impulse);
        }
        if (massB > 0) {
            bodyB.applyCentralImpulse(impulse.negate());
        }
    }

    /**
     * 应用排斥力防止穿透
     *
     * @param bodyA         刚体A
     * @param bodyB         刚体B
     * @param minDistance   最小距离
     * @param forceStrength 力强度
     */
    public void applySeparationForce(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
                                      float minDistance, float forceStrength) {
        Vector3f posA = bodyA.getPhysicsLocation(null);
        Vector3f posB = bodyB.getPhysicsLocation(null);

        Vector3f diff = posA.subtract(posB);
        float distance = diff.length();

        if (distance < minDistance && distance > 0.001f) {
            float penetration = minDistance - distance;
            Vector3f separationDir = diff.normalize();

            float massA = bodyA.getMass();
            float massB = bodyB.getMass();
            float totalMass = massA + massB;

            if (totalMass <= 0) return;

            // 根据质量比例分配力
            float forceMagnitude = penetration * forceStrength;

            if (massA > 0) {
                Vector3f forceA = separationDir.mult(forceMagnitude * massB / totalMass);
                bodyA.applyCentralForce(forceA);
            }
            if (massB > 0) {
                Vector3f forceB = separationDir.mult(-forceMagnitude * massA / totalMass);
                bodyB.applyCentralForce(forceB);
            }
        }
    }

    // ==================== 批量处理 ====================

    /**
     * 批量处理实体推挤
     *
     * @param level 世界
     */
    public void processAllPushForces(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity.level().isClientSide()) continue;

            // 只处理移动中的实体
            Vec3 delta = entity.getDeltaMovement();
            if (delta.lengthSqr() > 0.001) {
                applyEntityPushForce(entity, level);
            }
        }
    }

    /**
     * 清理堆叠状态
     */
    public void clearStackingState() {
        stackingMap.clear();
    }
}
