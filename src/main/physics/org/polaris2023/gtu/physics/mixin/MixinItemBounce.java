package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.collision.CollisionEnergy;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品实体反弹 Mixin
 * <p>
 * 实现物品掉落时的反弹效果：
 * <ul>
 *   <li>落地反弹</li>
 *   <li>碰撞墙壁反弹</li>
 *   <li>不同材质物品的弹性差异</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemBounce {

    /**
     * 处理物品碰撞反弹
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onItemTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) return;

        // 获取旋转物理
        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());

        // 处理地面反弹
        if (self.onGround()) {
            gtu_physic$handleGroundBounce(self, rp);
        }

        // 处理墙壁碰撞
        if (self.horizontalCollision) {
            gtu_physic$handleWallBounce(self, rp);
        }
    }

    /**
     * 处理地面反弹
     */
    @Unique
    private void gtu_physic$handleGroundBounce(ItemEntity self, RotationalPhysics rp) {
        Vec3 velocity = self.getDeltaMovement();

        // 检查是否刚落地（垂直速度向下）
        if (velocity.y >= -0.01) return;

        // 获取恢复系数
        float restitution = CollisionEnergy.getRestitution(self);

        // 根据物品材质调整弹性
        String itemId = self.getItem().getItem().builtInRegistryHolder().key().location().toString();
        restitution = adjustRestitutionForItem(restitution, itemId);

        // 计算反弹速度
        float bounceVel = (float) (-velocity.y * restitution);

        // 只有足够快的碰撞才会反弹
        if (bounceVel > 0.05) {
            // 水平速度因摩擦减少
            double friction = 0.8;
            double newVx = velocity.x * friction;
            double newVz = velocity.z * friction;

            self.setDeltaMovement(new Vec3(newVx, bounceVel, newVz));

            // 应用旋转
            if (rp != null && rp.enabled) {
                gtu_physics$applyBounceRotation(rp, velocity, bounceVel);
            }
        } else {
            // 停止垂直运动
            self.setDeltaMovement(new Vec3(velocity.x * 0.8, 0, velocity.z * 0.8));
        }
    }

    /**
     * 处理墙壁反弹
     */
    @Unique
    private void gtu_physic$handleWallBounce(ItemEntity self, RotationalPhysics rp) {
        Vec3 velocity = self.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (horizontalSpeed < 0.05) return;

        float restitution = CollisionEnergy.getRestitution(self);
        String itemId = self.getItem().getItem().builtInRegistryHolder().key().location().toString();
        restitution = adjustRestitutionForItem(restitution, itemId);

        // 墙壁反弹（水平方向）
        double bounceFactor = restitution * 0.5;
        self.setDeltaMovement(new Vec3(-velocity.x * bounceFactor, velocity.y, -velocity.z * bounceFactor));

        // 应用旋转
        if (rp != null && rp.enabled) {
            rp.applyTorque(
                    (float) (velocity.z * 2),
                    0,
                    (float) (-velocity.x * 2)
            );
        }
    }

    /**
     * 根据物品材质调整恢复系数
     */
    private float adjustRestitutionForItem(float baseRestitution, String itemId) {
        // 弹性物品
        if (itemId.contains("slime_ball") || itemId.contains("honeycomb")) {
            return 0.7f;  // 粘液球高弹性
        }
        if (itemId.contains("rubber") || itemId.contains("bounce")) {
            return 0.8f;
        }

        // 硬质物品
        if (itemId.contains("diamond") || itemId.contains("emerald") ||
            itemId.contains("iron") || itemId.contains("gold")) {
            return 0.4f;  // 宝石/金属中等弹性
        }

        // 软质物品
        if (itemId.contains("wool") || itemId.contains("feather") ||
            itemId.contains("leather") || itemId.contains("cloth")) {
            return 0.1f;  // 软质物品几乎不反弹
        }

        // 木制品
        if (itemId.contains("wood") || itemId.contains("stick") ||
            itemId.contains("plank") || itemId.contains("log")) {
            return 0.3f;
        }

        // 石制品
        if (itemId.contains("stone") || itemId.contains("cobblestone") ||
            itemId.contains("rock")) {
            return 0.25f;
        }

        return baseRestitution;
    }

    /**
     * 应用反弹旋转
     */
    @Unique
    private void gtu_physics$applyBounceRotation(RotationalPhysics rp, Vec3 velocity, float bounceVel) {
        // 落地时产生滚动旋转
        float rollTorque = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * bounceVel * 3.0f;

        if (Math.abs(velocity.x) > Math.abs(velocity.z)) {
            rp.applyTorque(0, 0, -rollTorque * (float) Math.signum(velocity.x));
        } else {
            rp.applyTorque(rollTorque * (float) Math.signum(velocity.z), 0, 0);
        }
    }
}
