package org.polaris2023.gtu.space.runtime;

import net.minecraft.world.level.Level;

import java.util.Map;

public record PlanetDomainDefinition(
        String bodyId,
        String displayName,
        String planetDimension,
        double takeoffPrewarmAltitudeMeters,
        double sdCutoverAltitudeMeters,
        double sdFadeCompleteAltitudeMeters,
        double landingPrewarmAltitudeMeters,
        double takeoffMetersPerTick,
        double landingMetersPerTick
) {
    private static final PlanetDomainDefinition EARTH = new PlanetDomainDefinition(
            "earth",
            "Earth",
            Level.OVERWORLD.location().toString(),
            12_000.0,
            48_000.0,
            140_000.0,
            128_000.0,
            1_000.0,
            1_000.0
    );
    private static final Map<String, PlanetDomainDefinition> DEFINITIONS = Map.of(
            EARTH.bodyId(), EARTH
    );

    public static PlanetDomainDefinition earth() {
        return EARTH;
    }

    public static boolean hasDefinition(String bodyId) {
        return bodyId != null && DEFINITIONS.containsKey(bodyId);
    }

    public static PlanetDomainDefinition forBody(String bodyId) {
        return DEFINITIONS.getOrDefault(bodyId, EARTH);
    }
}
