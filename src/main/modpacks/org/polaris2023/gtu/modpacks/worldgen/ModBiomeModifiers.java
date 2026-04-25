package org.polaris2023.gtu.modpacks.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.worldgen.features.ModPlaceFeatures;

public class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_GROUND_STICK_DISPLAY =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GregtechUniverseModPacks.id("add_ground_stick_display"));

    public static void register(BootstrapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(ADD_GROUND_STICK_DISPLAY, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlaceFeatures.GROUND_STICK_DISPLAY)),
                GenerationStep.Decoration.VEGETAL_DECORATION
        ));
    }
}
