package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StructureCheckOrder {
    private static final Comparator<CheckNode> NODE_ORDER = Comparator
            .comparing(CheckNode::isCritical).reversed()
            .thenComparing(CheckNode::isSameChunkPreferred).reversed()
            .thenComparingInt(CheckNode::horizontalDistanceSq)
            .thenComparing(Comparator.comparingInt(CheckNode::priority).reversed())
            .thenComparingLong(CheckNode::relativePos);

    private StructureCheckOrder() {
    }

    public static List<LayerPlan> buildOrderedLayers(Collection<CheckNode> nodes) {
        Map<Integer, List<CheckNode>> grouped = new LinkedHashMap<>();
        for (CheckNode node : nodes) {
            int yOffset = BlockPos.of(node.relativePos()).getY();
            grouped.computeIfAbsent(yOffset, ignored -> new ArrayList<>()).add(node);
        }

        List<Integer> yOffsets = new ArrayList<>(grouped.keySet());
        yOffsets.sort(Integer::compareTo);

        List<LayerPlan> layers = new ArrayList<>(yOffsets.size());
        for (Integer yOffset : yOffsets) {
            List<CheckNode> orderedNodes = new ArrayList<>(grouped.get(yOffset));
            orderedNodes.sort(NODE_ORDER);

            List<CheckNode> criticalNodes = orderedNodes.stream()
                    .filter(CheckNode::isCritical)
                    .toList();

            layers.add(new LayerPlan(yOffset, criticalNodes, orderedNodes, orderedNodes.size()));
        }
        return List.copyOf(layers);
    }
}
