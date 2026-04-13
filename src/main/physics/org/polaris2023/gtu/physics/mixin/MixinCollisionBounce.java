package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.collision.CollisionEnergy;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体碰撞反弹 Mixin
 * <p>
 * 处理实体与其他实体/方块碰撞后的反弹效果
 */
@Mixin(Entity.class)
public abstract class MixinCollisionBounce {

    @Shadow public abstract Vec3 getDeltaMovement();

    @Shadow public abstract void setDeltaMovement(Vec3 velocity);

    @Shadow public abstract boolean onGround();

    @Shadow public abstract boolean isInWater();

    @Shadow public abstract boolean isInLava();

    /**
     * 处理实体碰撞后的反弹
     */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void onEntityCollision(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) return;

        // 跳过某些实体类型
        if (shouldSkipCollision(self, other)) return;

        // 计算碰撞参数
        Vec3 posSelf = self.position();
        Vec3 posOther = other.position();

        // 碰撞法线（从other指向self）
        Vec3 normal = posSelf.subtract(posOther).normalize();
        if (normal.lengthSqr() < 0.001) {
            normal = new Vec3(1, 0, 0);  // 默认方向
        }

        // 相对速度
        Vec3 velSelf = this.getDeltaMovement();
        Vec3 velOther = other.getDeltaMovement();
        Vec3 relativeVel = velSelf.subtract(velOther);

        // 只处理接近的碰撞
        double relativeVelN = relativeVel.dot(normal);
        if (relativeVelN >= 0) return;

        // 获取质量
        float massSelf = CollisionEnergy.getEntityMass(self);
        float massOther = CollisionEnergy.getEntityMass(other);

        // 获取恢复系数
        float restitutionSelf = CollisionEnergy.getRestitution(self);
        float restitutionOther = CollisionEnergy.getRestitution(other);
        float restitution = (restitutionSelf + restitutionOther) / 2.0f;

        // 史莱姆弹跳增强
        restitution *= CollisionEnergy.getSlimeBounceMultiplier(self);
        restitution *= CollisionEnergy.getSlimeBounceMultiplier(other);

        // 计算冲量
        float impulse = CollisionEnergy.calculateImpulse(massSelf, massOther, (float) relativeVelN, restitution);

        // 应用冲量
        Vec3 impulseVec = normal.scale(impulse);
        Vec3 newVelSelf = velSelf.add(impulseVec.scale(1.0 / massSelf));

        this.setDeltaMovement(newVelSelf);

        // 应用旋转力矩
        applyCollisionRotation(self, other, impulseVec);
    }

    /**
     * 处理与方块的碰撞反弹
     */
    @Inject(method = "move", at = @At("TAIL"))
    private void onBlockCollision(MoverType type, Vec3 pos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) return;

        // 只在碰撞时处理
        if (!self.horizontalCollision && !self.verticalCollision) return;

        // 获取恢复系数
        float restitution = CollisionEnergy.getRestitution(self);

        // 垂直碰撞（地面/天花板）
        if (self.verticalCollision) {
            Vec3 velocity = this.getDeltaMovement();

            // 落地反弹
            if (velocity.y < -0.1 && self.onGround()) {
                // 史莱姆等弹性实体的弹跳
                float bounceMultiplier = CollisionEnergy.getSlimeBounceMultiplier(self);
                float bounceVel = (float) (-velocity.y * restitution * bounceMultiplier);

                // 只有足够快的碰撞才会反弹
                if (bounceVel > 0.1) {
                    this.setDeltaMovement(new Vec3(velocity.x, bounceVel, velocity.z));

                    // 应用旋转（落地时的滚动）
                    applyGroundBounceRotation(self, bounceVel);
                }
            }
        }

        // 水平碰撞（墙壁）
        if (self.horizontalCollision) {
            Vec3 velocity = this.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

            // 高速碰撞墙壁时反弹
            if (horizontalSpeed > 0.3 && restitution > 0.1) {
                // 反弹方向
                double bounceX = -velocity.x * restitution * 0.5;
                double bounceZ = -velocity.z * restitution * 0.5;

                this.setDeltaMovement(new Vec3(bounceX, velocity.y, bounceZ));
            }
        }
    }

    /**
     * 应用碰撞产生的旋转
     */
    private void applyCollisionRotation(Entity self, Entity other, Vec3 impulseVec) {
        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        // 碰撞点相对于质心的位置
        Vec3 contactPoint = self.position().add(other.position()).scale(0.5);
        Vec3 r = contactPoint.subtract(self.position());

        // 力矩 = r × F
        float torqueX = (float) (r.y * impulseVec.z - r.z * impulseVec.y);
        float torqueY = (float) (r.z * impulseVec.x - r.x * impulseVec.z);
        float torqueZ = (float) (r.x * impulseVec.y - r.y * impulseVec.x);

        rp.applyTorque(torqueX * 0.1f, torqueY * 0.1f, torqueZ * 0.1f);
    }

    /**
     * 应用落地反弹时的旋转
     */
    private void applyGroundBounceRotation(Entity self, float bounceVel) {
        RotationalPhysics rp = self.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        Vec3 velocity = this.getDeltaMovement();

        // 落地时产生滚动旋转
        // 绕垂直于运动方向的轴旋转
        float rollTorque = (float) (Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * bounceVel * 0.5f);

        // 根据运动方向确定旋转轴
        if (Math.abs(velocity.x) > Math.abs(velocity.z)) {
            rp.applyTorque(0, 0, -rollTorque * (float) Math.signum(velocity.x));
        } else {
            rp.applyTorque(rollTorque * (float) Math.signum(velocity.z), 0, 0);
        }
    }

    /**
     * 检查是否应该跳过碰撞处理
     */
    private boolean shouldSkipCollision(Entity self, Entity other) {
        // 玩家之间的碰撞不处理
        if (self instanceof Player && other instanceof Player) {
            return true;
        }

        // 箭矢碰撞由专门的逻辑处理
        if (self instanceof AbstractArrow || other instanceof AbstractArrow) {
            return true;
        }

        return false;
    }
}
