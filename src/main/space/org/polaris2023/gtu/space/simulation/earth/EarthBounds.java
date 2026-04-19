package org.polaris2023.gtu.space.simulation.earth;

public final class EarthBounds {
    public static final double EQUATORIAL_CIRCUMFERENCE_METERS = 40_075_016.68557849;
    public static final double HALF_EQUATORIAL_CIRCUMFERENCE_METERS = EQUATORIAL_CIRCUMFERENCE_METERS * 0.5;
    public static final double WEB_MERCATOR_MAX_METERS = 20_037_508.342789244;
    public static final double MIN_BLOCK_X_DOUBLE = -HALF_EQUATORIAL_CIRCUMFERENCE_METERS;
    public static final double MAX_BLOCK_X_DOUBLE = HALF_EQUATORIAL_CIRCUMFERENCE_METERS;
    public static final double MIN_BLOCK_Z_DOUBLE = -WEB_MERCATOR_MAX_METERS;
    public static final double MAX_BLOCK_Z_DOUBLE = WEB_MERCATOR_MAX_METERS;
    public static final double WRAP_WIDTH_BLOCKS_DOUBLE = EQUATORIAL_CIRCUMFERENCE_METERS;

    public static final int MIN_BLOCK_X = floorToBlock(-HALF_EQUATORIAL_CIRCUMFERENCE_METERS);
    public static final int MAX_BLOCK_X_EXCLUSIVE = ceilToBlock(HALF_EQUATORIAL_CIRCUMFERENCE_METERS);
    public static final int MIN_BLOCK_Z = floorToBlock(-WEB_MERCATOR_MAX_METERS);
    public static final int MAX_BLOCK_Z = ceilToBlock(WEB_MERCATOR_MAX_METERS);
    public static final int WRAP_WIDTH_BLOCKS = MAX_BLOCK_X_EXCLUSIVE - MIN_BLOCK_X;

    private EarthBounds() {
    }

    public static boolean isWithinProjectedEarth(int blockX, int blockZ) {
        return blockZ >= MIN_BLOCK_Z && blockZ <= MAX_BLOCK_Z;
    }

    public static boolean isOutsideProjectedEarth(int blockX, int blockZ) {
        return !isWithinProjectedEarth(blockX, blockZ);
    }

    public static boolean isWithinProjectedEarth(double blockX, double blockZ) {
        return blockZ >= MIN_BLOCK_Z_DOUBLE && blockZ <= MAX_BLOCK_Z_DOUBLE;
    }

    public static boolean isOutsideProjectedEarth(double blockX, double blockZ) {
        return !isWithinProjectedEarth(blockX, blockZ);
    }

    public static int wrapBlockX(int blockX) {
        return MIN_BLOCK_X + Math.floorMod(blockX - MIN_BLOCK_X, WRAP_WIDTH_BLOCKS);
    }

    public static double wrapBlockX(double blockX) {
        double wrapped = (blockX - MIN_BLOCK_X_DOUBLE) % WRAP_WIDTH_BLOCKS_DOUBLE;
        if (wrapped < 0.0D) {
            wrapped += WRAP_WIDTH_BLOCKS_DOUBLE;
        }
        return MIN_BLOCK_X_DOUBLE + wrapped;
    }

    public static int clampBlockZ(int blockZ) {
        return Math.max(MIN_BLOCK_Z, Math.min(MAX_BLOCK_Z, blockZ));
    }

    public static double clampBlockZ(double blockZ) {
        return Math.max(MIN_BLOCK_Z_DOUBLE, Math.min(MAX_BLOCK_Z_DOUBLE, blockZ));
    }

    private static int floorToBlock(double value) {
        return (int) Math.floor(value);
    }

    private static int ceilToBlock(double value) {
        return (int) Math.ceil(value);
    }
}
