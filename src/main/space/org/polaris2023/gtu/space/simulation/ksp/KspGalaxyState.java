package org.polaris2023.gtu.space.simulation.ksp;

import org.polaris2023.gtu.space.simulation.math.UniverseVector;

public record KspGalaxyState(
        String galaxyId,
        String displayName,
        UniverseVector universePosition,
        UniverseVector universeDriftPerSecond
) {
}

