package org.polaris2023.gtu.space.simulation.ksp;

import org.polaris2023.gtu.space.simulation.math.SpaceVector;

public record KspBodyState(
        KspBodyDefinition definition,
        String referenceBodyId,
        SpaceVector absolutePosition,
        SpaceVector absoluteVelocity,
        boolean perturbed,
        SpaceVector thrustAcceleration,
        double rotationPhaseRadians
) {
}

