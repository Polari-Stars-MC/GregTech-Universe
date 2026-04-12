package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.Entity;
import org.polaris2023.gtu.physics.world.PhysicsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 Entity 类
 * <p>
 * 禁用原版重力，使用 Bullet 物理引擎接管
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    /**
     * 在 tick 结束后同步实体位置到物理引擎
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_physics$onTickEnd(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) {
            return;
        }

        // 只同步实体位置到物理引擎（单向），不覆盖 MC 实体位置
        PhysicsManager.syncEntityToPhysics(self);
    }
}
