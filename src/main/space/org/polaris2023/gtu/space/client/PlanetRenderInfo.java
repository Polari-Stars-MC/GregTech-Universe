package org.polaris2023.gtu.space.client;

import org.joml.Vector3d;

public record PlanetRenderInfo(
        String id,
        String displayName,
        Vector3d worldPosition,
        Vector3d rotationAxis,
        double radius,
        double distanceToCamera,
        double realDistanceToCamera,
        double viewerAltitude,
        boolean primary,
        boolean atmosphere,
        float alpha,
        double rotationPhaseRadians
) {
}
