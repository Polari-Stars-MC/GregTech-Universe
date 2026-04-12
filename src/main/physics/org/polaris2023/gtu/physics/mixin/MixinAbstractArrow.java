package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.entity.ProjectileTrajectory;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 AbstractArrow 类
 * <p>
 * 实现箭矢的物理弹道，包含空气阻力
 */
@Mixin(AbstractArrow.class)
public abstract class MixinAbstractArrow {

    @Unique
    private boolean gtu_physics$initialized = false;

    /**
     * 在 tick 结束后应用物理弹道
     * <p>
     * 原版重力已经在 tick 中应用，我们用物理弹道重新计算速度
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_physics$applyPhysicsTrajectory(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;

        if (self.level().isClientSide()) {
            return;
        }

        DimensionPhysics dimPhysics = self.level().getData(DataAttachments.DIMENSION_PHYSICS.get());
        if (dimPhysics == null) {
            return;
        }

        // 初始化物理状态
        if (!gtu_physics$initialized) {
            gtu_physics$initialized = true;
            Vec3 velocity = self.getDeltaMovement();
            double speed = velocity.length() * 20.0;
            ProjectileTrajectory.initializeProjectile(self, speed);
        }

        // 获取当前速度（已包含原版重力）
        Vec3 currentVelocity = self.getDeltaMovement();

        // 用物理弹道重新计算
        // 注意：这里需要从"当前速度"反推出"应用重力前的速度"，然后重新计算
        // 简化处理：直接在当前速度基础上应用空气阻力
        Vec3 newVelocity = gtu_physics$applyAirResistance(self, currentVelocity, dimPhysics);

        self.setDeltaMovement(newVelocity);
    }

    /**
     * 应用空气阻力
     */
    @Unique
    private Vec3 gtu_physics$applyAirResistance(AbstractArrow arrow, Vec3 velocity, DimensionPhysics dimPhysics) {
        double speed = velocity.length();
        if (speed < 0.001) {
            return velocity;
        }

        var physics = arrow.getData(DataAttachments.PROJECTILE_PHYSICS.get());

        // 空气密度
        float airDensity = 1.225f * (1.0f + dimPhysics.airResistance * 10);

        // 阻力加速度: a = 0.5 * ρ * v² * C_d * A / m
        double dragAccel = 0.5 * airDensity * speed * speed *
                physics.dragCoefficient * physics.crossSectionArea / physics.mass;

        // 阻力方向与速度相反
        Vec3 drag = velocity.normalize().scale(-dragAccel * 0.05);

        return velocity.add(drag);
    }
}
