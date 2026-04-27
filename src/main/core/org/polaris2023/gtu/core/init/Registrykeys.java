package org.polaris2023.gtu.core.init;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureTemplateDefinition;

public final class Registrykeys {
    public static final ResourceKey<Registry<StructureTemplateDefinition>> DYNAMIC_STRUCTURE_TEMPLATE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(GregtechUniverseCore.id("structure_template_dynamic"));

    private Registrykeys() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(Registrykeys::registerDatapackRegistries);
    }

    private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(DYNAMIC_STRUCTURE_TEMPLATE_REGISTRY_KEY,
                StructureTemplateDefinition.CODEC,
                StructureTemplateDefinition.CODEC);
    }
}
