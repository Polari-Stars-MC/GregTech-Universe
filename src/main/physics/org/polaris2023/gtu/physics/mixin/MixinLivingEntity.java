package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin 到 LivingEntity 类
 * <p>
 * 禁用原版重力计算，使用 Bullet 物理引擎接管
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * 修改 travel 方法中的重力值
     * 我们将其设为 0，让 Bullet 物理引擎接管
     */
    @ModifyVariable(
            method = "travel",
            at = @At(value = "LOAD", ordinal = 0),
            ordinal = 2
    )
    private double gtu_physics$disableGravity(double gravity) {
        // 禁用原版重力，Bullet 会接管
        return 0.0;
    }
}
