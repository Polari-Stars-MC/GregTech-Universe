package org.polaris2023.gtu.space.runtime;

import org.polaris2023.gtu.space.runtime.math.SpaceCoordinate;

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
