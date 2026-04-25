package org.polaris2023.gtu.modpacks.worldgen.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.FeatureRegistries;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> GROUND_STICK_DISPLAY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, GregtechUniverseModPacks.id("ground_stick_display"));

    public static void register(BootstrapContext<ConfiguredFeature<?, ?>> configuredFeatureBootstrapContext) {
        configuredFeatureBootstrapContext.register(
                GROUND_STICK_DISPLAY,
                new ConfiguredFeature<>(FeatureRegistries.GROUND_STICK_DISPLAY.get(), NoneFeatureConfiguration.NONE)
        );
    }
}
