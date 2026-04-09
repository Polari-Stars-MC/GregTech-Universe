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

    public static final DeferredItem<Item> FLINT_SHARD =
            REGISTER.registerItem("flint_shard", Item::new);
    public static final DeferredItem<Item> STONE_SHARD =
            REGISTER.registerItem("stone_shard", Item::new);
    public static final DeferredItem<Item> GRAVELY_COPPER =
            REGISTER.registerItem("gravely_copper", Item::new);
    public static final DeferredItem<Item> GRAVELY_TIN =
            REGISTER.registerItem("gravely_tin", Item::new);



    public static final DeferredItem<BlockItem> STONE_CRAFTING_TABLE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STONE_CRAFTING_TABLE);
    public static final DeferredItem<BlockItem> GRAVEL_COPPER_ORE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.GRAVEL_COPPER_ORE);
    public static final DeferredItem<BlockItem> GRAVEL_TIN_ORE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.GRAVEL_TIN_ORE);
    public static final DeferredItem<BlockItem> FLINT_CRAFTING_TABLE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.FLINT_CRAFTING_TABLE);

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
