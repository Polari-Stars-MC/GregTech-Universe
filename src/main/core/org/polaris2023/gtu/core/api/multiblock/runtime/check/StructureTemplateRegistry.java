package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureNodeDefinition;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureTemplateSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StructureTemplateRegistry implements StructureTemplateSource {
    private final Map<ResourceLocation, Collection<StructureNodeDefinition>> definitions = new ConcurrentHashMap<>();

    public void register(ResourceLocation machineId, Collection<StructureNodeDefinition> nodes) {
        definitions.put(machineId, List.copyOf(nodes));
    }

    public boolean contains(ResourceLocation machineId) {
        return definitions.containsKey(machineId);
    }

    @Override
    public Collection<StructureNodeDefinition> load(ResourceLocation machineId) {
        Collection<StructureNodeDefinition> nodes = definitions.get(machineId);
        return nodes == null ? List.of() : nodes;
    }
}
