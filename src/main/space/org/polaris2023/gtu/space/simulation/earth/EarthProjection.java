package org.polaris2023.gtu.space.simulation.earth;

public final class EarthProjection {
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double RAD_TO_DEG = 180.0 / Math.PI;
    private static final double WEB_MERCATOR_LAT_LIMIT = 85.0511287798066;

    private EarthProjection() {
    }

    public static ProjectedEarthPoint wrapToEarth(int blockX, int blockZ) {
        int wrappedX = EarthBounds.wrapBlockX(blockX);
        boolean inside = EarthBounds.isWithinProjectedEarth(wrappedX, blockZ);
        int clampedZ = EarthBounds.clampBlockZ(blockZ);
        return new ProjectedEarthPoint(
                wrappedX,
                clampedZ,
                inside,
                xToLongitudeDegrees(wrappedX),
                zToLatitudeDegrees(clampedZ)
        );
    }

    public static double xToLongitudeDegrees(double blockX) {
        double wrappedX = EarthBounds.wrapBlockX((int) Math.floor(blockX));
        return (wrappedX / EarthBounds.HALF_EQUATORIAL_CIRCUMFERENCE_METERS) * 180.0;
    }

    public static double zToLatitudeDegrees(double blockZ) {
        double clamped = Math.max(-EarthBounds.WEB_MERCATOR_MAX_METERS, Math.min(EarthBounds.WEB_MERCATOR_MAX_METERS, blockZ));
        double normalized = clamped / EarthBounds.WEB_MERCATOR_MAX_METERS;
        return RAD_TO_DEG * Math.atan(Math.sinh(normalized * Math.PI));
    }

    public static int longitudeDegreesToBlockX(double longitudeDegrees) {
        double wrappedLongitude = wrapLongitudeDegrees(longitudeDegrees);
        double projectedX = wrappedLongitude / 180.0 * EarthBounds.HALF_EQUATORIAL_CIRCUMFERENCE_METERS;
        return EarthBounds.wrapBlockX((int) Math.round(projectedX));
    }

    public static int latitudeDegreesToBlockZ(double latitudeDegrees) {
        double clampedLatitude = Math.max(-WEB_MERCATOR_LAT_LIMIT, Math.min(WEB_MERCATOR_LAT_LIMIT, latitudeDegrees));
        double latitudeRadians = clampedLatitude * DEG_TO_RAD;
        double projectedZ = EarthBounds.WEB_MERCATOR_MAX_METERS * Math.log(Math.tan(Math.PI * 0.25 + latitudeRadians * 0.5)) / Math.PI;
        return EarthBounds.clampBlockZ((int) Math.round(projectedZ));
    }

    public static double wrapLongitudeDegrees(double longitudeDegrees) {
        double wrapped = (longitudeDegrees + 180.0) % 360.0;
        if (wrapped < 0.0) {
            wrapped += 360.0;
        }
        return wrapped - 180.0;
    }

    public record ProjectedEarthPoint(
            int wrappedBlockX,
            int clampedBlockZ,
            boolean insideProjectedEarth,
            double longitudeDegrees,
            double latitudeDegrees
    ) {
    }
}
