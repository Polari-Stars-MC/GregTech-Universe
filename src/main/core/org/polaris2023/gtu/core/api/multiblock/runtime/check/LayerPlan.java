package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import java.util.List;

public record LayerPlan(
        int yOffset,
        List<CheckNode> criticalNodes,
        List<CheckNode> orderedNodes,
        int requiredCount
) {
    public LayerPlan {
        criticalNodes = List.copyOf(criticalNodes);
        orderedNodes = List.copyOf(orderedNodes);
    }
}
