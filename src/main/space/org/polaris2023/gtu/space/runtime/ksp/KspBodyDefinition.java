package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.SpaceVector;

import java.util.List;

public record KspBodyDefinition(
        String id,
        String displayName,
        KspBodyKind bodyKind,
        KspReferenceFrameKind referenceFrameKind,
        String defaultReferenceBodyId,
        boolean referenceCaptureAllowed,
        SpaceVector initialPosition,
        SpaceVector initialVelocity,
        double gravitationalParameter,
        double radius,
        double sphereOfInfluence,
        double rotationPeriodSeconds,
        SpaceVector rotationAxis,
        double semiMajorAxis,
        double eccentricity,
        double inclinationRadians,
        double longitudeOfAscendingNodeRadians,
        double argumentOfPeriapsisRadians,
        double meanAnomalyAtEpochRadians,
        double orbitPeriodSeconds,
        double epochSeconds,
        List<KspBodyDefinition> children
) {
    public KspBodyDefinition {
        children = List.copyOf(children);
    }

    public double axialTiltRadians() {
        return Math.acos(Math.clamp(-rotationAxis.y(), -1.0, 1.0));
    }

    public double rocheLimit(KspBodyDefinition secondary) {
        if (radius <= 0.0 || secondary.radius() <= 0.0
                || gravitationalParameter <= 0.0 || secondary.gravitationalParameter() <= 0.0) {
            return 0.0;
        }
        double densityRatio = (gravitationalParameter * secondary.radius() * secondary.radius() * secondary.radius())
                / (secondary.gravitationalParameter() * radius * radius * radius);
        return 2.4554 * radius * Math.cbrt(densityRatio);
    }

    public OrbitalState stateAt(double elapsedSeconds) {
        return stateAt(elapsedSeconds, gravitationalParameter);
    }

    public OrbitalState stateAt(double elapsedSeconds, double centralGravitationalParameter) {
        if (referenceFrameKind == KspReferenceFrameKind.SYSTEM_CENTER) {
            return new OrbitalState(initialPosition, initialVelocity);
        }
        if (orbitPeriodSeconds <= 0.0 || semiMajorAxis <= 0.0) {
            return new OrbitalState(SpaceVector.zero(), SpaceVector.zero());
        }

        double meanMotion = Math.PI * 2.0 / orbitPeriodSeconds;
        double meanAnomaly = normalizeRadians(meanAnomalyAtEpochRadians + (elapsedSeconds - epochSeconds) * meanMotion);
        double eccentricAnomaly = solveEccentricAnomaly(meanAnomaly, eccentricity);

        double cosE = Math.cos(eccentricAnomaly);
        double sinE = Math.sin(eccentricAnomaly);
        double sqrtOneMinusESquared = Math.sqrt(Math.max(0.0, 1.0 - eccentricity * eccentricity));
        double radiusFactor = 1.0 - eccentricity * cosE;

        double xPerifocal = semiMajorAxis * (cosE - eccentricity);
        double yPerifocal = semiMajorAxis * sqrtOneMinusESquared * sinE;

        double speedFactor = Math.sqrt(centralGravitationalParameter / semiMajorAxis) / Math.max(radiusFactor, 1.0E-12);
        double vxPerifocal = -speedFactor * sinE;
        double vyPerifocal = speedFactor * sqrtOneMinusESquared * cosE;

        RotationMatrix rotation = rotationMatrix();
        return new OrbitalState(
                rotation.apply(xPerifocal, yPerifocal),
                rotation.apply(vxPerifocal, vyPerifocal)
        );
    }

    public SpaceVector positionAt(double elapsedSeconds) {
        return stateAt(elapsedSeconds).position();
    }

    public SpaceVector positionAt(double elapsedSeconds, double centralGravitationalParameter) {
        return stateAt(elapsedSeconds, centralGravitationalParameter).position();
    }

    private RotationMatrix rotationMatrix() {
        double cosOmega = Math.cos(longitudeOfAscendingNodeRadians);
        double sinOmega = Math.sin(longitudeOfAscendingNodeRadians);
        double cosI = Math.cos(inclinationRadians);
        double sinI = Math.sin(inclinationRadians);
        double cosW = Math.cos(argumentOfPeriapsisRadians);
        double sinW = Math.sin(argumentOfPeriapsisRadians);

        return new RotationMatrix(
                cosOmega * cosW - sinOmega * sinW * cosI,
                -cosOmega * sinW - sinOmega * cosW * cosI,
                sinOmega * cosW + cosOmega * sinW * cosI,
                -sinOmega * sinW + cosOmega * cosW * cosI,
                sinW * sinI,
                cosW * sinI
        );
    }

    private static double solveEccentricAnomaly(double meanAnomaly, double eccentricity) {
        double estimate = eccentricity < 0.8 ? meanAnomaly : Math.PI;
        for (int i = 0; i < 10; i++) {
            double f = estimate - eccentricity * Math.sin(estimate) - meanAnomaly;
            double fPrime = 1.0 - eccentricity * Math.cos(estimate);
            estimate -= f / fPrime;
        }
        return estimate;
    }

    private static double normalizeRadians(double angle) {
        double twoPi = Math.PI * 2.0;
        double normalized = angle % twoPi;
        return normalized < 0.0 ? normalized + twoPi : normalized;
    }

    public record OrbitalState(SpaceVector position, SpaceVector velocity) {
    }

    private record RotationMatrix(
            double m11,
            double m12,
            double m21,
            double m22,
            double m31,
            double m32
    ) {
        private SpaceVector apply(double xPerifocal, double yPerifocal) {
            double x = m11 * xPerifocal + m12 * yPerifocal;
            double y = m31 * xPerifocal + m32 * yPerifocal;
            double z = m21 * xPerifocal + m22 * yPerifocal;
            return new SpaceVector(x, y, z);
        }
    }
}
