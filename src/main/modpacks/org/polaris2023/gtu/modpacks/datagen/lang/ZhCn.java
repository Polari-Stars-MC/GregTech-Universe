package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;

public class ZhCn extends Lang {
    public ZhCn(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("item.gtu_modpacks.flint_rope_axe", "燧石绳绑斧");
        add("item.gtu_modpacks.flint_rope_pickaxe", "燧石绳绑镐");
        add("item.gtu_modpacks.flint_rope_hoe", "燧石绳绑锄");
        add("item.gtu_modpacks.flint_rope_sword", "燧石绳绑剑");
        add("item.gtu_modpacks.flint_rope_shovel", "燧石绳绑锹");
    }
}
