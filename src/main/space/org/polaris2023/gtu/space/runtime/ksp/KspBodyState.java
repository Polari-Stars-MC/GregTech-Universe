package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.SpaceVector;

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
