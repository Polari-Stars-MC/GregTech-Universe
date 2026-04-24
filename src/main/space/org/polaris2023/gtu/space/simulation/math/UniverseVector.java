package org.polaris2023.gtu.space.simulation.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

public record UniverseVector(BigDecimal x, BigDecimal y, BigDecimal z) {
    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public UniverseVector {
        x = normalize(x);
        y = normalize(y);
        z = normalize(z);
    }

    public static UniverseVector zero() {
        return new UniverseVector(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static UniverseVector of(double x, double y, double z) {
        return new UniverseVector(BigDecimal.valueOf(x), BigDecimal.valueOf(y), BigDecimal.valueOf(z));
    }

    public UniverseVector add(UniverseVector other) {
        return new UniverseVector(
                x.add(other.x, MATH_CONTEXT),
                y.add(other.y, MATH_CONTEXT),
                z.add(other.z, MATH_CONTEXT)
        );
    }

    public UniverseVector subtract(UniverseVector other) {
        return new UniverseVector(
                x.subtract(other.x, MATH_CONTEXT),
                y.subtract(other.y, MATH_CONTEXT),
                z.subtract(other.z, MATH_CONTEXT)
        );
    }

    public UniverseVector scale(BigDecimal scalar) {
        BigDecimal normalized = normalize(scalar);
        return new UniverseVector(
                x.multiply(normalized, MATH_CONTEXT),
                y.multiply(normalized, MATH_CONTEXT),
                z.multiply(normalized, MATH_CONTEXT)
        );
    }

    public UniverseVector addScaled(UniverseVector velocityPerSecond, BigDecimal elapsedSeconds) {
        return add(velocityPerSecond.scale(elapsedSeconds));
    }

    public double lengthEstimate() {
        double dx = x.doubleValue();
        double dy = y.doubleValue();
        double dz = z.doubleValue();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return Objects.requireNonNull(value, "value").round(MATH_CONTEXT);
    }
}

