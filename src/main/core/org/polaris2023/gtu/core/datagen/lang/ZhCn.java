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
        addItem(ItemRegistries.PLANT_FIBER, "\u690d\u7269\u7ea4\u7ef4");
        addItem(ItemRegistries.FLINT_SHARD, "\u71e7\u77f3\u788e\u7247");
        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "\u77f3\u5236\u5408\u6210\u53f0");
        addBlock(BlockRegistries.FLINT_CRAFTING_TABLE, "\u71e7\u77f3\u5de5\u4f5c\u53f0");
        add("gui.gtu_core.flint_crafting.progress", "\u8fdb\u5ea6\uff1a%s / %s");
    }
}
