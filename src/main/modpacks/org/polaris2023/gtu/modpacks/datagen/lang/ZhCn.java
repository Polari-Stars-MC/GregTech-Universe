package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

public class ZhCn extends Lang {
    public ZhCn(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        addItem(ItemRegistries.FLINT_ROPE_AXE, "燧石绳绑斧");
        addItem(ItemRegistries.FLINT_ROPE_PICKAXE, "燧石绳绑镐");
        addItem(ItemRegistries.FLINT_ROPE_HOE, "燧石绳绑锄");
        addItem(ItemRegistries.FLINT_ROPE_SWORD, "燧石绳绑剑");
        addItem(ItemRegistries.FLINT_ROPE_SHOVEL, "燧石绳绑锹");
    }
}
