package org.polaris2023.gtu.space.simulation;

import org.polaris2023.gtu.space.simulation.math.SpaceVector;

public record SpacePhysicsInput(
        SpaceVector thrustDirection,
        double thrustMagnitude,
        SpaceVector angularVelocity,
        boolean ascending,
        boolean descending,
        boolean landingContact
) {
    public static SpacePhysicsInput idle() {
        return new SpacePhysicsInput(SpaceVector.zero(), 0.0, SpaceVector.zero(), false, false, false);
    }
}
