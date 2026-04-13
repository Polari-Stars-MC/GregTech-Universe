package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.polaris2023.gtu.physics.rotation.RotationalPhysicsCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 船只旋转物理 Mixin
 * <p>
 * 增强船只在水面上的旋转物理效果，包括：
 * <ul>
 *   <li>波浪引起的摇晃</li>
 *   <li>碰撞后的旋转</li>
 *   <li>转向时的倾斜</li>
 * </ul>
 */
@Mixin(Boat.class)
public abstract class MixinBoatRotation {
    /**
     * 在船只tick时应用旋转物理
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onBoatTick(CallbackInfo ci) {
        Boat self = (Boat) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) return;

        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 应用波浪摇晃效果
        applyWaveRocking(self, rp);

        // 应用转向倾斜
        applyTurningTilt(self, rp);

        // 更新旋转状态
        rp.update(PhysicsConstants.SECONDS_PER_TICK);

        // 同步Y轴旋转到实体
        syncYRotation(self, rp);
    }

    /**
     * 应用波浪摇晃效果
     * <p>
     * 船只在水面会因波浪产生轻微的摇晃
     */
    private void applyWaveRocking(Boat self, RotationalPhysics rp) {
        if (!self.isInWater()) return;

        long gameTime = self.level().getGameTime();

        // 使用正弦波模拟波浪效果
        float waveX = (float) Math.sin(gameTime * 0.1 + self.getX()) * 0.01f;
        float waveZ = (float) Math.cos(gameTime * 0.13 + self.getZ()) * 0.01f;

        // 波浪产生的恢复力矩（使船保持水平）
        float restoreX = -rp.rotationX * 0.5f;
        float restoreZ = -rp.rotationZ * 0.5f;

        rp.applyTorque(waveX + restoreX, 0, waveZ + restoreZ);
    }

    /**
     * 应用转向倾斜效果
     * <p>
     * 船只转向时会产生侧向倾斜（离心力效果）
     */
    private void applyTurningTilt(Boat self, RotationalPhysics rp) {
        // 获取角速度变化率来判断是否在转向
        float deltaRotation = self.getYRot() - self.yRotO;

        if (Math.abs(deltaRotation) > 0.1f) {
            // 转向时产生侧倾
            float tiltTorque = deltaRotation * 0.001f;
            rp.applyTorque(0, 0, tiltTorque);
        }
    }

    /**
     * 同步Y轴旋转
     */
    private void syncYRotation(Boat self, RotationalPhysics rp) {
        // 将角速度转换为角度变化
        float yRotChange = RotationalPhysicsCalculator.radiansToDegrees(rp.omegaY) * PhysicsConstants.SECONDS_PER_TICK;

        if (Math.abs(yRotChange) > 0.01f) {
            self.setYRot(self.getYRot() + yRotChange);
        }
    }

    /**
     * 碰撞时应用旋转力矩
     */
    @Inject(method = "push", at = @At("TAIL"))
    private void onPush(Entity entity, CallbackInfo ci) {
        Boat self = (Boat) (Object) this;

        if (self.level().isClientSide()) return;

        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 碰撞产生旋转力矩
        float dx = (float) (entity.getX() - self.getX());
        float dz = (float) (entity.getZ() - self.getZ());

        // 侧向碰撞产生Y轴旋转
        float torqueY = (dx * (float) entity.getDeltaMovement().z - dz * (float) entity.getDeltaMovement().x) * 0.1f;
        rp.applyTorque(0, torqueY, 0);
    }
}
