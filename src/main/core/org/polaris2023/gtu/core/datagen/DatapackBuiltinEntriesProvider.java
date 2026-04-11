package org.polaris2023.gtu.core.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.worldgen.BiomeModifiers;
import org.polaris2023.gtu.core.worldgen.ConfiguredFeatures;
import org.polaris2023.gtu.core.worldgen.PlacedFeatures;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatapackBuiltinEntriesProvider extends net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider {
    public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries,
                new RegistrySetBuilder()
                        .add(Registries.CONFIGURED_FEATURE, ConfiguredFeatures::bootstrap)
                        .add(Registries.PLACED_FEATURE, PlacedFeatures::bootstrap)
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifiers::bootstrap)
                , Set.of(GregtechUniverseCore.MOD_ID, "minecraft"));
    }
}
