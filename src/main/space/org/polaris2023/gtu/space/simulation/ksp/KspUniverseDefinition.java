package org.polaris2023.gtu.space.simulation.ksp;

import java.math.BigDecimal;
import java.util.Objects;

public record KspUniverseDefinition(
        String universeId,
        BigDecimal initialScaleFactor,
        BigDecimal baseExpansionRatePerSecond,
        BigDecimal expansionReferenceDistance,
        BigDecimal minimumExpansionMultiplier
) {
    public KspUniverseDefinition {
        Objects.requireNonNull(universeId, "universeId");
        Objects.requireNonNull(initialScaleFactor, "initialScaleFactor");
        Objects.requireNonNull(baseExpansionRatePerSecond, "baseExpansionRatePerSecond");
        Objects.requireNonNull(expansionReferenceDistance, "expansionReferenceDistance");
        Objects.requireNonNull(minimumExpansionMultiplier, "minimumExpansionMultiplier");
    }

    public static KspUniverseDefinition localUniverse() {
        return new KspUniverseDefinition(
                "observable_universe",
                BigDecimal.ONE,
                new BigDecimal("0.000000000000000001"),
                new BigDecimal("1.000000000000000000000000E22"),
                BigDecimal.ONE
        );
    }
}

