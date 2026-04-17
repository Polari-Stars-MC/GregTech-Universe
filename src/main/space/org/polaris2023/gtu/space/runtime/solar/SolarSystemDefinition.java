package org.polaris2023.gtu.space.runtime.solar;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.List;

public record SolarSystemDefinition(
        long dayLengthTicks,
        long sunriseTick,
        long noonTick,
        long sunsetTick,
        long midnightTick,
        float axialTilt,
        List<SolarSystemBody> bodies
) {
    public static SolarSystemDefinition earthLike() {
        return new SolarSystemDefinition(
                24000L,
                0L,
                6000L,
                12000L,
                18000L,
                0.38F,
                List.of(
                        new SolarSystemBody(
                                "sun",
                                CelestialTrack.SOLAR,
                                CelestialRenderShape.SPHERE,
                                directionVector(Direction.EAST),
                                1600.0F,
                                2.50F,
                                1.00F,
                                0.95F,
                                0.72F,
                                1.0F,
                                74.0F,
                                false,
                                24000L,
                                0.0F
                        ),
                        new SolarSystemBody(
                                "moon",
                                CelestialTrack.LUNAR,
                                CelestialRenderShape.BILLBOARD,
                                directionVector(Direction.WEST),
                                1610.0F,
                                0.52F,
                                0.56F,
                                0.84F,
                                1.00F,
                                0.80F,
                                0.0F,
                                false,
                                24000L,
                                350.0F
                        )
                )
        );
    }

    private static Vector3f directionVector(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
