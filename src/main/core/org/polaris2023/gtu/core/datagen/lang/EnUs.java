package org.polaris2023.gtu.core.datagen.lang;

import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class EnUs extends Lang {
    public EnUs(PackOutput output) {
        super(output, "en_us");
    }

    @Override
    protected void addTranslations() {
        addItem(ItemRegistries.PLANT_FIBER, "Plant Fiber");

        addItem(ItemRegistries.FLINT_SHARD, "Flint Shard");
        addItem(ItemRegistries.GRAVELY_COPPER, "Gravely Copper");
        addItem(ItemRegistries.GRAVELY_TIN, "Gravely Tin");

        addItem(ItemRegistries.FLINT_SHARE, "Flint Share");

        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "Stone Crafting Table");
        addBlock(BlockRegistries.FLINT_CRAFTING_TABLE, "Flint Crafting Table");
        addBlock(BlockRegistries.GRAVEL_COPPER_ORE, "Gravel Copper Ore");
        addBlock(BlockRegistries.GRAVEL_TIN_ORE, "Gravel Tin Ore");
        add("itemGroup.gtu_core.main", "GregTech Universe Core");
        add("gui.gtu_core.flint_crafting.progress", "Progress: %s / %s");
    }
}
