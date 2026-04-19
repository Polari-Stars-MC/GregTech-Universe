//package org.polaris2023.gtu.space.mixin.client;
//
//import net.minecraft.client.renderer.LightTexture;
//import net.minecraft.world.level.dimension.DimensionType;
//import org.polaris2023.gtu.space.client.render.SpaceClientRenderHooks;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(LightTexture.class)
//public abstract class MixinLightTexture {
//    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
//    private static void gtu$removeVanillaBrightness(DimensionType dimensionType, int lightLevel, CallbackInfoReturnable<Float> cir) {
//        if (SpaceClientRenderHooks.shouldSuppressVanillaSpaceVisuals()) {
//            cir.setReturnValue(0.0F);
//        }
//    }
//}
