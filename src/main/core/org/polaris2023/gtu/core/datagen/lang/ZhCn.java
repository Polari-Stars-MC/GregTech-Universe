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
        addItem(ItemRegistries.FLINT_SHARD, "燧石碎片");
        addItem(ItemRegistries.STONE_SHARD, "石片");
        addItem(ItemRegistries.GRAVELY_COPPER, "掺砾沙铜");
        addItem(ItemRegistries.GRAVELY_TIN, "掺砾沙锡");
        addItem(ItemRegistries.GRAVELY_IRON, "掺砾沙铁");
        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "石制工作台");
        addBlock(BlockRegistries.FLINT_CRAFTING_TABLE, "燧石工作台");
        addBlock(BlockRegistries.GRAVEL_COPPER_ORE, "沙砾铜矿");
        addBlock(BlockRegistries.GRAVEL_TIN_ORE, "沙砾锡矿");
        addBlock(BlockRegistries.GRAVEL_IRON_ORE, "沙砾铁矿");
        add("itemGroup.gtu_core.main", "GregTech Universe Core");
        add("gui.gtu_core.flint_crafting.progress", "进度：%s / %s");
    }
}
