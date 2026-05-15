package org.polaris2023.gtu.modpacks.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.polaris2023.gtu.core.init.CreativeTabRegistries;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class CreativesEvents {
    @SubscribeEvent
    public static void event(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeTabRegistries.MAIN.getKey())) {
            ItemRegistries.REGISTER.getEntries().forEach(h -> {
                event.accept(h.get());
            });

        }
    }
}
