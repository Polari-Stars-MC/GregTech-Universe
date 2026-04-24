package org.polaris2023.gtu.space.simulation.math;

public record SpaceVector(double x, double y, double z) {
    public static SpaceVector zero() {
        return new SpaceVector(0.0, 0.0, 0.0);
    }

    public SpaceVector add(SpaceVector other) {
        return new SpaceVector(x + other.x, y + other.y, z + other.z);
    }

    public SpaceVector scale(double scalar) {
        return new SpaceVector(x * scalar, y * scalar, z * scalar);
    }

    public SpaceVector subtract(SpaceVector other) {
        return new SpaceVector(x - other.x, y - other.y, z - other.z);
    }

    public SpaceVector cross(SpaceVector other) {
        return new SpaceVector(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    public double dot(SpaceVector other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double lengthSquared() {
        return dot(this);
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public SpaceVector normalize() {
        double length = length();
        if (length < 1.0E-9) {
            return zero();
        }
        return scale(1.0 / length);
    }
}

