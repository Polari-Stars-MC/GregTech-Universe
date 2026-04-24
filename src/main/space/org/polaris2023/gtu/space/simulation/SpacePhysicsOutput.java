package org.polaris2023.gtu.space.simulation;

import org.polaris2023.gtu.space.simulation.math.SpaceCoordinate;

public record SpacePhysicsOutput(
        SpaceDomain domain,
        String bodyId,
        String sdSlotDimension,
        SpaceCoordinate localCoordinate,
        double shellX,
        double shellY,
        double shellZ
) {
}
