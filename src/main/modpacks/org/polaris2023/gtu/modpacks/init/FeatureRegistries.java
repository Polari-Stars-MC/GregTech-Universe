package org.polaris2023.gtu.modpacks.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.worldgen.feature.GroundStickDisplayFeature;

public class FeatureRegistries {
    public static final DeferredRegister<Feature<?>> REGISTER =
            DeferredRegister.create(Registries.FEATURE, GregtechUniverseModPacks.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GROUND_STICK_DISPLAY =
            REGISTER.register("ground_stick_display", () -> new GroundStickDisplayFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
