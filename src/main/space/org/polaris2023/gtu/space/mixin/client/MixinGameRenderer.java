package org.polaris2023.gtu.space.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "getDepthFar", at = @At("HEAD"), cancellable = true)
    private void gtu_space$extendSkyDepth(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Float.POSITIVE_INFINITY);
    }
}
