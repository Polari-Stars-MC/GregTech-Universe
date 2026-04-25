package org.polaris2023.gtu.modpacks;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.modpacks.datagen.ItemModelProvider;
import org.polaris2023.gtu.modpacks.datagen.RecipesProvider;
import org.polaris2023.gtu.modpacks.datagen.lang.EnUs;
import org.polaris2023.gtu.modpacks.datagen.lang.ZhCnFixed;
import org.polaris2023.gtu.modpacks.datagen.tag.BlockTagProvider;
import org.polaris2023.gtu.modpacks.datagen.tag.ItemTagProvider;



import org.polaris2023.gtu.modpacks.worldgen.*;
import org.polaris2023.gtu.modpacks.worldgen.biomes.*;
import org.polaris2023.gtu.modpacks.worldgen.features.*;
import org.polaris2023.gtu.modpacks.worldgen.functions.*;
import org.polaris2023.gtu.modpacks.worldgen.noise.*;
import org.polaris2023.gtu.modpacks.worldgen.structures.*;


import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class GregtechUniverseModpacksDatagen {
    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        event.createProvider(RecipesProvider::new);
        final AtomicReference<BlockTagProvider> blockTagProvider = new AtomicReference<>();
        event.createProvider((output, future) -> {
            blockTagProvider.set(new BlockTagProvider(output, future, event.getExistingFileHelper()));
            return blockTagProvider.get();
        });
        event.createProvider((packOutput, completableFuture) -> new ItemTagProvider(packOutput, completableFuture, blockTagProvider.get().contentsGetter()));
        event.createProvider(ZhCnFixed::new);
        event.createProvider(EnUs::new);
        event.createProvider(output -> new ItemModelProvider(output, event.getExistingFileHelper()));
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.BIOME, ModBiomes::register)
                .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::register)
                .add(Registries.CONFIGURED_CARVER, ModConfiguredCavers::register)
                .add(Registries.DENSITY_FUNCTION, ModDensityFunctions::register)
                .add(Registries.DIMENSION_TYPE, ModDimensionTypes::register)
                .add(Registries.FLAT_LEVEL_GENERATOR_PRESET, ModFlatLevelGeneratorPresets::register)
                .add(Registries.NOISE_SETTINGS, ModNoiseGeneratedSettings::register)
                .add(Registries.NOISE, ModNoiseParameters::register)
                .add(Registries.PLACED_FEATURE, ModPlaceFeatures::register)
                .add(Registries.PROCESSOR_LIST, ModProcessorLists::register)
                .add(Registries.STRUCTURE_SET, ModStructureSets::register)
                .add(Registries.TEMPLATE_POOL, ModTemplatePools::register)
                .add(Registries.WORLD_PRESET, ModWorldPresets::register)
                .add(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MOdMultiNoiseBiomeSourceParameterLists::register)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::register)
                , Set.of("minecraft", GregtechUniverseModPacks.MOD_ID));

    }
}
