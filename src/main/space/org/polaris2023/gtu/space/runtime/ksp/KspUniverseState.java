package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.UniverseVector;

import java.math.BigDecimal;

public record KspUniverseState(
        String universeId,
        KspGalaxyState galaxy,
        UniverseVector systemUniversePosition,
        UniverseVector systemUniverseDriftPerSecond,
        BigDecimal systemDistanceFromOrigin,
        BigDecimal scaleFactor,
        BigDecimal effectiveExpansionRatePerSecond
) {
}
