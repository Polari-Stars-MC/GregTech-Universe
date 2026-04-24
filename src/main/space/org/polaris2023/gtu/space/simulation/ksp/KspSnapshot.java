package org.polaris2023.gtu.space.simulation.ksp;

import org.polaris2023.gtu.space.simulation.math.SpaceVector;

import java.util.List;
import java.util.Map;

public record KspSnapshot(
        long simulationTick,
        double simulationSeconds,
        KspUniverseState universe,
        Map<String, KspBodyState> bodies,
        Map<String, List<SpaceVector>> bodyHistory,
        Map<String, KspVesselState> vessels,
        List<RocheViolation> rocheViolations
) {
}

