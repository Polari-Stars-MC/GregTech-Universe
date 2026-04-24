package org.polaris2023.gtu.space.client.render;

import org.polaris2023.gtu.space.simulation.earth.EarthBounds;

public record EarthClientSeamState(
        boolean active,
        boolean approachingEastSeam,
        boolean approachingWestSeam,
        boolean cameraSwitched,
        double sourceX,
        double wrappedX,
        double sourceY,
        double sourceZ,
        double distanceToEastSeam,
        double distanceToWestSeam
) {
    public static final double PREVIEW_DISTANCE_BLOCKS = 128.0D;
    public static final double CAMERA_SWITCH_DISTANCE_BLOCKS = 24.0D;
    public static final double MIN_OUTWARD_SPEED = 0.015D;
    public static final double WRAP_WIDTH_BLOCKS = EarthBounds.WRAP_WIDTH_BLOCKS_DOUBLE;

    public static EarthClientSeamState inactive(double x, double y, double z) {
        return new EarthClientSeamState(
                false,
                false,
                false,
                false,
                x,
                x,
                y,
                z,
                EarthBounds.MAX_BLOCK_X_DOUBLE - x,
                x - EarthBounds.MIN_BLOCK_X_DOUBLE
        );
    }

    public static EarthClientSeamState from(double x, double y, double z, double deltaX) {
        double eastDistance = EarthBounds.MAX_BLOCK_X_DOUBLE - x;
        double westDistance = x - EarthBounds.MIN_BLOCK_X_DOUBLE;
        boolean towardEast = deltaX > MIN_OUTWARD_SPEED;
        boolean towardWest = deltaX < -MIN_OUTWARD_SPEED;
        boolean nearEast = eastDistance <= PREVIEW_DISTANCE_BLOCKS;
        boolean nearWest = westDistance <= PREVIEW_DISTANCE_BLOCKS;
        boolean approachingEast = towardEast && nearEast;
        boolean approachingWest = towardWest && nearWest;
        boolean active = approachingEast || approachingWest;
        boolean cameraSwitched = (approachingEast && eastDistance <= CAMERA_SWITCH_DISTANCE_BLOCKS)
                || (approachingWest && westDistance <= CAMERA_SWITCH_DISTANCE_BLOCKS);
        return new EarthClientSeamState(
                active,
                approachingEast,
                approachingWest,
                cameraSwitched,
                x,
                EarthBounds.wrapBlockX(x),
                y,
                z,
                eastDistance,
                westDistance
        );
    }
}
