package org.polaris2023.gtu.space.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class MixinReceivingLevelScreen {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void gtu_space$cancelRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ClientSpaceCache.shouldSuppressLoadingScreen()) {
            ci.cancel();
        }
    }
}
