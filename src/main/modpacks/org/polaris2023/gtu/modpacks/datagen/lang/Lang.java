package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

public abstract class Lang extends LanguageProvider {
    public Lang(PackOutput output, String locale) {
        super(output, GregtechUniverseModPacks.MOD_ID, locale);
    }
}
