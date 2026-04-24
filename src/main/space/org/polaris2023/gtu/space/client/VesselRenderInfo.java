package org.polaris2023.gtu.space.client;

import org.joml.Vector3d;

import java.util.UUID;

public record VesselRenderInfo(
        UUID id,
        String authorityId,
        String bodyId,
        Vector3d position,
        float yawDegrees,
        double distanceToCamera,
        boolean playerControlled,
        boolean firstPerson
) {
}
