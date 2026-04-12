package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

public class ZhCnFixed extends Lang {
    public ZhCnFixed(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        addItem(ItemRegistries.FLINT_ROPE_AXE, "燧石绳斧");
        addItem(ItemRegistries.FLINT_ROPE_PICKAXE, "燧石绳镐");
        addItem(ItemRegistries.FLINT_ROPE_HOE, "燧石绳锄");
        addItem(ItemRegistries.FLINT_ROPE_SWORD, "燧石绳剑");
        addItem(ItemRegistries.FLINT_ROPE_SHOVEL, "燧石绳铲");
        addBlock(BlockRegistries.WATER_DAM_CONTROLLER, "水坝控制器");
    }
}
