package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import java.util.Map;

public record StructureValidationResult(
        boolean quickProbePassed,
        boolean fullCheckRan,
        boolean formed,
        int matchedCount,
        int totalRequired,
        Map<Long, CheckNode> mismatches
) {
    public StructureValidationResult {
        mismatches = Map.copyOf(mismatches);
    }

    public float completion() {
        return totalRequired <= 0 ? 1.0F : (float) matchedCount / (float) totalRequired;
    }
}
