package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 ThrowableProjectile 类
 * <p>
 * 实现投掷物（雪球、末影珍珠等）的空气阻力
 */
@Mixin(ThrowableProjectile.class)
public abstract class MixinThrowableProjectile {

    @Unique
    private boolean gtu_physics$initialized = false;

    /**
     * 在 tick 结束后应用空气阻力
     * <p>
     * 原版重力已经由 Projectile.tick() 应用，我们只添加空气阻力效果
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_physics$applyAirResistance(CallbackInfo ci) {
        ThrowableProjectile self = (ThrowableProjectile) (Object) this;

        if (self.level().isClientSide()) {
            return;
        }

        DimensionPhysics dimPhysics = self.level().getData(DataAttachments.DIMENSION_PHYSICS.get());
        if (dimPhysics == null) {
            return;
        }

        // 初始化
        if (!gtu_physics$initialized) {
            gtu_physics$initialized = true;
        }

        // 获取当前速度
        Vec3 velocity = self.getDeltaMovement();
        double speed = velocity.length();

        if (speed < 0.001) {
            return;
        }

        var physics = self.getData(DataAttachments.PROJECTILE_PHYSICS.get());
        if (physics == null) {
            return;
        }

        // 空气密度
        float airDensity = 1.225f * (1.0f + dimPhysics.airResistance * 10);

        // 阻力加速度: a = 0.5 * ρ * v² * C_d * A / m
        double dragAccel = 0.5 * airDensity * speed * speed *
                physics.dragCoefficient * physics.crossSectionArea / physics.mass;

        // 阻力方向与速度相反
        Vec3 drag = velocity.normalize().scale(-dragAccel * 0.05);

        // 应用空气阻力
        self.setDeltaMovement(velocity.add(drag));
    }
}
