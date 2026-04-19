package org.polaris2023.gtu.space.simulation;

import org.polaris2023.gtu.space.simulation.math.SpaceCoordinate;
import org.polaris2023.gtu.space.simulation.math.UniverseVector;

import java.util.UUID;

public record SpacePlayerState(
        Mode mode,
        String anchorDimension,
        String universeId,
        String galaxyId,
        String systemId,
        UniverseVector universeAnchorPosition,
        SpaceCoordinate coordinate,
        UUID vesselId
) {
    public enum Mode {
        PLANET,
        SPACE
    }
}

