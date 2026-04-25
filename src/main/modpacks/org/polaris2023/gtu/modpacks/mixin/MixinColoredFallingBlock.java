package org.polaris2023.gtu.modpacks.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.polaris2023.gtu.core.init.ModProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinColoredFallingBlock {

    @Unique
    private final Block gtu$modpacks$self = (Block) (Object) this;

    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
    private void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        if (gtu$modpacks$self instanceof ColoredFallingBlock) {
            builder.add(ModProperties.PLACE);
        }
    }

    @Inject(method = "defaultBlockState", at = @At("RETURN"), cancellable = true)
    private void defaultBlockState(CallbackInfoReturnable<BlockState> cir) {
        if (gtu$modpacks$self instanceof ColoredFallingBlock) {
            BlockState returnValue = cir.getReturnValue();
            if (returnValue.hasProperty(ModProperties.PLACE)) {
                cir.setReturnValue(returnValue.setValue(ModProperties.PLACE, false));
            }
        }
    }
}
