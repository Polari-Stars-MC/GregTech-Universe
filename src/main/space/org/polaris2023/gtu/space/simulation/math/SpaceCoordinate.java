package org.polaris2023.gtu.space.simulation.math;

public record SpaceCoordinate(long sectorX, long sectorY, long sectorZ, SpaceVector local) {
    public static final double SECTOR_SIZE = 1_000_000.0;

    public static SpaceCoordinate origin() {
        return new SpaceCoordinate(0L, 0L, 0L, SpaceVector.zero());
    }

    public SpaceCoordinate add(SpaceVector delta) {
        double x = local.x() + delta.x();
        double y = local.y() + delta.y();
        double z = local.z() + delta.z();

        long moveX = floorDiv(x, SECTOR_SIZE);
        long moveY = floorDiv(y, SECTOR_SIZE);
        long moveZ = floorDiv(z, SECTOR_SIZE);

        return new SpaceCoordinate(
                sectorX + moveX,
                sectorY + moveY,
                sectorZ + moveZ,
                new SpaceVector(
                        x - moveX * SECTOR_SIZE,
                        y - moveY * SECTOR_SIZE,
                        z - moveZ * SECTOR_SIZE
                )
        );
    }

    private static long floorDiv(double value, double divisor) {
        return (long) Math.floor(value / divisor);
    }
}

