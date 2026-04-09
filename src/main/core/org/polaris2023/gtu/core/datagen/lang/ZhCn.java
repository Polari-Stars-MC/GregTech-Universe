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
        addItem(ItemRegistries.GRAVELY_COPPER, "\u542b\u6c99\u783e\u94dc");
        addItem(ItemRegistries.GRAVELY_TIN, "\u542b\u6c99\u783e\u9521");
        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "\u77f3\u5236\u5408\u6210\u53f0");
        addBlock(BlockRegistries.FLINT_CRAFTING_TABLE, "\u71e7\u77f3\u5de5\u4f5c\u53f0");
        addBlock(BlockRegistries.GRAVEL_COPPER_ORE, "\u6c99\u783e\u94dc\u77ff");
        addBlock(BlockRegistries.GRAVEL_TIN_ORE, "\u6c99\u783e\u9521\u77ff");
        add("itemGroup.gtu_core.main", "GregTech Universe Core");
        add("gui.gtu_core.flint_crafting.progress", "\u8fdb\u5ea6\uff1a%s / %s");

        addItem(ItemRegistries.PLANT_FIBER, "植物纤维");
        addItem(ItemRegistries.FLINT_SHARE, "燧石碎片");
        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "石制合成台");

    }
}
