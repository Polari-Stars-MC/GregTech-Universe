package org.polaris2023.gtu.space.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import org.polaris2023.gtu.space.client.portal.ClientPortalCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Inject(method = "allChanged", at = @At("HEAD"))
    private void gtu_space$clearPortalCacheOnReload(CallbackInfo ci) {
        ClientPortalCache.clear();
    }
}
