package org.polaris2023.gtu.core.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.BlockRegistries;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends BlockTagsProvider {
    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, GregtechUniverseCore.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        IntrinsicTagAppender<Block> axeMine = tag(BlockTags.MINEABLE_WITH_AXE);
        axeMine.add(BlockRegistries.FLINT_CRAFTING_TABLE.get());
        axeMine.add(BlockRegistries.STONE_CRAFTING_TABLE.get());
        IntrinsicTagAppender<Block> pickaxeMine = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        pickaxeMine.add(BlockRegistries.CLAY_CAULDRON.get());
        pickaxeMine.add(BlockRegistries.WATER_CLAY_CAULDRON.get());
        IntrinsicTagAppender<Block> shovelMine = tag(BlockTags.MINEABLE_WITH_SHOVEL);
        shovelMine.add(BlockRegistries.GRAVEL_COPPER_ORE.get());
        shovelMine.add(BlockRegistries.GRAVEL_TIN_ORE.get());
        shovelMine.add(BlockRegistries.GRAVEL_IRON_ORE.get());

    }
}
