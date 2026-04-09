package org.polaris2023.gtu.core.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.client.screen.FlintCraftingScreen;
import org.polaris2023.gtu.core.init.MenuRegistries;

@EventBusSubscriber(modid = GregtechUniverseCore.MOD_ID)
public final class ClientRegistries {
    private ClientRegistries() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistries.FLINT_CRAFTING_MENU.get(), FlintCraftingScreen::new);
    }
}
