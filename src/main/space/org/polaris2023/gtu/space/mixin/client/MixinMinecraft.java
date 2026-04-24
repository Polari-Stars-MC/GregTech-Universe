package org.polaris2023.gtu.space.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void gtu_space$suppressReceivingLevelScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof ReceivingLevelScreen && ClientSpaceCache.shouldSuppressLoadingScreen()) {
            ci.cancel();
        }
    }
}
