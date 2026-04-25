package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import java.util.ArrayList;
import java.util.List;

public final class StructureQuickProbe {
    private StructureQuickProbe() {
    }

    public static List<CheckNode> buildQuickProbeNodes(List<LayerPlan> layers) {
        List<CheckNode> result = new ArrayList<>();
        for (LayerPlan layer : layers) {
            if (!layer.criticalNodes().isEmpty()) {
                result.addAll(layer.criticalNodes());
                continue;
            }
            int take = Math.min(4, layer.orderedNodes().size());
            result.addAll(layer.orderedNodes().subList(0, take));
        }
        return List.copyOf(result);
    }

    public static boolean shouldRunFullCheck(int matchedProbeCount, int totalProbeCount) {
        return totalProbeCount <= 0 || matchedProbeCount >= Math.max(1, totalProbeCount / 2);
    }
}
