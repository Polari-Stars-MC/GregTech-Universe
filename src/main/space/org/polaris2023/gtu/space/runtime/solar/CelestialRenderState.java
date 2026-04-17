package org.polaris2023.gtu.space.runtime.solar;

import org.joml.Vector3f;

public record CelestialRenderState(
        String id,
        CelestialRenderShape renderShape,
        Vector3f direction,
        float renderRadius,
        float renderSize,
        float red,
        float green,
        float blue,
        float alpha,
        float glowSize,
        boolean glowEnabled
) {
}
