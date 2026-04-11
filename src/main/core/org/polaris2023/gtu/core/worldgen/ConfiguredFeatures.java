package org.polaris2023.gtu.core.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.BlockRegistries;

import java.util.List;

public class ConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> SURFACE_GRAVEL_ORE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, GregtechUniverseCore.id("surface_gravel_ore"));
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // 精准匹配沙砾方块
        RuleTest gravelReplacement = new BlockMatchTest(Blocks.GRAVEL);

        // 定义替换规则列表
        List<OreConfiguration.TargetBlockState> targets = List.of(
                OreConfiguration.target(gravelReplacement, BlockRegistries.GRAVEL_COPPER_ORE.get().defaultBlockState()),
                OreConfiguration.target(gravelReplacement, BlockRegistries.GRAVEL_COPPER_ORE.get().defaultBlockState()),
                OreConfiguration.target(gravelReplacement, BlockRegistries.GRAVEL_TIN_ORE.get().defaultBlockState()),
                OreConfiguration.target(gravelReplacement, BlockRegistries.GRAVEL_TIN_ORE.get().defaultBlockState())
        );

        // 矿脉大小（原版沙砾矿石是33）
        context.register(SURFACE_GRAVEL_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets, 33)));
    }
}
