package org.polaris2023.gtu.core.datagen.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockLoot extends BlockLootSubProvider {

    private static final Set<Item> EXPLOSION_RESISTANT = new HashSet<>();

    public BlockLoot(HolderLookup.Provider registries) {
        super(EXPLOSION_RESISTANT, FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        dropSelf(BlockRegistries.FLINT_CRAFTING_TABLE.get());
        dropSelf(BlockRegistries.STONE_CRAFTING_TABLE.get());
        dropOther(BlockRegistries.GRAVEL_COPPER_ORE.get(), ItemRegistries.GRAVELY_COPPER);
        dropOther(BlockRegistries.GRAVEL_TIN_ORE.get(), ItemRegistries.GRAVELY_TIN);
    }
}
