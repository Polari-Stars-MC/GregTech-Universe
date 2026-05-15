package org.polaris2023.gtu.modpacks.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CopperToolKeyMappings {
    public static final Lazy<KeyMapping> VEIN_MINE_KEY = Lazy.of(() ->
            new KeyMapping(
                    "key.gtu_modpacks.copper_vein_mine",
                    GLFW.GLFW_KEY_GRAVE_ACCENT,
                    "key.categories.gtu_modpacks"
            )
    );

    private CopperToolKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(VEIN_MINE_KEY.get());
    }
}
