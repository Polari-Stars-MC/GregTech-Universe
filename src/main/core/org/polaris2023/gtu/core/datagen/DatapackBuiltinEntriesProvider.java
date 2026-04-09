package org.polaris2023.gtu.core.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.core.GregtechUniverseCore;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatapackBuiltinEntriesProvider extends net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider {
    public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries,
                new RegistrySetBuilder()
                , Set.of(GregtechUniverseCore.MOD_ID, "minecraft"));
    }
}
