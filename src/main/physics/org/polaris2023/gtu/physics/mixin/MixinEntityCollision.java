package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.player.Player;
import org.polaris2023.gtu.physics.collision.EntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 Player 类
 * <p>
 * 在玩家 tick 时应用实体推挤力，使玩家移动时能推动其他实体
 */
@Mixin(Player.class)
public abstract class MixinEntityCollision {

    /**
     * 在玩家 tick 结束后应用推挤力
     * <p>
     * 当玩家移动时，推开前方的其他实体
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void gtu_physics$applyPushForce(CallbackInfo ci) {
        Player self = (Player) (Object) this;

        // 只在服务端处理
        if (self.level().isClientSide()) {
            return;
        }

        // 应用玩家推挤力
        EntityCollisionManager.getInstance().applyPlayerPushForce(self, self.level());
    }
}
