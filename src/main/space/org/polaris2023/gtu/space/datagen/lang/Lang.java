package org.polaris2023.gtu.space.datagen.lang;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

@SuppressWarnings("unused")
public class Lang implements DataProvider {
    private final Map<String, ILang> languages = new HashMap<>();

    private ILang target;

    final PackOutput output;
    final String modid;

    public Lang(PackOutput output, String modid) {
        this.output = output;
        this.modid = modid;
    }

    public Lang(PackOutput output) {
        this(output, GregtechUniverseSpace.MOD_ID);
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

    public Lang addItemStack(Supplier<ItemStack> stack, String translate) {
        target.languages.add(l -> l.addItemStack(stack, translate));
        return this;
    }

    public Lang addEffect(Supplier<? extends MobEffect> mobEffect, String translate) {
        target.languages.add(l -> l.addEffect(mobEffect, translate));
        return this;
    }

    public Lang addEntityType(Supplier<? extends EntityType<?>> entity, String translate) {
        target.languages.add(l -> l.addEntityType(entity, translate));
        return this;
    }

    public Lang addTag(Supplier<? extends TagKey<?>> tag, String translate) {
        target.languages.add(l -> l.addTag(tag, translate));
        return this;
    }

    public Lang addDimension(ResourceKey<Level> dimension, String translate) {
        target.languages.add(l -> l.addDimension(dimension, translate));
        return this;
    }

    public Lang add(String key, String translate) {
        target.languages.add(l -> l.add(key, translate));
        return this;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        return CompletableFuture.allOf(languages
                .values()
                .stream()
                .map(iLang -> iLang.run(cachedOutput))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public @NotNull String getName() {
        return "Language Provider by" + modid;
    }

    public static class ILang extends LanguageProvider {
        final List<Consumer<LanguageProvider>> languages = new ArrayList<>();

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
