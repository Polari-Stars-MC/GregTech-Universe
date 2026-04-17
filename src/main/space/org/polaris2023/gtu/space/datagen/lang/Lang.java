package org.polaris2023.gtu.space.datagen.lang;

import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.*;

public abstract class Lang implements DataProvider {
    private final Map<String, ILang> languages = new HashMap<>();

    private ILang target;

    final PackOutput output;
    final String modid;

    public Lang(PackOutput output, String modid) {
        this.output = output;
        this.modid = modid;
    }

    public Lang locate(String lang) {
        if (!languages.containsKey(lang)) {
            languages.put(lang, new ILang(output, modid, lang));
        }
        target = languages.get(lang);
        return this;
    }

    public Lang addItem(Supplier<Item> item, String translate) {
        target.languages.add(l -> l.addItem(item, translate));
        return this;
    }

    public Lang addBlock(Supplier<Block> item, String translate) {
        target.languages.add(l -> l.addBlock(item, translate));
        return this;
    }


    public static class ILang extends LanguageProvider {
        public final List<Consumer<LanguageProvider>> languages = new ArrayList<>();

        public ILang(PackOutput output, String modid, String locale) {
            super(output, modid, locale);
        }

        @Override
        protected void addTranslations() {
            for (var language : languages) {
                language.accept(this);
            }
        }
    }

}
