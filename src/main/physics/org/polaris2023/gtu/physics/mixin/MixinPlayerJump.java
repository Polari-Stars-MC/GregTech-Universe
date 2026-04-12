package org.polaris2023.gtu.physics.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.polaris2023.gtu.physics.load.PlayerLoadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin 到 LivingEntity 类
 * <p>
 * 应用负重对跳跃高度的影响
 */
@Mixin(LivingEntity.class)
public abstract class MixinPlayerJump {

    /**
     * 使用 MixinExtras 的 ModifyReturnValue 修改跳跃力度
     * <p>
     * 在 getJumpPower() 返回时应用负重修正
     */
    @ModifyReturnValue(
            method = "getJumpPower()F",
            at = @At("RETURN")
    )
    private float gtu_physics$modifyJumpPower(float original) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 只对玩家应用负重修正
        if (self instanceof Player player) {
            return original * PlayerLoadManager.getJumpModifier(player);
        }

        return original;
    }
}
