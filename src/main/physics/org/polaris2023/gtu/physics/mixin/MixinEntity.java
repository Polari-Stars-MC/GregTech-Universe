package org.polaris2023.gtu.physics.mixin;

import com.jme3.math.Vector3f;
import net.minecraft.world.entity.Entity;
import org.polaris2023.gtu.physics.world.PhysicsManager;
import org.polaris2023.gtu.physics.world.PhysicsWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 Entity 类
 * <p>
 * 在实体 tick 开始时，将玩家输入的移动同步到物理引擎。
 * 物理引擎计算后的位置由 PhysicsManager.tickPhysics() 同步回实体。
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    /**
     * 在 tick 开始时同步实体速度到物理引擎
     * <p>
     * 这确保玩家的输入（跳跃、移动等）能影响物理引擎
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void gtu_physics$onTickStart(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) {
            return;
        }

        // 将玩家输入的速度同步到物理引擎
        PhysicsWorld physicsWorld = PhysicsManager.getOrCreatePhysicsWorld(self.level());
        if (physicsWorld != null) {
            var body = physicsWorld.getEntityBody(self.getId());
            if (body != null) {
                // 同步 Minecraft 实体的速度到 Bullet 刚体
                // 这允许玩家的移动输入影响物理模拟
                var velocity = self.getDeltaMovement();
                body.setLinearVelocity(new Vector3f(
                        (float) velocity.x,
                        (float) velocity.y,
                        (float) velocity.z
                ));
            }
        }
    }
}
