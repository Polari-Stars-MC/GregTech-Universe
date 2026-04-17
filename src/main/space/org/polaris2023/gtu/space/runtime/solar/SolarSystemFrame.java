package org.polaris2023.gtu.space.runtime.solar;

import java.util.List;

public record SolarSystemFrame(
        long absoluteTick,
        float dayTick,
        float dayFraction,
        List<CelestialRenderState> bodies
) {
}
