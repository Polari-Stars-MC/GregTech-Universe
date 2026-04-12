package org.polaris2023.gtu.physics.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.polaris2023.gtu.physics.load.PlayerLoadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin 到 LivingEntity 类
 * <p>
 * 应用负重对移动速度的影响
 */
@Mixin(LivingEntity.class)
public abstract class MixinPlayerLoad {

    /**
     * 使用 MixinExtras 的 ModifyExpressionValue 修改移动速度
     * <p>
     * 在 travel 方法中获取移动速度时应用负重修正
     * 注: Player 类重写了 getSpeed() 方法返回 MOVEMENT_SPEED 属性值
     */
    @ModifyExpressionValue(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getSpeed()F")
    )
    private float gtu_physics$modifyTravelSpeed(float original) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 只对玩家应用负重修正
        if (self instanceof Player player) {
            return original * PlayerLoadManager.getSpeedModifier(player);
        }

        return original;
    }
}
