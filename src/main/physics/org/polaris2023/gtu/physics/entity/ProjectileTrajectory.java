package org.polaris2023.gtu.physics.entity;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

/**
 * 投掷物弹道物理计算
 * <p>
 * 实现带空气阻力的抛物线运动
 * <p>
 * 物理模型:
 * - 重力: a_y = -g (向下)
 * - 空气阻力: F_drag = -0.5 * ρ * v² * C_d * A * (v̂)
 * - 加速度: a = g + F_drag / m
 * <p>
 * 其中:
 * - ρ = 空气密度 (维度相关)
 * - v = 速度
 * - C_d = 阻力系数
 * - A = 横截面积
 * - m = 质量
 * - v̂ = 速度单位向量
 */
public class ProjectileTrajectory {

    /**
     * 标准空气密度 (kg/m³) - 海平面
     */
    private static final float STANDARD_AIR_DENSITY = 1.225f;

    /**
     * 计算下一个 tick 的速度
     *
     * @param projectile 投掷物
     * @param currentVel 当前速度 (m/tick)
     * @param deltaTime  时间步长 (秒)
     * @return 新速度 (m/tick)
     */
    public static Vec3 calculateVelocity(Projectile projectile, Vec3 currentVel, float deltaTime) {
        Level level = projectile.level();
        if (level.isClientSide()) {
            return currentVel;
        }

        // 获取物理参数
        ProjectilePhysics physics = projectile.getData(DataAttachments.PROJECTILE_PHYSICS.get());
        DimensionPhysics dimPhysics = level.getData(DataAttachments.DIMENSION_PHYSICS.get());

        if (physics == null || dimPhysics == null) {
            return applyVanillaGravity(currentVel, dimPhysics);
        }

        // 空气密度 (根据维度调整)
        float airDensity = getAirDensity(dimPhysics);

        // 计算速度大小
        double speed = currentVel.length();

        if (speed < 0.001) {
            // 速度太小，只应用重力
            return currentVel.add(0, -dimPhysics.gravity * deltaTime, 0);
        }

        // 计算阻力加速度大小
        // a_drag = 0.5 * ρ * v² * C_d * A / m
        double dragAccel = 0.5 * airDensity * speed * speed *
                physics.dragCoefficient * physics.crossSectionArea / physics.mass;

        // 阻力方向与速度相反
        Vec3 dragForce = currentVel.normalize().scale(-dragAccel * deltaTime);

        // 重力加速度
        Vec3 gravityAccel = new Vec3(0, -dimPhysics.gravity * deltaTime, 0);

        // 新速度 = 当前速度 + 重力 + 阻力
        return currentVel.add(gravityAccel).add(dragForce);
    }

    /**
     * 应用原版重力 (无阻力)
     */
    private static Vec3 applyVanillaGravity(Vec3 currentVel, DimensionPhysics dimPhysics) {
        float gravity = dimPhysics != null ? dimPhysics.gravity : 9.80665f;
        return currentVel.add(0, -gravity * 0.05f, 0);
    }

    /**
     * 根据维度获取空气密度
     */
    private static float getAirDensity(DimensionPhysics physics) {
        // 基于空气阻力系数估算空气密度
        // 主世界: 1.225 kg/m³
        // 下界: 更高 (炎热致密) ~1.5
        // 末地: 更低 (虚空) ~0.1
        // 也可以从维度配置读取

        return STANDARD_AIR_DENSITY * (1.0f + physics.airResistance * 10);
    }

    /**
     * 计算最大射程 (无阻力情况下的理论值)
     *
     * @param speed     初始速度 (m/s)
     * @param angleDeg  发射角度 (度)
     * @param gravity   重力加速度 (m/s²)
     * @return 最大射程 (米)
     */
    public static double calculateMaxRange(double speed, double angleDeg, float gravity) {
        double angleRad = Math.toRadians(angleDeg);
        return (speed * speed * Math.sin(2 * angleRad)) / gravity;
    }

    /**
     * 计算最大高度 (无阻力情况下的理论值)
     */
    public static double calculateMaxHeight(double speed, double angleDeg, float gravity) {
        double angleRad = Math.toRadians(angleDeg);
        return (speed * speed * Math.sin(angleRad) * Math.sin(angleRad)) / (2 * gravity);
    }

    /**
     * 估算落地时间 (无阻力情况下的理论值)
     */
    public static double estimateFlightTime(double speed, double angleDeg, float gravity) {
        double angleRad = Math.toRadians(angleDeg);
        return (2 * speed * Math.sin(angleRad)) / gravity;
    }

    /**
     * 初始化投掷物物理状态
     */
    public static void initializeProjectile(Projectile projectile, double initialSpeed) {
        ProjectilePhysics physics = projectile.getData(DataAttachments.PROJECTILE_PHYSICS.get());
        if (physics != null && !physics.initialized) {
            physics.initialSpeed = (float) initialSpeed;
            physics.initialized = true;
        }
    }

    /**
     * 检查投掷物是否应该使用物理弹道
     */
    public static boolean shouldUsePhysicsTrajectory(Projectile projectile) {
        // 箭矢和投掷物使用物理弹道
        return projectile instanceof AbstractArrow || projectile instanceof ThrowableProjectile;
    }
}
