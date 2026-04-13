package org.polaris2023.gtu.physics.collision;

import com.jme3.math.Vector3f;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.polaris2023.gtu.physics.rotation.RotationalPhysicsCalculator;

/**
 * 碰撞能量传递和反弹计算
 * <p>
 * 实现真实的碰撞物理：
 * <ul>
 *   <li>能量守恒和动量守恒</li>
 *   <li>恢复系数（弹性/非弹性碰撞）</li>
 *   <li>摩擦力导致的能量损失</li>
 *   <li>碰撞产生的旋转力矩</li>
 *   <li>材料属性影响碰撞结果</li>
 * </ul>
 * <p>
 * <b>物理公式：</b>
 * <ul>
 *   <li>动量守恒: m₁v₁ + m₂v₂ = m₁v₁' + m₂v₂'</li>
 *   <li>能量守恒: ½m₁v₁² + ½m₂v₂² = ½m₁v₁'² + ½m₂v₂'² (弹性碰撞)</li>
 *   <li>恢复系数: e = -(v₁' - v₂') / (v₁ - v₂)</li>
 *   <li>冲量: J = (1 + e) × m_reduced × v_rel</li>
 * </ul>
 */
public class CollisionEnergy {

    /**
     * 默认恢复系数
     */
    public static final float DEFAULT_RESTITUTION = 0.3f;

    /**
     * 默认摩擦系数
     */
    public static final float DEFAULT_FRICTION = 0.5f;

    // ==================== 恢复系数定义 ====================

    /**
     * 不同材料/实体类型的恢复系数
     */
    public static float getRestitution(Entity entity) {
        if (entity instanceof Player) {
            return 0.0f;  // 玩家不会反弹
        }
        if (entity instanceof AbstractArrow) {
            return 0.0f;  // 箭矢不反弹
        }
        if (entity instanceof ItemEntity) {
            return 0.2f;  // 物品轻微反弹
        }
        if (entity instanceof Boat) {
            return 0.3f;  // 船只有一定弹性
        }
        if (entity instanceof FallingBlockEntity) {
            return 0.1f;  // 下落方块几乎不反弹
        }

        // 根据实体ID判断
        String id = entity.getType().builtInRegistryHolder().key().location().toString();

        // 弹性实体
        if (id.contains("slime") || id.contains("magma_cube")) {
            return 0.8f;  // 史莱姆高弹性
        }
        if (id.contains("rubber") || id.contains("bouncy")) {
            return 0.9f;  // 橡胶类
        }

        // 硬质实体
        if (id.contains("iron_golem") || id.contains("shulker")) {
            return 0.05f;  // 铁傀儡几乎不反弹
        }

        return DEFAULT_RESTITUTION;
    }

    /**
     * 不同材料/实体类型的摩擦系数
     */
    public static float getFriction(Entity entity) {
        if (entity instanceof Player) {
            return 0.6f;
        }
        if (entity instanceof Boat) {
            return 0.3f;  // 船只滑溜
        }
        if (entity instanceof ItemEntity) {
            return 0.4f;
        }

        String id = entity.getType().builtInRegistryHolder().key().location().toString();

        if (id.contains("slime")) {
            return 0.8f;  // 史莱姆粘滞
        }
        if (id.contains("ice") || id.contains("packed_ice")) {
            return 0.1f;  // 冰滑
        }

        return DEFAULT_FRICTION;
    }

    // ==================== 碰撞冲量计算 ====================

    /**
     * 计算碰撞冲量
     * <p>
     * J = (1 + e) × m_reduced × v_rel_n
     * <p>
     * 其中 m_reduced = (m₁ × m₂) / (m₁ + m₂) 是约化质量
     *
     * @param massA         物体A质量
     * @param massB         物体B质量
     * @param relativeVelN  相对速度在碰撞法线方向的分量
     * @param restitution   恢复系数
     * @return 冲量大小
     */
    public static float calculateImpulse(float massA, float massB, float relativeVelN, float restitution) {
        // 约化质量
        float reducedMass = (massA * massB) / (massA + massB);
        // 冲量大小
        return -(1.0f + restitution) * reducedMass * relativeVelN;
    }

    /**
     * 计算碰撞后的速度
     * <p>
     * v₁' = v₁ + J/m₁ × n
     * v₂' = v₂ - J/m₂ × n
     *
     * @param massA     物体A质量
     * @param massB     物体B质量
     * @param velA      物体A初始速度
     * @param velB      物体B初始速度
     * @param normal    碰撞法线（从B指向A）
     * @param impulse   冲量大小
     * @return [vA', vB'] 碰撞后速度
     */
    public static Vector3f[] calculateVelocitiesAfterCollision(
            float massA, float massB,
            Vector3f velA, Vector3f velB,
            Vector3f normal, float impulse) {

        Vector3f impulseVec = normal.mult(impulse);

        Vector3f newVelA = velA.add(impulseVec.mult(1.0f / massA));
        Vector3f newVelB = velB.subtract(impulseVec.mult(1.0f / massB));

        return new Vector3f[] { newVelA, newVelB };
    }

    // ==================== 能量计算 ====================

    /**
     * 计算动能
     * <p>
     * E = ½mv²
     */
    public static float calculateKineticEnergy(float mass, Vector3f velocity) {
        float speedSq = velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z;
        return 0.5f * mass * speedSq;
    }

    /**
     * 计算碰撞前后的能量损失
     *
     * @param massA     物体A质量
     * @param massB     物体B质量
     * @param velABefore 碰撞前A速度
     * @param velBBefore 碰撞前B速度
     * @param velAAfter  碰撞后A速度
     * @param velBAfter  碰撞后B速度
     * @return 能量损失（正值表示损失，负值表示增加）
     */
    public static float calculateEnergyLoss(
            float massA, float massB,
            Vector3f velABefore, Vector3f velBBefore,
            Vector3f velAAfter, Vector3f velBAfter) {

        float energyBefore = calculateKineticEnergy(massA, velABefore) + calculateKineticEnergy(massB, velBBefore);
        float energyAfter = calculateKineticEnergy(massA, velAAfter) + calculateKineticEnergy(massB, velBAfter);

        return energyBefore - energyAfter;
    }

    /**
     * 计算恢复系数对应的能量损失比例
     * <p>
     * 完全弹性碰撞 (e=1): 0% 能量损失
     * 完全非弹性碰撞 (e=0): 最大能量损失
     */
    public static float calculateEnergyLossRatio(float restitution) {
        return 1.0f - restitution * restitution;
    }

    // ==================== 摩擦力计算 ====================

    /**
     * 计算碰撞时的摩擦冲量
     * <p>
     * 摩擦力作用于切向，减少切向速度
     *
     * @param normalImpulse   法向冲量
     * @param frictionCoeff   摩擦系数
     * @param tangentVelocity 切向速度
     * @return 摩擦冲量向量
     */
    public static Vector3f calculateFrictionImpulse(
            float normalImpulse, float frictionCoeff, Vector3f tangentVelocity) {

        float tangentSpeed = tangentVelocity.length();
        if (tangentSpeed < 0.001f) {
            return Vector3f.ZERO;
        }

        // 摩擦冲量大小（库仑摩擦）
        float maxFrictionImpulse = frictionCoeff * Math.abs(normalImpulse);

        // 实际摩擦冲量不能超过停止切向运动所需的冲量
        float frictionImpulse = Math.min(maxFrictionImpulse, tangentSpeed);

        // 方向与切向速度相反
        return tangentVelocity.normalize().mult(-frictionImpulse);
    }

    // ==================== 碰撞响应应用 ====================

    /**
     * 应用碰撞响应到实体
     *
     * @param entityA      实体A
     * @param entityB      实体B
     * @param contactPoint 碰撞点
     * @param normal       碰撞法线（从B指向A）
     * @param penetration  穿透深度
     */
    public static void applyCollisionResponse(
            Entity entityA, Entity entityB,
            Vec3 contactPoint, Vec3 normal, float penetration) {

        // 获取物理参数
        float massA = getEntityMass(entityA);
        float massB = getEntityMass(entityB);
        float restitutionA = getRestitution(entityA);
        float restitutionB = getRestitution(entityB);
        float restitution = (restitutionA + restitutionB) / 2.0f;  // 平均恢复系数

        // 获取速度
        Vec3 velA = entityA.getDeltaMovement();
        Vec3 velB = entityB.getDeltaMovement();

        // 计算相对速度
        Vec3 relativeVel = velA.subtract(velB);
        double relativeVelN = relativeVel.dot(normal);

        // 只处理接近的碰撞
        if (relativeVelN >= 0) return;

        // 计算冲量
        float impulse = calculateImpulse(massA, massB, (float) relativeVelN, restitution);

        // 应用冲量
        Vec3 impulseVec = normal.scale(impulse);
        Vec3 newVelA = velA.add(impulseVec.scale(1.0 / massA));
        Vec3 newVelB = velB.subtract(impulseVec.scale(1.0 / massB));

        entityA.setDeltaMovement(newVelA);
        entityB.setDeltaMovement(newVelB);

        // 应用摩擦力
        applyFriction(entityA, entityB, impulse, normal, relativeVel);

        // 应用旋转力矩
        applyCollisionTorque(entityA, entityB, contactPoint, impulseVec);

        // 分离穿透
        separateEntities(entityA, entityB, normal, penetration, massA, massB);
    }

    /**
     * 应用摩擦力
     */
    private static void applyFriction(Entity entityA, Entity entityB,
                                      float normalImpulse, Vec3 normal, Vec3 relativeVel) {
        float frictionA = getFriction(entityA);
        float frictionB = getFriction(entityB);
        float friction = (frictionA + frictionB) / 2.0f;

        // 计算切向速度
        double normalComponent = relativeVel.dot(normal);
        Vec3 tangentVel = relativeVel.subtract(normal.scale(normalComponent));

        if (tangentVel.lengthSqr() < 0.0001) return;

        // 摩擦冲量
        float maxFriction = friction * Math.abs(normalImpulse);
        Vec3 frictionImpulse = tangentVel.normalize().scale(-Math.min(maxFriction, tangentVel.length() * 0.5));

        float massA = getEntityMass(entityA);
        float massB = getEntityMass(entityB);

        entityA.setDeltaMovement(entityA.getDeltaMovement().add(frictionImpulse.scale(1.0 / massA)));
        entityB.setDeltaMovement(entityB.getDeltaMovement().subtract(frictionImpulse.scale(1.0 / massB)));
    }

    /**
     * 应用碰撞产生的旋转力矩
     */
    private static void applyCollisionTorque(Entity entityA, Entity entityB,
                                             Vec3 contactPoint, Vec3 impulseVec) {
        // 获取旋转物理状态
        RotationalPhysics rpA = entityA.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        RotationalPhysics rpB = entityB.getData(DataAttachments.ROTATIONAL_PHYSICS.get());

        if (rpA != null && rpA.enabled) {
            // 力矩 = r × F
            Vec3 rA = contactPoint.subtract(entityA.position());
            float torqueX = (float) (rA.y * impulseVec.z - rA.z * impulseVec.y);
            float torqueY = (float) (rA.z * impulseVec.x - rA.x * impulseVec.z);
            float torqueZ = (float) (rA.x * impulseVec.y - rA.y * impulseVec.x);
            rpA.applyTorque(torqueX, torqueY, torqueZ);
        }

        if (rpB != null && rpB.enabled) {
            Vec3 rB = contactPoint.subtract(entityB.position());
            float torqueX = (float) (rB.y * (-impulseVec.z) - rB.z * (-impulseVec.y));
            float torqueY = (float) (rB.z * (-impulseVec.x) - rB.x * (-impulseVec.z));
            float torqueZ = (float) (rB.x * (-impulseVec.y) - rB.y * (-impulseVec.x));
            rpB.applyTorque(torqueX, torqueY, torqueZ);
        }
    }

    /**
     * 分离穿透的实体
     */
    private static void separateEntities(Entity entityA, Entity entityB,
                                         Vec3 normal, float penetration,
                                         float massA, float massB) {
        if (penetration <= 0) return;

        float totalMass = massA + massB;
        float moveA = penetration * massB / totalMass;
        float moveB = penetration * massA / totalMass;

        Vec3 posA = entityA.position();
        Vec3 posB = entityB.position();

        entityA.setPos(posA.add(normal.scale(moveA)));
        entityB.setPos(posB.subtract(normal.scale(moveB)));
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取实体质量
     */
    public static float getEntityMass(Entity entity) {
        if (entity instanceof Player) {
            return 70.0f;
        }
        if (entity instanceof Boat) {
            return 100.0f;
        }
        if (entity instanceof ItemEntity) {
            return 0.1f;
        }
        if (entity instanceof AbstractArrow) {
            return 0.025f;
        }

        // 根据碰撞箱估算
        var bb = entity.getBoundingBox();
        double volume = bb.getXsize() * bb.getYsize() * bb.getZsize();
        return (float) (volume * 100.0);
    }

    /**
     * 计算碰撞信息字符串
     */
    public static String getCollisionInfo(Entity entityA, Entity entityB, Vec3 relativeVel) {
        float massA = getEntityMass(entityA);
        float massB = getEntityMass(entityB);
        float restitution = (getRestitution(entityA) + getRestitution(entityB)) / 2.0f;

        float energyBefore = (float) (0.5 * massA * entityA.getDeltaMovement().lengthSqr() +
                0.5 * massB * entityB.getDeltaMovement().lengthSqr());
        float energyLossRatio = calculateEnergyLossRatio(restitution);

        return String.format(
                "质量: %.1f / %.1f kg | 恢复系数: %.2f | 相对速度: %.2f m/s | 能量损失: %.1f%%",
                massA, massB, restitution, relativeVel.length() * 20, energyLossRatio * 100
        );
    }

    // ==================== 特殊碰撞处理 ====================

    /**
     * 处理史莱姆弹跳
     * <p>
     * 史莱姆碰撞时会弹得更高
     */
    public static float getSlimeBounceMultiplier(Entity entity) {
        String id = entity.getType().builtInRegistryHolder().key().location().toString();
        if (id.contains("slime") || id.contains("magma_cube")) {
            return 1.5f;  // 史莱姆弹跳增强
        }
        return 1.0f;
    }

    /**
     * 检查是否应该完全反弹（如弹射物）
     */
    public static boolean shouldFullyReflect(Entity entity) {
        return entity instanceof AbstractArrow;
    }

    /**
     * 计算反弹方向
     * <p>
     * v' = v - 2(v·n)n
     */
    public static Vec3 reflect(Vec3 velocity, Vec3 normal) {
        double dot = velocity.dot(normal);
        return velocity.subtract(normal.scale(2 * dot));
    }
}
