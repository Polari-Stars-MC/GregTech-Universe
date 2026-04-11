package org.polaris2023.gtu.core.worldgen;

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
import org.polaris2023.gtu.core.GregtechUniverseCore;

public class BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_SURFACE_GRAVEL_ORE =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GregtechUniverseCore.id("add_surface_gravel_ore"));
    public static void bootstrap(BootstrapContext<BiomeModifier>  context) {
        HolderGetter<PlacedFeature> placedFeature = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biome = context.lookup(Registries.BIOME);
        context.register(ADD_SURFACE_GRAVEL_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                biome.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeature.getOrThrow(PlacedFeatures.PLACED_SURFACE_GRAVEL_ORE)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION
        ));
    }
}
