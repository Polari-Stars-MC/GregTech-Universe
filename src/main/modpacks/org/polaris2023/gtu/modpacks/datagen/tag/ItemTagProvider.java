package org.polaris2023.gtu.modpacks.datagen.tag;

import com.gregtechceu.gtceu.GTCEu;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.core.init.tag.ItemTags;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ItemTagProvider extends ItemTagsProvider {
    public ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags) {
        super(output, lookupProvider, blockTags);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        IntrinsicTagAppender<Item> grass_fiber = tag(ItemTags.GRASS_FIBER);
        Item flint_knife = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_knife"));
        grass_fiber.add(flint_knife);
    }
}
