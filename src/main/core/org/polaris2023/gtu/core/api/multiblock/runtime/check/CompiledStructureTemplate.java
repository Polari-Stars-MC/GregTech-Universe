package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CompiledStructureTemplate(
        ResourceLocation machineId,
        int minYOffset,
        int maxYOffset,
        int totalRequired,
        List<LayerPlan> layers,
        List<CheckNode> quickProbeNodes,
        Map<Long, CheckNode> nodesByRelativePos
) {
    public CompiledStructureTemplate {
        layers = List.copyOf(layers);
        quickProbeNodes = List.copyOf(quickProbeNodes);
        nodesByRelativePos = Map.copyOf(nodesByRelativePos);
    }

    public static CompiledStructureTemplate of(ResourceLocation machineId, List<CheckNode> nodes) {
        List<LayerPlan> layers = StructureCheckOrder.buildOrderedLayers(nodes);
        List<CheckNode> quickProbeNodes = StructureQuickProbe.buildQuickProbeNodes(layers);
        Map<Long, CheckNode> nodesByRelativePos = new HashMap<>();
        int minYOffset = 0;
        int maxYOffset = 0;
        boolean first = true;

        for (LayerPlan layer : layers) {
            if (first) {
                minYOffset = layer.yOffset();
                maxYOffset = layer.yOffset();
                first = false;
            } else {
                minYOffset = Math.min(minYOffset, layer.yOffset());
                maxYOffset = Math.max(maxYOffset, layer.yOffset());
            }
            for (CheckNode node : layer.orderedNodes()) {
                nodesByRelativePos.put(node.relativePos(), node);
            }
        }

        return new CompiledStructureTemplate(machineId, minYOffset, maxYOffset, nodes.size(), layers, quickProbeNodes, nodesByRelativePos);
    }
}
