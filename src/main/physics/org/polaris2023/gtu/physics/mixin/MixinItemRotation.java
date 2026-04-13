package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品实体旋转物理 Mixin
 * <p>
 * 实现掉落物品的旋转效果：
 * <ul>
 *   <li>空中下落时的旋转</li>
 *   <li>落地后的滚动</li>
 *   <li>流体中的旋转变化</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemRotation {

    /**
     * 在物品tick时应用旋转物理
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onItemTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) return;

        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 根据状态应用不同的旋转效果
        if (self.isInWater()) {
            handleWaterRotation(self, rp);
        } else if (self.onGround()) {
            handleGroundRotation(self, rp);
        } else {
            handleAirRotation(self, rp);
        }

        // 更新旋转状态
        rp.update(PhysicsConstants.SECONDS_PER_TICK);
    }

    /**
     * 处理空中的旋转
     * <p>
     * 物品在空中下落时会旋转
     */
    private void handleAirRotation(ItemEntity self, RotationalPhysics rp) {
        Vec3 velocity = self.getDeltaMovement();

        // 根据下落速度产生旋转
        // 空气阻力会产生力矩
        float fallSpeed = (float) Math.abs(velocity.y);

        // 随机旋转效果（模拟空气湍流）
        long gameTime = self.level().getGameTime();
        float turbulence = (float) Math.sin(gameTime * 0.3 + self.getX()) * 0.5f;

        rp.applyTorque(
                turbulence * fallSpeed,
                turbulence * fallSpeed * 0.5f,
                fallSpeed * 0.1f
        );

        // 空气阻力衰减旋转
        float airDrag = 0.02f;
        rp.applyTorque(
                -airDrag * rp.omegaX * Math.abs(rp.omegaX),
                -airDrag * rp.omegaY * Math.abs(rp.omegaY),
                -airDrag * rp.omegaZ * Math.abs(rp.omegaZ)
        );
    }

    /**
     * 处理地面上的旋转
     * <p>
     * 物品落地后会滚动然后停止
     */
    private void handleGroundRotation(ItemEntity self, RotationalPhysics rp) {
        Vec3 velocity = self.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // 如果还在移动，继续滚动
        if (horizontalSpeed > 0.01) {
            // 滚动产生的旋转
            // 滚动角速度 = 线速度 / 半径
            float rollOmega = (float) (horizontalSpeed / 0.125) * 2.0f;  // 假设半径0.125m

            // 根据移动方向确定旋转轴
            float moveAngle = (float) Math.atan2(velocity.z, velocity.x);
            rp.applyTorque(
                    rollOmega * (float) Math.cos(moveAngle),
                    0,
                    rollOmega * (float) Math.sin(moveAngle)
            );
        }

        // 地面摩擦使旋转停止
        float groundFriction = 0.3f;
        rp.applyTorque(
                -groundFriction * rp.omegaX,
                -groundFriction * rp.omegaY,
                -groundFriction * rp.omegaZ
        );
    }

    /**
     * 处理水中的旋转
     * <p>
     * 物品在水中旋转较慢，且受浮力影响
     */
    private void handleWaterRotation(ItemEntity self, RotationalPhysics rp) {
        Vec3 velocity = self.getDeltaMovement();

        // 水流产生的旋转
        float flowEffect = (float) (velocity.x * velocity.x + velocity.z * velocity.z) * 0.1f;
        rp.applyTorque(flowEffect, flowEffect * 0.5f, flowEffect);

        // 水的高阻力
        float waterDrag = 0.5f;
        rp.applyTorque(
                -waterDrag * rp.omegaX,
                -waterDrag * rp.omegaY,
                -waterDrag * rp.omegaZ
        );

        // 浮力产生的旋转（如果物品密度不均匀）
        // 简化处理：随机小扰动
        if (self.getRandom().nextFloat() < 0.1f) {
            rp.applyTorque(
                    self.getRandom().nextFloat() * 0.1f,
                    self.getRandom().nextFloat() * 0.1f,
                    self.getRandom().nextFloat() * 0.1f
            );
        }
    }
}
