package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.polaris2023.gtu.core.init.Registrykeys;

import java.util.Collection;
import java.util.List;

public class StructureTemplateRegistry implements StructureTemplateSource {
    public boolean contains(ResourceLocation machineId) {
        return false;
    }

    @Override
    public Collection<StructureNodeDefinition> load(Level level, ResourceLocation machineId) {
        StructureTemplateDefinition definition = level.registryAccess()
                .registry(Registrykeys.DYNAMIC_STRUCTURE_TEMPLATE_REGISTRY_KEY)
                .map(registry -> registry.get(machineId))
                .orElse(null);
        return definition == null ? List.of() : definition.nodes();
    }
}
