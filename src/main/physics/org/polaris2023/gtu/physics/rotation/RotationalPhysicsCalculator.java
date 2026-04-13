package org.polaris2023.gtu.physics.rotation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.polaris2023.gtu.physics.fluid.FluidPhysics;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import org.polaris2023.gtu.physics.world.PhysicsManager;

/**
 * 旋转物理计算器
 * <p>
 * 提供旋转物理的各种计算方法，包括：
 * <ul>
 *   <li>转动惯量计算</li>
 *   <li>碰撞力矩计算</li>
 *   <li>流体阻力力矩计算</li>
 *   <li>角动量守恒计算</li>
 * </ul>
 */
public class RotationalPhysicsCalculator {

    /**
     * 空气角阻尼系数
     */
    public static final float AIR_ANGULAR_DAMPING = 0.02f;

    /**
     * 水中角阻尼系数
     */
    public static final float WATER_ANGULAR_DAMPING = 0.3f;

    /**
     * 岩浆中角阻尼系数
     */
    public static final float LAVA_ANGULAR_DAMPING = 0.5f;

    /**
     * 地面摩擦角阻尼系数
     */
    public static final float GROUND_ANGULAR_DAMPING = 2.0f;

    // ==================== 转动惯量计算 ====================

    /**
     * 计算球体的转动惯量
     * <p>
     * I = (2/5) × m × r²
     *
     * @param mass   质量 (kg)
     * @param radius 半径 (m)
     * @return 转动惯量 (kg·m²)
     */
    public static float sphereMomentOfInertia(float mass, float radius) {
        return (2.0f / 5.0f) * mass * radius * radius;
    }

    /**
     * 计算实心圆柱体的转动惯量（绕轴）
     * <p>
     * I = (1/2) × m × r²
     *
     * @param mass   质量 (kg)
     * @param radius 半径 (m)
     * @return 转动惯量 (kg·m²)
     */
    public static float cylinderMomentOfInertia(float mass, float radius) {
        return (1.0f / 2.0f) * mass * radius * radius;
    }

    /**
     * 计算长方体的转动惯量
     * <p>
     * 绕x轴: Ixx = (1/12) × m × (h² + d²)
     * 绕y轴: Iyy = (1/12) × m × (w² + d²)
     * 绕z轴: Izz = (1/12) × m × (w² + h²)
     *
     * @param mass   质量 (kg)
     * @param width  宽度 (m)
     * @param height 高度 (m)
     * @param depth  深度 (m)
     * @return [Ixx, Iyy, Izz]
     */
    public static float[] boxMomentOfInertia(float mass, float width, float height, float depth) {
        float Ixx = (1.0f / 12.0f) * mass * (height * height + depth * depth);
        float Iyy = (1.0f / 12.0f) * mass * (width * width + depth * depth);
        float Izz = (1.0f / 12.0f) * mass * (width * width + height * height);
        return new float[] { Ixx, Iyy, Izz };
    }

    /**
     * 计算细杆的转动惯量
     * <p>
     * 绕中心: I = (1/12) × m × L²
     * 绕端点: I = (1/3) × m × L²
     *
     * @param mass       质量 (kg)
     * @param length     长度 (m)
     * @param aboutEnd   是否绕端点旋转
     * @return 转动惯量 (kg·m²)
     */
    public static float rodMomentOfInertia(float mass, float length, boolean aboutEnd) {
        if (aboutEnd) {
            return (1.0f / 3.0f) * mass * length * length;
        }
        return (1.0f / 12.0f) * mass * length * length;
    }

    /**
     * 根据实体碰撞箱计算转动惯量
     *
     * @param entity 实体
     * @param mass   质量 (kg)
     * @return [Ixx, Iyy, Izz]
     */
    public static float[] calculateFromBoundingBox(Entity entity, float mass) {
        AABB bb = entity.getBoundingBox();
        float width = (float) bb.getXsize();
        float height = (float) bb.getYsize();
        float depth = (float) bb.getZsize();
        return boxMomentOfInertia(mass, width, height, depth);
    }

    // ==================== 力矩计算 ====================

    /**
     * 计算碰撞产生的力矩
     * <p>
     * τ = r × F
     *
     * @param contactPoint 接触点位置（世界坐标）
     * @param centerOfMass 质心位置（世界坐标）
     * @param force        作用力向量
     * @return 力矩向量 [τx, τy, τz]
     */
    public static float[] calculateCollisionTorque(Vec3 contactPoint, Vec3 centerOfMass, Vec3 force) {
        // r = 接触点 - 质心
        float rx = (float) (contactPoint.x - centerOfMass.x);
        float ry = (float) (contactPoint.y - centerOfMass.y);
        float rz = (float) (contactPoint.z - centerOfMass.z);

        // τ = r × F
        float tx = ry * (float) force.z - rz * (float) force.y;
        float ty = rz * (float) force.x - rx * (float) force.z;
        float tz = rx * (float) force.y - ry * (float) force.x;

        return new float[] { tx, ty, tz };
    }

    /**
     * 计算偏心力矩（力不通过质心时产生的力矩）
     * <p>
     * 例如：风力、推力等作用在实体一侧时
     *
     * @param force         作用力
     * @param forcePoint    力作用点（相对于质心）
     * @return 力矩向量
     */
    public static float[] calculateOffsetForceTorque(Vec3 force, Vec3 forcePoint) {
        return calculateCollisionTorque(forcePoint, Vec3.ZERO, force);
    }

    // ==================== 流体阻力力矩 ====================

    /**
     * 计算流体对旋转的阻力力矩
     * <p>
     * 流体阻力会减缓旋转，力矩方向与角速度相反
     * <p>
     * τ_drag = -k × ω × |ω|
     *
     * @param omegaX        x轴角速度 (rad/s)
     * @param omegaY        y轴角速度 (rad/s)
     * @param omegaZ        z轴角速度 (rad/s)
     * @param fluidDensity  流体密度 (kg/m³)
     * @param crossSection  有效横截面积 (m²)
     * @param radius        有效半径 (m)
     * @return 阻力力矩 [τx, τy, τz]
     */
    public static float[] calculateFluidDragTorque(float omegaX, float omegaY, float omegaZ,
                                                    float fluidDensity, float crossSection, float radius) {
        float omegaMag = (float) Math.sqrt(omegaX * omegaX + omegaY * omegaY + omegaZ * omegaZ);
        if (omegaMag < 0.001f) {
            return new float[] { 0, 0, 0 };
        }

        // 阻力系数
        float k = 0.5f * fluidDensity * crossSection * radius * radius;

        // 阻力力矩与角速度平方成正比，方向相反
        float dragMag = k * omegaMag;

        return new float[] {
                -dragMag * omegaX / omegaMag,
                -dragMag * omegaY / omegaMag,
                -dragMag * omegaZ / omegaMag
        };
    }

    // ==================== 角动量守恒 ====================

    /**
     * 计算角动量守恒下的新角速度
     * <p>
     * 当转动惯量改变时（如伸展手臂），角动量守恒：
     * L = I₁ × ω₁ = I₂ × ω₂
     * <p>
     * 因此：ω₂ = ω₁ × (I₁ / I₂)
     *
     * @param oldOmega      原角速度
     * @param oldInertia    原转动惯量
     * @param newInertia    新转动惯量
     * @return 新角速度
     */
    public static float conserveAngularMomentum(float oldOmega, float oldInertia, float newInertia) {
        return oldOmega * oldInertia / newInertia;
    }

    /**
     * 计算碰撞后的角速度（角动量守恒）
     * <p>
     * 两个旋转物体碰撞后，总角动量守恒
     *
     * @param omega1 物体1初始角速度
     * @param I1     物体1转动惯量
     * @param omega2 物体2初始角速度
     * @param I2     物体2转动惯量
     * @param restitution 恢复系数 (0-1)
     * @return [新omega1, 新omega2]
     */
    public static float[] collisionAngularVelocities(float omega1, float I1, float omega2, float I2, float restitution) {
        // 角动量守恒
        float L_total = I1 * omega1 + I2 * omega2;

        // 能量损失（恢复系数）
        float v_rel = omega1 - omega2;
        float v_rel_new = -restitution * v_rel;

        // 解方程组
        float newOmega1 = (L_total + I2 * v_rel_new) / (I1 + I2);
        float newOmega2 = newOmega1 - v_rel_new;

        return new float[] { newOmega1, newOmega2 };
    }

    // ==================== 实体旋转物理更新 ====================

    /**
     * 更新实体的旋转物理状态
     *
     * @param entity 实体
     */
    public static void updateEntityRotation(Entity entity) {
        RotationalPhysics rp = entity.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        Level level = entity.level();
        float deltaTime = PhysicsConstants.SECONDS_PER_TICK;

        // 1. 应用环境阻尼
        applyEnvironmentalDamping(entity, rp);

        // 2. 更新旋转状态
        rp.update(deltaTime);

        // 3. 同步到实体（如果需要）
        syncRotationToEntity(entity, rp);
    }

    /**
     * 应用环境阻尼
     */
    private static void applyEnvironmentalDamping(Entity entity, RotationalPhysics rp) {
        float damping = AIR_ANGULAR_DAMPING;

        if (entity.isInWater()) {
            damping = WATER_ANGULAR_DAMPING;
        } else if (entity.isInLava()) {
            damping = LAVA_ANGULAR_DAMPING;
        } else if (entity.onGround()) {
            damping = GROUND_ANGULAR_DAMPING;
        }

        // 应用阻尼力矩
        rp.applyTorque(
                -damping * rp.omegaX * Math.abs(rp.omegaX),
                -damping * rp.omegaY * Math.abs(rp.omegaY),
                -damping * rp.omegaZ * Math.abs(rp.omegaZ)
        );
    }

    /**
     * 同步旋转状态到实体
     */
    private static void syncRotationToEntity(Entity entity, RotationalPhysics rp) {
        // 对于船只，同步Y轴旋转
        if (entity instanceof Boat) {
            // 将角速度转换为度/秒，然后应用到实体
            float yRotChange = (float) Math.toDegrees(rp.omegaY) * PhysicsConstants.SECONDS_PER_TICK;
            entity.setYRot(entity.getYRot() + yRotChange);
            entity.setYBodyRot(entity.getYRot());
        }

        // 对于箭矢，计算飞行姿态
        if (entity instanceof AbstractArrow) {
            // 箭矢的旋转主要由弹道决定，这里可以添加额外的旋转效果
        }

        // 对于物品实体，可以添加旋转动画
        if (entity instanceof ItemEntity) {
            // 物品旋转由渲染器处理
        }
    }

    /**
     * 施加碰撞力矩到实体
     *
     * @param entity       实体
     * @param contactPoint 接触点
     * @param force        碰撞力
     */
    public static void applyCollisionToEntity(Entity entity, Vec3 contactPoint, Vec3 force) {
        RotationalPhysics rp = entity.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        Vec3 centerOfMass = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
        float[] torque = calculateCollisionTorque(contactPoint, centerOfMass, force);

        rp.applyTorque(torque[0], torque[1], torque[2]);
    }

    /**
     * 计算并应用流体浮力产生的旋转力矩
     * <p>
     * 当浮力中心与质心不重合时，会产生旋转力矩
     *
     * @param entity 实体
     */
    public static void applyBuoyancyTorque(Entity entity) {
        if (!entity.isInWater() && !entity.isInLava()) return;

        RotationalPhysics rp = entity.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 计算浮力中心（假设在浸入体积的几何中心）
        float buoyancyCenterY = (float) (entity.getY() + entity.getBbHeight() * 0.3);

        // 质心位置
        float centerOfMassY = (float) (entity.getY() + entity.getBbHeight() * 0.5);

        // 浮力大小（简化计算）
        float buoyancy = FluidPhysics.WATER_DENSITY * 9.80665f * rp.mass / 1000.0f;

        // 力矩 = 浮力 × 力臂
        float armLength = centerOfMassY - buoyancyCenterY;
        float torque = buoyancy * armLength * 0.01f; // 缩放因子

        // 如果实体倾斜，浮力会产生恢复力矩
        rp.applyTorque(0, 0, -torque * (float) Math.sin(rp.rotationX));
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查实体是否应该启用旋转物理
     */
    public static boolean shouldEnableRotation(Entity entity) {
        // 船只、物品、箭矢启用旋转
        if (entity instanceof Boat || entity instanceof ItemEntity || entity instanceof AbstractArrow) {
            return true;
        }
        // 其他实体默认不启用
        return false;
    }

    /**
     * 获取实体的旋转信息字符串
     */
    public static String getRotationInfo(Entity entity) {
        RotationalPhysics rp = entity.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null) return "无旋转物理";

        return String.format(
                "角速度: (%.2f, %.2f, %.2f) rad/s\n" +
                "转动惯量: (%.4f, %.4f, %.4f) kg·m²\n" +
                "角动量: %.4f kg·m²/s\n" +
                "转动动能: %.4f J",
                rp.omegaX, rp.omegaY, rp.omegaZ,
                rp.Ixx, rp.Iyy, rp.Izz,
                rp.getAngularMomentumMagnitude(),
                rp.getRotationalKineticEnergy()
        );
    }

    /**
     * 角度转弧度
     */
    public static float degreesToRadians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /**
     * 弧度转角度
     */
    public static float radiansToDegrees(float radians) {
        return (float) Math.toDegrees(radians);
    }
}
