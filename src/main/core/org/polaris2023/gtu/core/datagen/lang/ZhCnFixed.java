package org.polaris2023.gtu.core.datagen.lang;

import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class ZhCnFixed extends Lang {
    public ZhCnFixed(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        addItem(ItemRegistries.PLANT_FIBER, "植物纤维");
        addItem(ItemRegistries.FLINT_SHARD, "燧石碎片");
        addItem(ItemRegistries.STONE_SHARD, "石片");
        addItem(ItemRegistries.GRAVELY_COPPER, "参杂铜矿沙砾");
        addItem(ItemRegistries.GRAVELY_TIN, "参杂锡矿沙砾");
        addItem(ItemRegistries.GRAVELY_IRON, "参杂铁矿沙砾");
        addItem(ItemRegistries.WASHED_COPPER_CONCENTRATE, "水洗铜精矿");
        addItem(ItemRegistries.WASHED_TIN_CONCENTRATE, "水洗锡精矿");
        addItem(ItemRegistries.WASHED_IRON_CONCENTRATE, "水洗铁精矿");
        addItem(ItemRegistries.ROPE, "绳子");
        addItem(ItemRegistries.UNFIRED_CLAY_BUCKET, "未烧制粘土桶");
        addItem(ItemRegistries.UNFIRED_CLAY_CAULDRON, "未烧制粘土炼药锅");
        addItem(ItemRegistries.CLAY_BUCKET, "灰色粘土桶");
        addItem(ItemRegistries.WATER_CLAY_BUCKET, "装满水的粘土桶");

        addBlock(BlockRegistries.STONE_CRAFTING_TABLE, "石制工作台");
        addBlock(BlockRegistries.FLINT_CRAFTING_TABLE, "燧石工作台");
        addBlock(BlockRegistries.CLAY_CAULDRON, "粘土炼药锅");
        addBlock(BlockRegistries.WATER_CLAY_CAULDRON, "装水的粘土炼药锅");
        addBlock(BlockRegistries.GRAVEL_COPPER_ORE, "沙砾铜矿");
        addBlock(BlockRegistries.GRAVEL_TIN_ORE, "沙砾锡矿");
        addBlock(BlockRegistries.GRAVEL_IRON_ORE, "沙砾铁矿");

        add("itemGroup.gtu_core.main", "GregTech Universe Core");
        add("gui.gtu_core.flint_crafting.progress", "进度：%s / %s");
    }
}
