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
                        output.accept(ItemRegistries.PLANT_FIBER.get());
                        output.accept(ItemRegistries.FLINT_SHARD.get());
                        output.accept(ItemRegistries.GRAVELY_COPPER.get());
                        output.accept(ItemRegistries.GRAVELY_TIN.get());
                        output.accept(ItemRegistries.STONE_CRAFTING_TABLE.get());
                        output.accept(ItemRegistries.GRAVEL_COPPER_ORE.get());
                        output.accept(ItemRegistries.GRAVEL_TIN_ORE.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
