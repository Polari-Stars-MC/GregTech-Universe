package org.polaris2023.gtu.modpacks.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import org.polaris2023.gtu.modpacks.data.DataAttachmentTypes;

/**
 * 修改水的流动速度
 * <p>
 * 可调整的参数:
 * - getTickDelay: 流体更新间隔 (tick), 值越小流动越快
 * - getDropOff: 每格液位下降, 值越小流动越远
 * - getSlopeFindDistance: 寻坡距离, 影响流动范围
 */
@Mixin(WaterFluid.class)
public abstract class MixinWaterFluid {

    /**
     * 修改流体更新间隔
     * <p>
     * 原版值: 5 ticks
     */
    @Inject(method = "getTickDelay", at = @At("RETURN"), cancellable = true)
    private void gtu_space$modifyTickDelay(LevelReader levelReader, CallbackInfoReturnable<Integer> cir) {
        int waterSpeed = gtu_modpacks$getWaterSpeed(levelReader);
        if (waterSpeed <= 0) return;
        cir.setReturnValue(Math.max(1, cir.getReturnValue() - waterSpeed));
    }

//    /**
//     * 修改液位下降速度
//     * 原版值: 1
//     * 值越小, 水流越远
//     */
//    @Inject(method = "getDropOff", at = @At("HEAD"), cancellable = true)
//    private void gtu_space$modifyDropOff(CallbackInfoReturnable<Integer> cir) {
//        // 原版 1, 改为 0 让水流无限远 (或保持 1)
//        cir.setReturnValue(1);
//    }

//    /**
//     * 修改寻坡距离
//     * 原版值: 4
//     * 值越大, 水能找到更远的斜坡
//     */
    @Inject(method = "getSlopeFindDistance", at = @At("RETURN"), cancellable = true)
    private void gtu_modpacks$modifySlopeDistance(LevelReader levelReader, CallbackInfoReturnable<Integer> cir) {
        int waterSpeed = gtu_modpacks$getWaterSpeed(levelReader);
        if (waterSpeed <= 0) return;
        cir.setReturnValue(Math.min(8, cir.getReturnValue() + waterSpeed * 2));
    }

    private static int gtu_modpacks$getWaterSpeed(LevelReader levelReader) {
        if (!(levelReader instanceof Level level)) {
            return 0;
        }
        return Mth.clamp(level.getData(DataAttachmentTypes.WATER_SPEED.get()), 0, 3);
    }
}
