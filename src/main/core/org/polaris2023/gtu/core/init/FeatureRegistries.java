package org.polaris2023.gtu.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.worldgen.feature.RealisticRiverFeature;

public class FeatureRegistries {
    public static final DeferredRegister<Feature<?>> REGISTER =
            DeferredRegister.create(Registries.FEATURE, GregtechUniverseCore.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> REALISTIC_RIVER =
            REGISTER.register("realistic_river", () -> new RealisticRiverFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
