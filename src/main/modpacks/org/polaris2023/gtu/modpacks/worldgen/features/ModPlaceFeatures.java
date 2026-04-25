package org.polaris2023.gtu.modpacks.worldgen.features;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

import java.util.List;

public class ModPlaceFeatures {
    public static final ResourceKey<PlacedFeature> GROUND_STICK_DISPLAY =
            ResourceKey.create(Registries.PLACED_FEATURE, GregtechUniverseModPacks.id("ground_stick_display"));

    public static void register(BootstrapContext<PlacedFeature> placedFeatureBootstrapContext) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = placedFeatureBootstrapContext.lookup(Registries.CONFIGURED_FEATURE);
        Holder.Reference<ConfiguredFeature<?, ?>> groundStickDisplay = configuredFeatures.getOrThrow(ModConfiguredFeatures.GROUND_STICK_DISPLAY);
        List<PlacementModifier> placementModifiers = List.of(
                CountPlacement.of(12),
                InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                BiomeFilter.biome()
        );
        placedFeatureBootstrapContext.register(GROUND_STICK_DISPLAY, new PlacedFeature(groundStickDisplay, placementModifiers));
    }
}
