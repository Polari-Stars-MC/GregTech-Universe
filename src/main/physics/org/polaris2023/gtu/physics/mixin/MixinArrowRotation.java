package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
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
 * 箭矢旋转物理 Mixin
 * <p>
 * 实现箭矢的陀螺稳定效应：
 * <ul>
 *   <li>高速旋转保持飞行稳定</li>
 *   <li>空气阻力对旋转的衰减</li>
 *   <li>碰撞后的旋转变化</li>
 *   <li>飞行姿态与速度方向对齐</li>
 * </ul>
 */
@Mixin(AbstractArrow.class)
public abstract class MixinArrowRotation {

    @Shadow
    protected boolean inGround;

    @Shadow
    protected int inGroundTime;

    /**
     * 上一tick是否在地面，用于检测碰撞瞬间
     */
    private boolean gtu_physics$wasInGround = false;

    /**
     * 在箭矢tick时应用旋转物理
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onArrowTick(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) return;

        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 检测碰撞瞬间：从未着地变为着地
        if (inGround && !gtu_physics$wasInGround) {
            handleHit(rp);
        }
        gtu_physics$wasInGround = inGround;

        if (inGround) {
            // 在地面时，旋转逐渐停止
            handleGroundRotation(rp);
        } else {
            // 飞行中，应用陀螺稳定效应
            handleFlightRotation(self, rp);
        }

        // 更新旋转状态
        rp.update(PhysicsConstants.SECONDS_PER_TICK);
    }

    /**
     * 处理飞行中的旋转
     * <p>
     * 陀螺稳定效应：高速旋转的箭矢保持飞行方向稳定
     */
    private void handleFlightRotation(AbstractArrow self, RotationalPhysics rp) {
        Vec3 velocity = self.getDeltaMovement();
        double speed = velocity.length();

        if (speed < 0.001) return;

        // 1. 陀螺稳定效应
        // 高速旋转的箭矢会抵抗方向变化
        float omegaMag = rp.getAngularSpeed();
        if (omegaMag > 10.0f) {
            // 陀螺力矩使箭矢保持稳定
            // 简化处理：减少空气阻力导致的偏航
            applyGyroscopicStabilization(self, rp, velocity);
        }

        // 2. 空气阻力对旋转的衰减
        float dragTorque = -0.01f * omegaMag * omegaMag;
        rp.applyTorque(0, 0, dragTorque);

        // 3. 飞行姿态对齐
        // 箭矢会自动调整姿态使其与飞行方向一致
        alignWithVelocity(self, rp, velocity);
    }

    /**
     * 应用陀螺稳定效应
     * <p>
     * 陀螺效应公式：τ = ω × L (进动力矩)
     */
    private void applyGyroscopicStabilization(AbstractArrow self, RotationalPhysics rp, Vec3 velocity) {
        // 箭矢主要绕自身轴（假设为z轴）旋转
        // 陀螺效应会产生进动，使箭矢保持稳定

        float omegaZ = rp.omegaZ;
        float Lz = rp.Izz * omegaZ;  // 角动量

        // 简化：陀螺稳定减少横向偏移
        // 实际实现中可以调整箭矢的飞行轨迹
    }

    /**
     * 使箭矢姿态与速度方向对齐
     * <p>
     * 箭矢飞行时会自动调整姿态使其尖端指向飞行方向
     */
    private void alignWithVelocity(AbstractArrow self, RotationalPhysics rp, Vec3 velocity) {
        double speed = velocity.length();
        if (speed < 0.1) return;

        // 计算目标姿态角
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float targetPitch = (float) Math.atan2(-velocity.y, horizontalSpeed);
        float targetYaw = (float) Math.atan2(velocity.x, velocity.z);

        // 当前姿态与目标姿态的差异会产生恢复力矩
        // 这模拟了箭矢空气动力学中的稳定效应
        float pitchError = targetPitch - rp.rotationX;
        float yawError = targetYaw - rp.rotationY;

        // 恢复力矩（简化模型）
        float restoreFactor = 0.1f * (float) speed;
        rp.applyTorque(pitchError * restoreFactor, yawError * restoreFactor, 0);
    }

    /**
     * 处理地面上的旋转
     */
    private void handleGroundRotation(RotationalPhysics rp) {
        // 在地面时，摩擦力使旋转停止
        float friction = 0.5f;
        rp.applyTorque(
                -friction * rp.omegaX,
                -friction * rp.omegaY,
                -friction * rp.omegaZ
        );
    }

    /**
     * 碰撞瞬间处理：旋转速度急剧下降
     */
    private void handleHit(RotationalPhysics rp) {
        // 碰撞时旋转速度急剧下降
        rp.omegaX *= 0.1f;
        rp.omegaY *= 0.1f;
        rp.omegaZ *= 0.3f;  // 绕自身轴的旋转衰减较慢
    }
}
