package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class KspDebugOverlay {
    private KspDebugOverlay() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }

        if (event.getKey() != GLFW.GLFW_KEY_F6) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof KspDebugScreen) {
            minecraft.setScreen(null);
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        minecraft.setScreen(new KspDebugScreen());
    }
}
