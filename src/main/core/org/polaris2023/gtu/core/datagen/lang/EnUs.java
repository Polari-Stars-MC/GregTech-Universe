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
        addItem(ItemRegistries.FLINT_SHARE, "Flint Share");
        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "Stone Crafting Table");
    }
}
