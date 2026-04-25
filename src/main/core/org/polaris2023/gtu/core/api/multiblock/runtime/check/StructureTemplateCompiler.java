package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class StructureTemplateCompiler {
    private StructureTemplateCompiler() {
    }

    public static CompiledStructureTemplate compile(ResourceLocation machineId, Collection<StructureNodeDefinition> definitions) {
        List<CheckNode> nodes = new ArrayList<>(definitions.size());
        for (StructureNodeDefinition definition : definitions) {
            BlockPos relativePos = BlockPos.of(definition.relativePos());
            int horizontalDistanceSq = relativePos.getX() * relativePos.getX() + relativePos.getZ() * relativePos.getZ();
            nodes.add(new CheckNode(
                    definition.relativePos(),
                    definition.expectedStateId(),
                    definition.flags(),
                    horizontalDistanceSq,
                    definition.priority()
            ));
        }
        return CompiledStructureTemplate.of(machineId, nodes);
    }
}
