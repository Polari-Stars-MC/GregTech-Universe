package org.polaris2023.gtu.core.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.polaris2023.gtu.core.datagen.loot.BlockLoot;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LootProvider extends LootTableProvider {
    public LootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)
        ), registries);
    }
}
