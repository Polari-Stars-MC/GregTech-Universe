package org.polaris2023.gtu.modpacks.init;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.vanilla.Tiers;

public class ItemRegistries {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(GregtechUniverseModPacks.MOD_ID);

    public static final DeferredItem<PickaxeItem> FLINT_ROPE_PICKAXE =
            REGISTER.registerItem("flint_rope_pickaxe",
                    p -> new PickaxeItem(Tiers.FLINT, p.attributes(PickaxeItem.createAttributes(Tiers.FLINT, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> FLINT_ROPE_AXE =
            REGISTER.registerItem("flint_rope_axe",
                    p -> new AxeItem(Tiers.FLINT, p.attributes(AxeItem.createAttributes(Tiers.FLINT, 6.0F, -3.2F))));
    public static final DeferredItem<HoeItem> FLINT_ROPE_HOE =
            REGISTER.registerItem("flint_rope_hoe",
                    p -> new HoeItem(Tiers.FLINT, p.attributes(HoeItem.createAttributes(Tiers.FLINT, -1.0F, -2.0F))));
    public static final DeferredItem<SwordItem> FLINT_ROPE_SWORD =
            REGISTER.registerItem("flint_rope_sword",
                    p -> new SwordItem(Tiers.FLINT, p.attributes(SwordItem.createAttributes(Tiers.FLINT, 3, -2.4F))));
    public static final DeferredItem<ShovelItem> FLINT_ROPE_SHOVEL =
            REGISTER.registerItem("flint_rope_shovel",
                    p -> new ShovelItem(Tiers.FLINT, p.attributes(ShovelItem.createAttributes(Tiers.FLINT, 1.5F, -3.0F))));

    public static final DeferredItem<BlockItem> DAM_SHAFT =
            REGISTER.registerSimpleBlockItem(BlockRegistries.DAM_SHAFT);

    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_PRIMITIVE =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_PRIMITIVE);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_ULV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_ULV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_LV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_LV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_MV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_MV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_HV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_HV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_EV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_EV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_IV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_IV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_LUV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_LUV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_ZPM =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_ZPM);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_UV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_UV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_UHV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_UHV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_UEV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_UEV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_UIV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_UIV);
    public static final DeferredItem<BlockItem> STRESS_OUTPUT_HATCH_UXV =
            REGISTER.registerSimpleBlockItem(BlockRegistries.STRESS_OUTPUT_HATCH_UXV);

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
