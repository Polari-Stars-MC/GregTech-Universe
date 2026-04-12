package org.polaris2023.gtu.physics.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.polaris2023.gtu.physics.load.BlockFrictionManager;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin 到 LivingEntity 类
 * <p>
 * 应用基于方块材质的摩擦力系统
 * <p>
 * 摩擦力模型：
 * - 特殊方块（冰、粘液、蜂蜜等）保留原版值
 * - 普通方块根据材质计算摩擦力
 * - 最终值乘以维度摩擦系数
 */
@Mixin(LivingEntity.class)
public abstract class MixinGroundFriction {

    /**
     * 使用 MixinExtras 的 ModifyExpressionValue 修改方块摩擦力
     * <p>
     * 在 LivingEntity.travel() 方法中拦截 BlockState.getFriction() 调用
     */
    @ModifyExpressionValue(
            method = "travel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F")
    )
    private float gtu_physics$modifyBlockFriction(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();

        // 获取玩家脚下的方块位置
        BlockPos pos = self.blockPosition();
        BlockState state = level.getBlockState(pos);

        // 通过 BlockFrictionManager 计算摩擦力（混合模式）
        float friction = BlockFrictionManager.getFriction(state, level, pos, self, original);

        // 应用维度摩擦系数
        DimensionPhysics physics = level.getData(org.polaris2023.gtu.physics.init.DataAttachments.DIMENSION_PHYSICS);
        if (physics != null) {
            friction *= physics.groundFriction;
        }

        return friction;
    }
}
