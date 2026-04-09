package org.polaris2023.gtu.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.menu.FlintCraftingMenu;

public class MenuRegistries {
    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(Registries.MENU, GregtechUniverseCore.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FlintCraftingMenu>> FLINT_CRAFTING_MENU =
            REGISTER.register(
                    "flint_crafting_menu",
                    () -> new MenuType<>(FlintCraftingMenu::new, FeatureFlags.DEFAULT_FLAGS)
            );

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
