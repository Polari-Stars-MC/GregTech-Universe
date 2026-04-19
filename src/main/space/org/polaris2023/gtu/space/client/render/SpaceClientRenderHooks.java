package org.polaris2023.gtu.space.client.render;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class SpaceClientRenderHooks {
    private SpaceClientRenderHooks() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!shouldSuppressVanillaSpaceVisuals()) {
            return;
        }
        event.setRed(0.0F);
        event.setGreen(0.0F);
        event.setBlue(0.0F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!shouldSuppressVanillaSpaceVisuals()) {
            return;
        }
        event.setNearPlaneDistance(event.getFarPlaneDistance());
        event.setCanceled(true);
    }

    public static boolean shouldSuppressVanillaSpaceVisuals() {
        Minecraft minecraft = Minecraft.getInstance();
        return false;
    }
}
