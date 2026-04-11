package org.polaris2023.gtu.modpacks.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
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
