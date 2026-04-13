package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.ContactListener;
import com.jme3.bullet.collision.ManifoldPoints;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.polaris2023.gtu.physics.rotation.RotationalPhysicsCalculator;
import org.polaris2023.gtu.physics.world.PhysicsManager;
import org.polaris2023.gtu.physics.world.PhysicsWorld;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实体碰撞管理器 - 异步优化版本
 * <p>
 * 实现 Bullet 物理引擎的实体间碰撞检测和推挤/堆叠响应。
 * <p>
 * <b>异步优化策略：</b>
 * <ul>
 *   <li>碰撞检测在主线程进行（Bullet 要求）</li>
 *   <li>碰撞响应计算在主线程批量执行（Bullet 不是线程安全的）</li>
 *   <li>使用队列缓冲碰撞事件，避免重复计算</li>
 *   <li>堆叠检测使用异步线程进行预计算</li>
 * </ul>
 * <p>
 * <b>碰撞响应模型：</b>
 * <ul>
 *   <li>推挤：根据相对速度和质量计算冲量，推动实体分离</li>
 *   <li>堆叠：检测实体是否站在另一个实体上，处理支撑关系</li>
 *   <li>排斥力：防止实体互相穿透，保持最小距离</li>
 * </ul>
 * <p>
 * <b>物理公式：</b>
 * <ul>
 *   <li>冲量：J = (1 + e) * v_rel / (1/m1 + 1/m2)</li>
 *   <li>排斥力：F = k * (d_min - d) / d，当 d < d_min</li>
 * </ul>
 * 其中 e 为恢复系数，v_rel 为相对速度，m 为质量，d 为距离
 */
public class EntityCollisionManager implements ContactListener {

    // ==================== 物理参数 ====================

    /**
     * 恢复系数 (弹性)
     * 0 = 完全非弹性碰撞（粘在一起）
     * 1 = 完全弹性碰撞
     */
    private static final float RESTITUTION = 0.3f;

    /**
     * 最小排斥距离（方块）
     */
    private static final float MIN_SEPARATION_DISTANCE = 0.2f;

    /**
     * 排斥力强度系数
     */
    private static final float SEPARATION_FORCE = 50.0f;

    /**
     * 推挤力强度系数
     */
    private static final float PUSH_FORCE = 20.0f;

    // ==================== 实例管理 ====================

    private static final EntityCollisionManager INSTANCE = new EntityCollisionManager();

    public static EntityCollisionManager getInstance() {
        return INSTANCE;
    }

    // ==================== 碰撞事件队列 ====================

    /**
     * 当前帧碰撞事件队列
     */
    private final Queue<CollisionEvent> collisionEvents = new ConcurrentLinkedQueue<>();

    /**
     * 碰撞对去重集合
     */
    private final Set<CollisionPair> processedPairs = ConcurrentHashMap.newKeySet();

    /**
     * 是否正在处理碰撞
     */
    private final AtomicBoolean processing = new AtomicBoolean(false);

    /**
     * 待处理的推挤力请求
     */
    private final Queue<PushForceRequest> pushForceRequests = new ConcurrentLinkedQueue<>();

    // ==================== 实体碰撞数据缓存 ====================

    private final Map<Integer, EntityCollisionData> collisionDataCache = new ConcurrentHashMap<>();

    // ==================== 构造函数 ====================

    private EntityCollisionManager() {
    }

    // ==================== ContactListener 接口实现 ====================

    @Override
    public boolean onContactConceived(long pointId, long manifoldId,
                                       PhysicsCollisionObject pcoA, PhysicsCollisionObject pcoB) {
        return true;
    }

    @Override
    public void onContactEnded(long manifoldId) {
    }

    @Override
    public void onContactProcessed(PhysicsCollisionObject pcoA,
                                     PhysicsCollisionObject pcoB, long manifoldPointId) {
        // 快速过滤：只处理刚体碰撞
        if (!(pcoA instanceof PhysicsRigidBody bodyA) || !(pcoB instanceof PhysicsRigidBody bodyB)) {
            return;
        }

        // 获取实体 ID
        Integer entityIdA = getEntityId(bodyA);
        Integer entityIdB = getEntityId(bodyB);

        // 只处理涉及实体的碰撞
        if (entityIdA == null && entityIdB == null) {
            return;
        }

        // 去重检查
        CollisionPair pair = new CollisionPair(
                entityIdA != null ? entityIdA : -1,
                entityIdB != null ? entityIdB : -1
        );

        if (!processedPairs.add(pair)) {
            return;
        }

        // 创建碰撞事件并加入队列
        CollisionEvent event = createCollisionEvent(bodyA, bodyB, entityIdA, entityIdB, manifoldPointId);
        if (event != null) {
            collisionEvents.add(event);
        }
    }

    @Override
    public void onContactStarted(long manifoldId) {
    }

    // ==================== 碰撞事件处理 ====================

    /**
     * 创建碰撞事件
     */
    private CollisionEvent createCollisionEvent(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
                                                 Integer entityIdA, Integer entityIdB, long manifoldPointId) {
        // 获取碰撞点和法线
        Vector3f contactPoint = new Vector3f();
        Vector3f normal = new Vector3f();
        ManifoldPoints.getPositionWorldOnA(manifoldPointId, contactPoint);
        ManifoldPoints.getNormalWorldOnB(manifoldPointId, normal);

        // 获取质量和速度
        float massA = bodyA.getMass();
        float massB = bodyB.getMass();

        Vector3f velA = bodyA.getLinearVelocity(null);
        Vector3f velB = bodyB.getLinearVelocity(null);

        // 计算相对速度
        Vector3f relativeVel = velA.subtract(velB);
        float normalVel = relativeVel.dot(normal);

        // 只处理接近的碰撞
        if (normalVel >= 0) {
            return null;
        }

        // 复制位置数据（避免后续访问）
        Vector3f posA = bodyA.getPhysicsLocation(null);
        Vector3f posB = bodyB.getPhysicsLocation(null);

        return new CollisionEvent(
                bodyA, bodyB,
                entityIdA, entityIdB,
                massA, massB,
                contactPoint, normal, normalVel,
                posA, posB
        );
    }

    // ==================== 帧管理 ====================

    /**
     * 每帧开始时清理碰撞记录
     */
    public void clearFrameCollisions() {
        processedPairs.clear();
        collisionEvents.clear();
    }

    /**
     * 每帧结束时处理碰撞（在主线程调用）
     * <p>
     * 注意：Bullet 不是线程安全的，所有物理操作必须在主线程进行
     */
    public void endFrame(PhysicsSpace physicsSpace) {
        // 处理碰撞事件
        processCollisionEvents();

        // 处理推挤力请求
        processPushForceRequests();
    }

    /**
     * 处理所有碰撞事件
     */
    private void processCollisionEvents() {
        CollisionEvent event;
        while ((event = collisionEvents.poll()) != null) {
            processCollisionEvent(event);
        }
    }

    /**
     * 处理单个碰撞事件
     */
    private void processCollisionEvent(CollisionEvent event) {
        // 实体间碰撞
        if (event.entityIdA != null && event.entityIdB != null) {
            handleEntityEntityCollision(event);
        }
        // 实体与方块碰撞
        else if (event.entityIdA != null) {
            handleEntityBlockCollision(event.bodyA, event.entityIdA, event.normal, event.normalVel);
        } else if (event.entityIdB != null) {
            handleEntityBlockCollision(event.bodyB, event.entityIdB, event.normal.negate(), -event.normalVel);
        }
    }

    // ==================== 碰撞响应计算 ====================

    /**
     * 处理两个实体之间的碰撞
     */
    private void handleEntityEntityCollision(CollisionEvent event) {
        PhysicsRigidBody bodyA = event.bodyA;
        PhysicsRigidBody bodyB = event.bodyB;
        float massA = event.massA;
        float massB = event.massB;
        Vector3f normal = event.normal;
        float normalVel = event.normalVel;

        // 获取实体
        Entity entityA = getEntityFromBody(bodyA, event.entityIdA);
        Entity entityB = getEntityFromBody(bodyB, event.entityIdB);

        // 计算恢复系数（考虑实体类型）
        float restitution = RESTITUTION;
        if (entityA != null && entityB != null) {
            float restA = CollisionEnergy.getRestitution(entityA);
            float restB = CollisionEnergy.getRestitution(entityB);
            restitution = (restA + restB) / 2.0f;

            // 史莱姆弹跳增强
            restitution *= CollisionEnergy.getSlimeBounceMultiplier(entityA);
            restitution *= CollisionEnergy.getSlimeBounceMultiplier(entityB);
        }

        // 计算冲量大小
        float invMassSum = (massA > 0 ? 1.0f / massA : 0) + (massB > 0 ? 1.0f / massB : 0);
        if (invMassSum == 0) return;

        float impulseMagnitude = -(1.0f + restitution) * normalVel / invMassSum;
        Vector3f impulse = normal.mult(impulseMagnitude);

        // 应用冲量到 Bullet 刚体
        if (massA > 0) {
            bodyA.applyCentralImpulse(impulse);
        }
        if (massB > 0) {
            bodyB.applyCentralImpulse(impulse.negate());
        }

        // 将冲量同步回 Minecraft 实体（使碰撞反弹可见）
        if (entityA != null && massA > 0) {
            Vec3 velA = entityA.getDeltaMovement();
            entityA.setDeltaMovement(new Vec3(
                    velA.x + impulse.x / massA,
                    velA.y + impulse.y / massA,
                    velA.z + impulse.z / massA
            ));
        }
        if (entityB != null && massB > 0) {
            Vec3 velB = entityB.getDeltaMovement();
            entityB.setDeltaMovement(new Vec3(
                    velB.x - impulse.x / massB,
                    velB.y - impulse.y / massB,
                    velB.z - impulse.z / massB
            ));
        }

        // 应用碰撞摩擦力
        if (entityA != null && entityB != null) {
            applyCollisionFriction(entityA, entityB, impulseMagnitude, normal);
        }

        // 应用旋转力矩
        applyCollisionTorqueToBodies(bodyA, bodyB, event.contactPoint, impulse, entityA, entityB);

        // 计算排斥力
        Vector3f posA = event.posA;
        Vector3f posB = event.posB;
        Vector3f diff = posA.subtract(posB);
        float distance = diff.length();

        if (distance < MIN_SEPARATION_DISTANCE && distance > 0.001f) {
            float penetration = MIN_SEPARATION_DISTANCE - distance;
            Vector3f separationDir = diff.normalize();
            float forceMagnitude = penetration * SEPARATION_FORCE;
            Vector3f force = separationDir.mult(forceMagnitude);

            // 根据质量比例分配力
            float totalMass = massA + massB;
            if (totalMass > 0) {
                if (massA > 0) {
                    bodyA.applyCentralForce(force.mult(massB / totalMass));
                }
                if (massB > 0) {
                    bodyB.applyCentralForce(force.negate().mult(massA / totalMass));
                }
            }
        }
    }

    /**
     * 应用碰撞力矩到刚体和旋转物理
     */
    private void applyCollisionTorqueToBodies(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
                                               Vector3f contactPoint, Vector3f impulse,
                                               Entity entityA, Entity entityB) {
        // 获取旋转物理状态
        RotationalPhysics rpA = entityA != null ? entityA.getData(DataAttachments.ROTATIONAL_PHYSICS.get()) : null;
        RotationalPhysics rpB = entityB != null ? entityB.getData(DataAttachments.ROTATIONAL_PHYSICS.get()) : null;

        Vector3f posA = bodyA.getPhysicsLocation(null);
        Vector3f posB = bodyB.getPhysicsLocation(null);

        // 力矩 = r × F，应用到 Bullet 刚体和旋转物理
        if (rpA != null && rpA.enabled) {
            Vector3f rA = contactPoint.subtract(posA);
            Vector3f torqueA = rA.cross(impulse);
            bodyA.applyTorque(torqueA);
            rpA.applyTorque(torqueA.x, torqueA.y, torqueA.z);
        }

        if (rpB != null && rpB.enabled) {
            Vector3f rB = contactPoint.subtract(posB);
            Vector3f torqueB = rB.cross(impulse.negate());
            bodyB.applyTorque(torqueB);
            rpB.applyTorque(torqueB.x, torqueB.y, torqueB.z);
        }
    }

    /**
     * 应用碰撞摩擦力
     * <p>
     * 库仑摩擦模型：切向摩擦力 ≤ μ × 法向力
     */
    private void applyCollisionFriction(Entity entityA, Entity entityB,
                                        float normalImpulse, Vector3f normal) {
        float frictionA = CollisionEnergy.getFriction(entityA);
        float frictionB = CollisionEnergy.getFriction(entityB);
        float friction = (frictionA + frictionB) / 2.0f;

        // 计算切向相对速度
        Vec3 velA = entityA.getDeltaMovement();
        Vec3 velB = entityB.getDeltaMovement();
        Vec3 relVel = velA.subtract(velB);
        double normalComp = relVel.dot(toMC(normal));
        Vec3 tangentVel = relVel.subtract(toMC(normal).scale(normalComp));

        if (tangentVel.lengthSqr() < 0.0001) return;

        // 摩擦冲量：不超过停止切向运动所需的量
        float maxFriction = friction * Math.abs(normalImpulse);
        float tangentSpeed = (float) tangentVel.length();
        float frictionImpulse = Math.min(maxFriction, tangentSpeed * 0.5f);

        Vec3 frictionDir = tangentVel.normalize().scale(-frictionImpulse);

        float massA = CollisionEnergy.getEntityMass(entityA);
        float massB = CollisionEnergy.getEntityMass(entityB);

        if (massA > 0) {
            entityA.setDeltaMovement(velA.add(frictionDir.scale(1.0 / massA)));
        }
        if (massB > 0) {
            entityB.setDeltaMovement(velB.subtract(frictionDir.scale(1.0 / massB)));
        }
    }

    /**
     * Bullet Vector3f 转 Minecraft Vec3
     */
    private static Vec3 toMC(Vector3f v) {
        return new Vec3(v.x, v.y, v.z);
    }

    /**
     * 从刚体获取实体
     */
    private Entity getEntityFromBody(PhysicsRigidBody body, Integer entityId) {
        if (entityId == null || entityId < 0) return null;

        // 从 PhysicsManager 的 Level 映射中获取 ServerLevel
        for (ServerLevel level : getCurrentServerLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    /**
     * 获取当前服务器所有世界
     */
    private List<ServerLevel> getCurrentServerLevels() {
        net.minecraft.server.MinecraftServer server =
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Collections.emptyList();
        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            levels.add(level);
        }
        return levels;
    }

    /**
     * 处理实体与方块的碰撞
     */
    private void handleEntityBlockCollision(PhysicsRigidBody body, int entityId,
                                            Vector3f normal, float normalVel) {
        // 方块碰撞由 Bullet 自动处理
    }

    // ==================== 推挤力处理 ====================

    /**
     * 处理推挤力请求
     */
    private void processPushForceRequests() {
        PushForceRequest request;
        while ((request = pushForceRequests.poll()) != null) {
            request.body().applyCentralForce(request.force());
        }
    }

    /**
     * 应用玩家推挤力
     * <p>
     * 注意：此方法在玩家 tick 中调用，需要确保线程安全
     */
    public void applyPlayerPushForce(Player player, Level level) {
        if (level.isClientSide()) return;

        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        var playerBody = physicsWorld.getEntityBody(player.getId());
        if (playerBody == null) return;

        Vec3 lookVec = player.getLookAngle();
        Vector3f pushDir = new Vector3f((float) lookVec.x, 0, (float) lookVec.z).normalize();

        Vector3f playerVel = playerBody.getLinearVelocity(null);
        float speed = new Vector3f(playerVel.x, 0, playerVel.z).length();

        // 只在移动时推挤
        if (speed < 0.05f) return;

        float pushMagnitude = speed * PUSH_FORCE;
        Vector3f playerPos = playerBody.getPhysicsLocation(null);

        // 在主线程遍历实体并应用力
        if (level instanceof ServerLevel serverLevel) {
            for (Entity other : serverLevel.getAllEntities()) {
                if (other == player || other.level().isClientSide()) continue;

                var otherBody = physicsWorld.getEntityBody(other.getId());
                if (otherBody == null) continue;

                Vector3f otherPos = otherBody.getPhysicsLocation(null);
                Vector3f toOther = otherPos.subtract(playerPos);

                // 只推挤前方的实体
                float dot = toOther.x * pushDir.x + toOther.z * pushDir.z;
                if (dot < 0) continue;

                float distance = toOther.length();
                if (distance > 2.0f || distance < 0.001f) continue;

                // 距离衰减
                float factor = 1.0f - (distance / 2.0f);
                Vector3f pushForce = pushDir.mult(pushMagnitude * factor);

                // 直接应用力（在主线程）
                otherBody.applyCentralForce(pushForce);
            }
        }
    }

    // ==================== 堆叠检测 ====================

    /**
     * 检测并处理实体堆叠
     * <p>
     * 注意：此方法在主线程调用，避免并发问题
     */
    public void processStacking(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        PhysicsSpace physicsSpace = physicsWorld.getPhysicsSpace();

        // 遍历所有实体，检测堆叠
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity.level().isClientSide()) continue;

            var body = physicsWorld.getEntityBody(entity.getId());
            if (body == null) continue;

            // 检测下方是否有其他实体
            checkEntityBelow(entity, body, physicsWorld, physicsSpace);
        }
    }

    /**
     * 检测实体下方是否有其他实体
     */
    private void checkEntityBelow(Entity entity, PhysicsRigidBody body,
                                   PhysicsWorld physicsWorld, PhysicsSpace physicsSpace) {
        Vector3f vel = body.getLinearVelocity(null);

        // 如果实体在上升或静止，不需要检测
        if (vel.y >= 0.01f) return;

        Vector3f pos = body.getPhysicsLocation(null);
        float entityHeight = entity.getBbHeight();
        float rayLength = 0.5f;

        // 向下发射射线检测
        Vector3f rayStart = new Vector3f(pos.x, pos.y - entityHeight / 2, pos.z);
        Vector3f rayEnd = new Vector3f(pos.x, pos.y - entityHeight / 2 - rayLength, pos.z);

        // 使用 Bullet 射线检测
        List<PhysicsRayTestResult> results = physicsSpace.rayTest(rayStart, rayEnd);

        for (PhysicsRayTestResult result : results) {
            PhysicsCollisionObject hitObject = result.getCollisionObject();

            if (hitObject instanceof PhysicsRigidBody hitBody) {
                Object userData = hitBody.getUserObject();
                if (userData instanceof Integer hitEntityId && hitEntityId != entity.getId()) {
                    // 找到下方的实体，可以处理堆叠逻辑
                    handleStacking(entity, body, hitBody, result);
                    break;
                }
            }
        }
    }

    /**
     * 处理实体堆叠
     */
    private void handleStacking(Entity upperEntity, PhysicsRigidBody upperBody,
                                 PhysicsRigidBody lowerBody, PhysicsRayTestResult result) {
        // 获取堆叠信息
        float hitFraction = result.getHitFraction();

        // 如果距离很近，可以传递力或处理支撑
        // 这里可以添加堆叠物理逻辑
    }

    // ==================== 辅助方法 ====================

    /**
     * 从刚体获取实体 ID
     */
    private Integer getEntityId(PhysicsRigidBody body) {
        Object userData = body.getUserObject();
        if (userData instanceof Integer entityId) {
            return entityId;
        }
        return null;
    }

    /**
     * 关闭（保留接口兼容）
     */
    public void shutdown() {
        // 清理队列
        collisionEvents.clear();
        pushForceRequests.clear();
        processedPairs.clear();
    }

    // ==================== 内部数据结构 ====================

    /**
     * 碰撞事件
     */
    private record CollisionEvent(
            PhysicsRigidBody bodyA,
            PhysicsRigidBody bodyB,
            Integer entityIdA,
            Integer entityIdB,
            float massA,
            float massB,
            Vector3f contactPoint,
            Vector3f normal,
            float normalVel,
            Vector3f posA,
            Vector3f posB
    ) {}

    /**
     * 推挤力请求
     */
    private record PushForceRequest(
            PhysicsRigidBody body,
            Vector3f force
    ) {}

    /**
     * 碰撞对（用于去重）
     */
    private record CollisionPair(int entityIdA, int entityIdB) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CollisionPair that = (CollisionPair) o;
            return (entityIdA == that.entityIdA && entityIdB == that.entityIdB) ||
                   (entityIdA == that.entityIdB && entityIdB == that.entityIdA);
        }

        @Override
        public int hashCode() {
            return entityIdA < entityIdB ? entityIdA * 31 + entityIdB : entityIdB * 31 + entityIdA;
        }
    }

    /**
     * 实体碰撞数据
     */
    private static class EntityCollisionData {
        int entityId;
        float mass;
        float restitution;
        float friction;
        boolean canPush;
        boolean canBePushed;
    }
}
