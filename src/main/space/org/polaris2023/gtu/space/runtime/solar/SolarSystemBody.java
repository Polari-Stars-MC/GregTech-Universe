package org.polaris2023.gtu.space.runtime.solar;

import org.joml.Vector3f;

public record SolarSystemBody(
        String id,
        CelestialTrack track,
        CelestialRenderShape renderShape,
        Vector3f baseDirection,
        float renderRadius,
        float apparentDiameterDegrees,
        float red,
        float green,
        float blue,
        float alpha,
        float glowSize,
        boolean glowEnabled,
        long orbitalPeriodTicks,
        float phaseOffsetTicks
) {
}
