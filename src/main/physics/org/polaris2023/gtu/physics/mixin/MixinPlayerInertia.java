package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.player.Player;
import org.polaris2023.gtu.physics.inertia.InertiaManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 Player 类
 * <p>
 * 实现惯性系统：玩家滞空时无法控制移动
 */
@Mixin(Player.class)
public abstract class MixinPlayerInertia {

    /**
     * 在 tick 结束后应用惯性
     * <p>
     * 如果玩家滞空，应用空气阻力而不是玩家输入
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_physics$applyInertia(CallbackInfo ci) {
        Player self = (Player) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) {
            return;
        }

        // 应用惯性（如果滞空）
        InertiaManager.applyInertia(self);
    }
}
