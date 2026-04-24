package org.polaris2023.gtu.space.simulation.ksp;

import org.polaris2023.gtu.space.simulation.math.UniverseVector;

import java.util.Objects;

public record KspGalaxyDefinition(
        String galaxyId,
        String displayName,
        UniverseVector universePosition,
        UniverseVector universeDriftPerSecond
) {
    public KspGalaxyDefinition {
        Objects.requireNonNull(galaxyId, "galaxyId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(universePosition, "universePosition");
        Objects.requireNonNull(universeDriftPerSecond, "universeDriftPerSecond");
    }
}

