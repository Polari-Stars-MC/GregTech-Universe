package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;

public class EnUs extends Lang {
    public EnUs(PackOutput output) {
        super(output, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("item.gtu_modpacks.flint_rope_axe", "Flint Rope Axe");
        add("item.gtu_modpacks.flint_rope_pickaxe", "Flint Rope Pickaxe");
        add("item.gtu_modpacks.flint_rope_hoe", "Flint Rope Hoe");
        add("item.gtu_modpacks.flint_rope_sword", "Flint Rope Sword");
        add("item.gtu_modpacks.flint_rope_shovel", "Flint Rope Shovel");
    }
}
