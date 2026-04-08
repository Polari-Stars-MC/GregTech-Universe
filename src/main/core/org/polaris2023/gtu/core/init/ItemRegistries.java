package org.polaris2023.gtu.core.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;


public class ItemRegistries {
    public static final DeferredRegister.Items REGISTER =
            DeferredRegister.createItems(GregtechUniverseCore.MOD_ID);

    public static final DeferredItem<Item> PLANT_FIBER =
            REGISTER.registerItem("plant_fiber", Item::new);

    public static final DeferredItem<BlockItem> STONE_CRAFTING_TABLE =
            ItemRegistries.REGISTER.registerSimpleBlockItem(BlockRegistries.STONE_CRAFTING_TABLE);

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
