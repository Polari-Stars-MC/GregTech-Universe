package org.polaris2023.gtu.core.datagen.lang;

import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class ZhCn extends Lang {
    public ZhCn(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        addItem(ItemRegistries.PLANT_FIBER, "植物纤维");
        addItem(ItemRegistries.FLINT_SHARE, "燧石碎片");
        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "石制合成台");
    }
}
