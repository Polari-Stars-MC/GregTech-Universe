package org.polaris2023.gtu.modpacks.init;

import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.api.item.datacomponents.GTTool;
import com.gregtechceu.gtceu.data.item.GTMaterialItems;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.vanilla.Tiers;

public class ItemRegistries {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(GregtechUniverseModPacks.MOD_ID);

    public static final DeferredItem<PickaxeItem> FLINT_ROPE_PICKAXE =
                REGISTER.registerItem("flint_rope_pickaxe", p -> new PickaxeItem(Tiers.FLINT, p.attributes(PickaxeItem.createAttributes(Tiers.FLINT, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> FLINT_ROPE_AXE =
                REGISTER.registerItem("flint_rope_axe", p -> new AxeItem(Tiers.FLINT, p.attributes(AxeItem.createAttributes(Tiers.FLINT, 6.0F, -3.2F))));
    public static final DeferredItem<HoeItem> FLINT_ROPE_HOE =
                REGISTER.registerItem("flint_rope_hoe", p -> new HoeItem(Tiers.FLINT, p.attributes(HoeItem.createAttributes(Tiers.FLINT, -1.0F, -2.0F))));
    public static final DeferredItem<SwordItem> FLINT_ROPE_SWORD =
                REGISTER.registerItem("flint_rope_sword", p -> new SwordItem(Tiers.FLINT, p.attributes(SwordItem.createAttributes(Tiers.FLINT, 3, -2.4F))));
    public static final DeferredItem<ShovelItem> FLINT_ROPE_SHOVEL =
                REGISTER.registerItem("flint_rope_shovel", p -> new ShovelItem(Tiers.FLINT, p.attributes(ShovelItem.createAttributes(Tiers.FLINT, 1.5F, -3.0F))));

    public static final DeferredItem<BlockItem> WATER_DAM_CONTROLLER =
            REGISTER.registerSimpleBlockItem(BlockRegistries.WATER_DAM_CONTROLLER);


    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }

}
