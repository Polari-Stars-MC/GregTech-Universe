package org.polaris2023.gtu.core.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
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
                // SurfaceWaterDepthFilter.forMaxDepth(0), // 地表水最大深度
                InSquarePlacement.spread(), // 区块内均匀分布
                EnvironmentScanPlacement.scanningFor(
                        Direction.DOWN,//向下扫描
                        BlockPredicate.solid(),// 直到碰到固态方块
                        BlockPredicate.ONLY_IN_AIR_OR_WATER_PREDICATE, // 上方必须是空气或者是水
                        8 //最大扫描深度
                ),
                RarityFilter.onAverageOnceEvery(2), //平均每5区块生成一次，稀有度低
                BiomeFilter.biome() //生物群系过滤
        );
        context.register(PLACED_SURFACE_GRAVEL_ORE, new PlacedFeature(gravelOre, list));

    }
}
