package org.polaris2023.gtu.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;

public class CreativeTabRegistries {
    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GregtechUniverseCore.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gtu_core.main"))
                    .icon(() -> new ItemStack(BlockRegistries.GRAVEL_COPPER_ORE.get()))
                    .displayItems((parameters, output) -> {
                        ItemRegistries.REGISTER.getEntries().forEach(h -> {
                            output.accept(h.get());
                        });
                    })
                    .build());

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
