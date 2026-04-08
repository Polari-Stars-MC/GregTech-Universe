package org.polaris2023.gtu.core.datagen.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.polaris2023.gtu.core.GregtechUniverseCore;

public abstract class Lang extends LanguageProvider {
    public Lang(PackOutput output, String locale) {
        super(output, GregtechUniverseCore.MOD_ID, locale);
    }
}
