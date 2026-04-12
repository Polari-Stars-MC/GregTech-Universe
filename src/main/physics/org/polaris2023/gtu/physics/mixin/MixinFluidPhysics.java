package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.Entity;
import org.polaris2023.gtu.physics.fluid.FluidPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 Entity 类
 * <p>
 * 在实体 tick 后应用流体物理效果（浮力、流体阻力、水流推力）
 */
@Mixin(Entity.class)
public abstract class MixinFluidPhysics {

    /**
     * 在 tick 结束后应用流体物理
     * <p>
     * 仅在实体处于流体中时应用
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_physics$applyFluidPhysics(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) {
            return;
        }

        // 检查是否在流体中
        if (self.isInWater() || self.isInLava()) {
            // 应用流体物理效果
            FluidPhysics.applyFluidPhysics(self);
        }
    }
}
