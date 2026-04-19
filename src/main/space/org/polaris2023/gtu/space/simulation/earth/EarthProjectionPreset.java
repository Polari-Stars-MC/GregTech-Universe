package org.polaris2023.gtu.space.simulation.earth;

public enum EarthProjectionPreset {
    WEB_MERCATOR_1_TO_1_VOID(
            new EarthProjectionConfig(true, true, EarthOutOfBoundsPolicy.VOID_AIR)
    ),
    WEB_MERCATOR_1_TO_1_AIR(
            new EarthProjectionConfig(true, true, EarthOutOfBoundsPolicy.AIR)
    );

    private final EarthProjectionConfig config;

    EarthProjectionPreset(EarthProjectionConfig config) {
        this.config = config;
    }

    public EarthProjectionConfig config() {
        return config;
    }
}
