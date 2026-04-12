package org.polaris2023.gtu.physics.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.polaris2023.gtu.physics.inertia.InertiaManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 LivingEntity 类
 * <p>
 * 拦截移动输入，在滞空时阻止玩家控制
 */
@Mixin(LivingEntity.class)
public abstract class MixinInertiaMovement {

    /**
     * 在 travel 方法开始时检查滞空状态
     * <p>
     * 如果玩家滞空，将移动输入相关字段归零
     */
    @Inject(
            method = "travel",
            at = @At("HEAD")
    )
    private void gtu_physics$onTravelHead(net.minecraft.world.phys.Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 只对玩家生效
        if (!(self instanceof Player player)) {
            return;
        }

        // 如果滞空，禁止移动输入
        if (InertiaManager.isInAirborne(player)) {
            // 清除移动输入 (通过设置 zza 和 xxa 为 0)
            // 注: 这需要在 Player 类中操作
            player.zza = 0.0f;
            player.xxa = 0.0f;
        }
    }
}
