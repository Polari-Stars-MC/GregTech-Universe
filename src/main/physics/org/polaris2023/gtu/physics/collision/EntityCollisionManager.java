package org.polaris2023.gtu.physics.collision;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.ContactListener;
import com.jme3.bullet.collision.ManifoldPoints;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.world.PhysicsManager;
import org.polaris2023.gtu.physics.world.PhysicsWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体碰撞管理器
 * <p>
 * 实现 Bullet 物理引擎的实体间碰撞检测和推挤/堆叠响应。
 * <p>
 * 碰撞响应模型：
 * <ul>
 *   <li>推挤：根据相对速度和质量计算冲量，推动实体分离</li>
 *   <li>堆叠：检测实体是否站在另一个实体上，处理支撑关系</li>
 *   <li>排斥力：防止实体互相穿透，保持最小距离</li>
 * </ul>
 * <p>
 * 物理公式：
 * <ul>
 *   <li>冲量：J = (1 + e) * v_rel / (1/m1 + 1/m2)</li>
 *   <li>排斥力：F = k * (d_min - d) / d，当 d < d_min</li>
 * </ul>
 * 其中 e 为恢复系数，v_rel 为相对速度，m 为质量，d 为距离
 */
public class EntityCollisionManager implements ContactListener {

    /**
     * 恢复系数 (弹性)
     * 0 = 完全非弹性碰撞（粘在一起）
     * 1 = 完全弹性碰撞
     */
    private static final float RESTITUTION = 0.3f;

    /**
     * 最小排斥距离（方块）
     * 实体之间保持的最小距离
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

    /**
     * 单例实例
     */
    private static final EntityCollisionManager INSTANCE = new EntityCollisionManager();

    /**
     * 当前帧的碰撞对（用于去重）
     */
    private final Set<CollisionPair> currentFrameCollisions = ConcurrentHashMap.newKeySet();

    /**
     * 实体碰撞数据缓存
     */
    private final Map<Integer, EntityCollisionData> collisionDataMap = new HashMap<>();

    private EntityCollisionManager() {
    }

    public static EntityCollisionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 处理物理空间中的碰撞
     * <p>
     * 使用 Bullet 的碰撞检测系统检测实体间碰撞
     *
     * @param physicsSpace Bullet 物理空间
     */
    public void processCollisions(PhysicsSpace physicsSpace) {
        // 清理上一帧的碰撞记录
        currentFrameCollisions.clear();

        // 碰撞处理由 ContactListener 回调自动完成
        // 这里可以添加额外的碰撞后处理逻辑
    }

    // ==================== ContactListener 接口实现 ====================

    /**
     * 碰撞点即将添加到流形时的回调
     * <p>
     * 可以通过返回 false 来拒绝碰撞点
     */
    @Override
    public boolean onContactConceived(long pointId, long manifoldId,
                                       PhysicsCollisionObject pcoA, PhysicsCollisionObject pcoB) {
        // 默认接受所有碰撞点
        return true;
    }

    /**
     * 碰撞流形结束时的回调
     */
    @Override
    public void onContactEnded(long manifoldId) {
        // 可以用于清理碰撞状态
    }

    /**
     * 碰撞点处理时的回调
     * <p>
     * 这是主要的碰撞处理入口，在物理步进期间被调用
     */
    @Override
    public void onContactProcessed(PhysicsCollisionObject pcoA,
                                     PhysicsCollisionObject pcoB, long manifoldPointId) {
        // 只处理刚体碰撞
        if (!(pcoA instanceof PhysicsRigidBody bodyA) || !(pcoB instanceof PhysicsRigidBody bodyB)) {
            return;
        }

        // 获取实体 ID
        Integer entityIdA = getEntityId(bodyA);
        Integer entityIdB = getEntityId(bodyB);

        // 只处理实体间碰撞（忽略方块碰撞）
        if (entityIdA == null && entityIdB == null) {
            return;
        }

        // 创建碰撞对用于去重
        CollisionPair pair = new CollisionPair(
                entityIdA != null ? entityIdA : -1,
                entityIdB != null ? entityIdB : -1
        );

        // 避免同一帧重复处理
        if (!currentFrameCollisions.add(pair)) {
            return;
        }

        // 处理碰撞
        processCollision(bodyA, bodyB, entityIdA, entityIdB, manifoldPointId);
    }

    /**
     * 碰撞流形开始时的回调
     */
    @Override
    public void onContactStarted(long manifoldId) {
        // 可以用于初始化碰撞状态
    }

    // ==================== 碰撞处理逻辑 ====================

    /**
     * 处理碰撞事件
     */
    private void processCollision(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
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

        // 计算法向相对速度
        float normalVel = relativeVel.dot(normal);

        // 只处理接近的碰撞（法向速度为负表示靠近）
        if (normalVel >= 0) {
            return;
        }

        // ── 实体间碰撞处理 ──
        if (entityIdA != null && entityIdB != null) {
            handleEntityEntityCollision(bodyA, bodyB, entityIdA, entityIdB, massA, massB, normal, normalVel);
        }
        // ── 实体与方块碰撞处理（可选）──
        else if (entityIdA != null) {
            handleEntityBlockCollision(bodyA, entityIdA, normal, normalVel);
        } else if (entityIdB != null) {
            handleEntityBlockCollision(bodyB, entityIdB, normal.negate(), -normalVel);
        }
    }

    /**
     * 处理两个实体之间的碰撞
     * <p>
     * 计算冲量并应用推挤力
     */
    private void handleEntityEntityCollision(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
                                             int entityIdA, int entityIdB,
                                             float massA, float massB,
                                             Vector3f normal, float normalVel) {
        // 计算冲量大小（基于恢复系数）
        // J = -(1 + e) * v_n / (1/m1 + 1/m2)
        float invMassSum = (massA > 0 ? 1.0f / massA : 0) + (massB > 0 ? 1.0f / massB : 0);
        if (invMassSum == 0) return;

        float impulseMagnitude = -(1.0f + RESTITUTION) * normalVel / invMassSum;

        // 应用冲量到两个刚体
        Vector3f impulse = normal.mult(impulseMagnitude);

        if (massA > 0) {
            bodyA.applyCentralImpulse(impulse);
        }
        if (massB > 0) {
            bodyB.applyCentralImpulse(impulse.negate());
        }

        // 应用排斥力（防止穿透）
        applySeparationForce(bodyA, bodyB, massA, massB, normal);
    }

    /**
     * 应用排斥力，防止实体穿透
     */
    private void applySeparationForce(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB,
                                      float massA, float massB, Vector3f normal) {
        Vector3f posA = bodyA.getPhysicsLocation(null);
        Vector3f posB = bodyB.getPhysicsLocation(null);

        // 计算距离
        Vector3f diff = posA.subtract(posB);
        float distance = diff.length();

        // 如果距离小于最小距离，施加排斥力
        if (distance < MIN_SEPARATION_DISTANCE && distance > 0.001f) {
            float penetration = MIN_SEPARATION_DISTANCE - distance;
            Vector3f separationDir = diff.normalize();

            // 排斥力与穿透深度成正比
            float forceMagnitude = penetration * SEPARATION_FORCE;

            Vector3f force = separationDir.mult(forceMagnitude);

            // 根据质量比例分配力
            if (massA > 0) {
                bodyA.applyCentralForce(force.mult(massB / (massA + massB)));
            }
            if (massB > 0) {
                bodyB.applyCentralForce(force.negate().mult(massA / (massA + massB)));
            }
        }
    }

    /**
     * 处理实体与方块的碰撞
     */
    private void handleEntityBlockCollision(PhysicsRigidBody body, int entityId,
                                            Vector3f normal, float normalVel) {
        // 方块碰撞通常由 Bullet 自动处理
        // 这里可以添加额外逻辑，如弹跳、滑动等
    }

    /**
     * 从刚体获取实体 ID
     * <p>
     * 通过检查用户数据获取关联的实体 ID
     */
    private Integer getEntityId(PhysicsRigidBody body) {
        Object userData = body.getUserObject();
        if (userData instanceof Integer entityId) {
            return entityId;
        }
        return null;
    }

    /**
     * 每帧开始时清理碰撞记录
     */
    public void clearFrameCollisions() {
        currentFrameCollisions.clear();
    }

    /**
     * 检测并处理实体堆叠
     * <p>
     * 检查一个实体是否站在另一个实体上方
     *
     * @param level 世界
     */
    public void processStacking(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        // 遍历所有实体，检测堆叠
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity.level().isClientSide()) continue;

            var body = physicsWorld.getEntityBody(entity.getId());
            if (body == null) continue;

            // 检测下方是否有其他实体
            checkEntityBelow(entity, body, physicsWorld);
        }
    }

    /**
     * 检测实体下方是否有其他实体
     */
    private void checkEntityBelow(Entity entity, PhysicsRigidBody body, PhysicsWorld physicsWorld) {
        Vector3f pos = body.getPhysicsLocation(null);
        Vector3f vel = body.getLinearVelocity(null);

        // 如果实体在上升或静止，不需要检测
        if (vel.y >= 0.01f) return;

        // 向下发射射线检测
        float rayLength = 0.5f; // 检测范围
        Vector3f rayStart = pos.add(0, -entity.getBbHeight() / 2, 0);
        Vector3f rayEnd = rayStart.add(0, -rayLength, 0);

        // 这里简化处理：检查附近的其他实体
        // 实际实现可以使用 Bullet 的射线检测
    }

    /**
     * 应用玩家推挤力
     * <p>
     * 当玩家移动时，推开前方的其他实体
     *
     * @param player 玩家
     * @param level  世界
     */
    public void applyPlayerPushForce(Player player, Level level) {
        if (level.isClientSide()) return;

        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(level);
        if (physicsWorld == null) return;

        var playerBody = physicsWorld.getEntityBody(player.getId());
        if (playerBody == null) return;

        Vec3 lookVec = player.getLookAngle();
        Vector3f pushDir = new Vector3f((float) lookVec.x, 0, (float) lookVec.z).normalize();

        // 获取玩家速度
        Vector3f playerVel = playerBody.getLinearVelocity(null);
        float speed = new Vector3f(playerVel.x, 0, playerVel.z).length();

        // 只在移动时推挤
        if (speed < 0.05f) return;

        // 推挤力与移动速度成正比
        float pushMagnitude = speed * PUSH_FORCE;

        // 查找玩家前方的实体并施加推力
        // 简化实现：遍历附近实体
        if (level instanceof ServerLevel serverLevel) {
            for (Entity other : serverLevel.getAllEntities()) {
                if (other == player || other.level().isClientSide()) continue;

                var otherBody = physicsWorld.getEntityBody(other.getId());
                if (otherBody == null) continue;

                // 计算方向
                Vector3f playerPos = playerBody.getPhysicsLocation(null);
                Vector3f otherPos = otherBody.getPhysicsLocation(null);
                Vector3f toOther = otherPos.subtract(playerPos);

                // 只推挤前方的实体
                float dot = toOther.x * pushDir.x + toOther.z * pushDir.z;
                if (dot < 0) continue; // 后方的不推

                float distance = toOther.length();
                if (distance > 2.0f) continue; // 太远不推

                // 距离衰减
                float factor = 1.0f - (distance / 2.0f);
                Vector3f pushForce = pushDir.mult(pushMagnitude * factor);

                otherBody.applyCentralForce(pushForce);
            }
        }
    }

    /**
     * 碰撞对记录（用于去重）
     */
    private record CollisionPair(int entityIdA, int entityIdB) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CollisionPair that = (CollisionPair) o;
            // 顺序无关的比较
            return (entityIdA == that.entityIdA && entityIdB == that.entityIdB) ||
                   (entityIdA == that.entityIdB && entityIdB == that.entityIdA);
        }

        @Override
        public int hashCode() {
            // 顺序无关的哈希
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
