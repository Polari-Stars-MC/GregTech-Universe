package org.polaris2023.gtu.core.datagen.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

import java.util.Set;

public class BlockLoot extends BlockLootSubProvider {



    public BlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return BlockRegistries.REGISTER.getEntries()
                .stream().map(h -> (Block) h.value()).toList();
    }

    @Override
    protected void generate() {
        dropSelf(BlockRegistries.FLINT_CRAFTING_TABLE.get());
        dropSelf(BlockRegistries.STONE_CRAFTING_TABLE.get());
        dropOther(BlockRegistries.GRAVEL_COPPER_ORE.get(), ItemRegistries.GRAVELY_COPPER);
        dropOther(BlockRegistries.GRAVEL_TIN_ORE.get(), ItemRegistries.GRAVELY_TIN);
    }
}
