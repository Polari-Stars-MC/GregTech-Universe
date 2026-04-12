package org.polaris2023.gtu.core.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import org.polaris2023.gtu.core.GregtechUniverseCore;

import java.util.List;

public class PlacedFeatures {
    public static final ResourceKey<PlacedFeature> PLACED_SURFACE_GRAVEL_ORE =
            ResourceKey.create(Registries.PLACED_FEATURE, GregtechUniverseCore.id("placed_surface_gravel_ore"));

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
//        new PlacedFeature()
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder.Reference<ConfiguredFeature<?, ?>> gravelOre = configuredFeatures.getOrThrow(ConfiguredFeatures.SURFACE_GRAVEL_ORE);
        List<PlacementModifier> list = List.of(
                CountPlacement.of(100), //每个区块尝试n次生成（增加次数以便测试）
                InSquarePlacement.spread(), // 区块内均匀分布
                HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(-64), VerticalAnchor.aboveBottom(320)), // 高度范围
                BiomeFilter.biome() //生物群系过滤
        );
        context.register(PLACED_SURFACE_GRAVEL_ORE, new PlacedFeature(gravelOre, list));
    }
}
