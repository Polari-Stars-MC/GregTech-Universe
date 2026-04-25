package org.polaris2023.gtu.core.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.item.ClayBucketItem;
import org.polaris2023.gtu.core.item.WaterClayBucketItem;


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
    public static final DeferredItem<Item> GRAVELY_IRON =
            REGISTER.registerItem("gravely_iron", Item::new);
    public static final DeferredItem<Item> WASHED_COPPER_CONCENTRATE =
            REGISTER.registerItem("washed_copper_concentrate", Item::new);
    public static final DeferredItem<Item> WASHED_TIN_CONCENTRATE =
            REGISTER.registerItem("washed_tin_concentrate", Item::new);
    public static final DeferredItem<Item> WASHED_IRON_CONCENTRATE =
            REGISTER.registerItem("washed_iron_concentrate", Item::new);
    public static final DeferredItem<Item> ROPE =
            REGISTER.registerItem("rope", Item::new);
    public static final DeferredItem<Item> UNFIRED_CLAY_BUCKET =
            REGISTER.registerSimpleItem("unfired_clay_bucket");
    public static final DeferredItem<Item> UNFIRED_CLAY_CAULDRON =
            REGISTER.registerSimpleItem("unfired_clay_cauldron");
    public static final DeferredItem<ClayBucketItem> CLAY_BUCKET =
            REGISTER.registerItem("clay_bucket", ClayBucketItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<WaterClayBucketItem> WATER_CLAY_BUCKET =
            REGISTER.registerItem("water_clay_bucket", WaterClayBucketItem::new, new Item.Properties().stacksTo(1));


    public static final DeferredItem<BlockItem> STONE_CRAFTING_TABLE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STONE_CRAFTING_TABLE);
    public static final DeferredItem<BlockItem> GRAVEL_COPPER_ORE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.GRAVEL_COPPER_ORE);
    public static final DeferredItem<BlockItem> GRAVEL_TIN_ORE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.GRAVEL_TIN_ORE);
    public static final DeferredItem<BlockItem> FLINT_CRAFTING_TABLE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.FLINT_CRAFTING_TABLE);
    public static final DeferredItem<BlockItem> CLAY_CAULDRON =
            REGISTER.registerSimpleBlockItem(BlockRegistries.CLAY_CAULDRON);
    public static final DeferredItem<BlockItem> TEST_MULTIBLOCK_CONTROLLER =
            REGISTER.registerSimpleBlockItem(BlockRegistries.TEST_MULTIBLOCK_CONTROLLER);



    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
