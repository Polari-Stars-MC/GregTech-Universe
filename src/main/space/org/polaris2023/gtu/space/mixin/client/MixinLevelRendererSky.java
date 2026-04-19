//package org.polaris2023.gtu.space.mixin.client;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Camera;
//import net.minecraft.client.renderer.LevelRenderer;
//import org.joml.Matrix4f;
//import org.polaris2023.gtu.space.client.render.SpaceClientRenderHooks;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(LevelRenderer.class)
//public abstract class MixinLevelRendererSky {
//    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
//    private void gtu$cancelVanillaSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera,
//                                      boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
//        if (SpaceClientRenderHooks.shouldSuppressVanillaSpaceVisuals()) {
//            ci.cancel();
//        }
//    }
//
//    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
//    private void gtu$cancelVanillaClouds(PoseStack poseStack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float partialTick,
//                                         double camX, double camY, double camZ, CallbackInfo ci) {
//        if (SpaceClientRenderHooks.shouldSuppressVanillaSpaceVisuals()) {
//            ci.cancel();
//        }
//    }
//}
