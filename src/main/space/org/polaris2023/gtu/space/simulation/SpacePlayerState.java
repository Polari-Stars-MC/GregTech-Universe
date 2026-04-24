package org.polaris2023.gtu.space.simulation;

import org.polaris2023.gtu.space.simulation.math.SpaceCoordinate;
import org.polaris2023.gtu.space.simulation.math.UniverseVector;

import java.util.UUID;

public record SpacePlayerState(
        Mode mode,
        String bodyId,
        String planetDimension,
        String stablePlanetDimension,
        double stableX,
        double stableY,
        double stableZ,
        float stableYRot,
        float stableXRot,
        String sdSlotDimension,
        String universeId,
        String galaxyId,
        String systemId,
        UniverseVector universeAnchorPosition,
        SpaceCoordinate coordinate,
        UUID vesselId,
        UUID transitionId
) {
    public enum Mode {
        PLANET,
        TAKEOFF_TRANSITION,
        SPACE,
        LANDING_TRANSITION;

        public boolean isSpaceLike() {
            return this == SPACE || this == TAKEOFF_TRANSITION || this == LANDING_TRANSITION;
        }
    }

    public boolean isTransitioning() {
        return mode == Mode.TAKEOFF_TRANSITION || mode == Mode.LANDING_TRANSITION;
    }
}

