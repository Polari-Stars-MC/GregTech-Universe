package org.polaris2023.gtu.space.simulation.earth;

public record EarthProjectionConfig(
        boolean wrapX,
        boolean clampZ,
        EarthOutOfBoundsPolicy outOfBoundsPolicy
) {
    public static EarthProjectionConfig defaultConfig() {
        return EarthProjectionPreset.WEB_MERCATOR_1_TO_1_VOID.config();
    }

    public static EarthProjectionConfig of(EarthProjectionPreset preset) {
        return preset.config();
    }
}
