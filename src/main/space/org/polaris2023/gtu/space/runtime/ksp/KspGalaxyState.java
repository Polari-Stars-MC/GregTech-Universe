package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.UniverseVector;

public record KspGalaxyState(
        String galaxyId,
        String displayName,
        UniverseVector universePosition,
        UniverseVector universeDriftPerSecond
) {
}
