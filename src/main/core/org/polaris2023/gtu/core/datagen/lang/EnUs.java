package org.polaris2023.gtu.core.datagen.lang;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.StringUtils;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class EnUs extends Lang {
    public EnUs(PackOutput output) {
        super(output, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistries.REGISTER.getEntries()) {
            ResourceLocation id = entry.getId();
            Item item = entry.get();
            if (shouldUseExplicitTranslation(id)) {
                continue;
            }
            switch (item) {
                case BlockItem ignored -> {}
                default -> {
                    String[] s = id.getPath().split("_");
                    StringBuilder t = new StringBuilder();
                    for (String string : s) {
                        t.append(StringUtils.capitalize(string));
                    }
                    addItem(entry, t.toString());
                }
            }
        }

        for (DeferredHolder<Block, ? extends Block> entry : BlockRegistries.REGISTER.getEntries()) {
            ResourceLocation id = entry.getId();
            if (shouldUseExplicitTranslation(id)) {
                continue;
            }
            String[] s = id.getPath().split("_");
            StringBuilder t = new StringBuilder();
            for (String string : s) {
                t.append(StringUtils.capitalize(string));
            }
            addBlock(entry, t.toString());
        }
        addItem(ItemRegistries.UNFIRED_CLAY_BUCKET, "Unfired Clay Bucket");
        addItem(ItemRegistries.UNFIRED_CLAY_CAULDRON, "Unfired Clay Cauldron");
        addItem(ItemRegistries.CLAY_BUCKET, "Clay Bucket");
        addItem(ItemRegistries.WATER_CLAY_BUCKET, "Water Clay Bucket");
        addItem(ItemRegistries.WASHED_COPPER_CONCENTRATE, "Washed Copper Concentrate");
        addItem(ItemRegistries.WASHED_TIN_CONCENTRATE, "Washed Tin Concentrate");
        addItem(ItemRegistries.WASHED_IRON_CONCENTRATE, "Washed Iron Concentrate");
        addBlock(BlockRegistries.CLAY_CAULDRON, "Clay Cauldron");
        addBlock(BlockRegistries.WATER_CLAY_CAULDRON, "Water Clay Cauldron");
        add("itemGroup.gtu_core.main", "GregTech Universe Core");
        add("gui.gtu_core.flint_crafting.progress", "Progress: %s / %s");
        add("quests.gtu.start", "Start");
    }

    private static boolean shouldUseExplicitTranslation(ResourceLocation id) {
        return switch (id.getPath()) {
            case "unfired_clay_bucket",
                 "unfired_clay_cauldron",
                 "clay_bucket",
                 "water_clay_bucket",
                 "washed_copper_concentrate",
                 "washed_tin_concentrate",
                 "washed_iron_concentrate",
                 "clay_cauldron",
                 "water_clay_cauldron" -> true;
            default -> false;
        };
    }
}
