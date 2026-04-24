package org.polaris2023.gtu.modpacks.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.menu.WaterDamMenu;

/**
 * 菜单类型注册 (modpacks 模块)。
 */
public final class MenuRegistries {
    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(Registries.MENU, GregtechUniverseModPacks.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<WaterDamMenu>> WATER_DAM_MENU =
            REGISTER.register("water_dam_menu",
                    () -> IMenuTypeExtension.create(WaterDamMenu::new));

    private MenuRegistries() {}

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
