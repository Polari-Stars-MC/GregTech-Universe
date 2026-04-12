package org.polaris2023.gtu.modpacks.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends BlockTagsProvider {
    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, GregtechUniverseModPacks.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        var white_list_break = tag(BlockTags.WHITE_LIST_BREAK);
        white_list_break.addTag(Tags.Blocks.GRAVELS);
        white_list_break.addTag(net.minecraft.tags.BlockTags.LEAVES);
        white_list_break.add(Blocks.FERN);
        white_list_break.add(Blocks.LARGE_FERN);
        white_list_break.add(Blocks.SHORT_GRASS);
        white_list_break.add(Blocks.TALL_GRASS);
        IntrinsicTagAppender<Block> tag = tag(BlockTags.INCORRECT_FOR_FLINT_TOOL);
        tag.addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
        tag.addTag(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL);
        tag.addTag(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL);
        tag.addTag(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL);
    }
}
