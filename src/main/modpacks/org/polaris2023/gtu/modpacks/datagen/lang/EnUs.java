package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.StringUtils;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

public class EnUs extends Lang {
    public EnUs(PackOutput output) {
        super(output, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistries.REGISTER.getEntries()) {
            ResourceLocation id = entry.getId();
            Item item = entry.get();
            switch (item) {
                case BlockItem ignored -> {}
                default -> {
                    String[] s = id.getPath().split("_");
                    StringBuilder t = new StringBuilder();
                    for (String string : s) {
                        t.append(StringUtils.capitalize(string));
                    }
                    addItem(entry, t.toString());
                }
            }
        }
    }
}
