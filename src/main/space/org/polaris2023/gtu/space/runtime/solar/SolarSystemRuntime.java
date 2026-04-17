package org.polaris2023.gtu.space.runtime.solar;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class SolarSystemRuntime {
    private static final Vector3f EAST = directionVector(Direction.EAST);
    private static final Vector3f WEST = directionVector(Direction.WEST);
    private static final Vector3f UP = directionVector(Direction.UP);
    private static final Vector3f DOWN = directionVector(Direction.DOWN);
    private static final Vector3f NORTH = directionVector(Direction.NORTH);

    private final SolarSystemDefinition definition;

    public SolarSystemRuntime(SolarSystemDefinition definition) {
        this.definition = definition;
    }

    public SolarSystemDefinition definition() {
        return definition;
    }

    public SolarSystemFrame sample(long absoluteTick, float partialTick) {
        float dayTick = ((absoluteTick % definition.dayLengthTicks()) + partialTick);
        float dayFraction = dayTick / (float) definition.dayLengthTicks();

        List<CelestialRenderState> bodies = new ArrayList<>(definition.bodies().size());
        for (SolarSystemBody body : definition.bodies()) {
            Vector3f direction = switch (body.track()) {
                case SOLAR -> computeSolarDirection(dayTick + body.phaseOffsetTicks());
                case LUNAR -> computeSolarDirection(
                        (dayTick + definition.dayLengthTicks() / 2.0F + body.phaseOffsetTicks()) % definition.dayLengthTicks()
                );
                case FIXED -> applySkyRotation(body.baseDirection(), dayFraction, body.phaseOffsetTicks(), body.orbitalPeriodTicks());
            };
            float renderSize = computeRenderSize(body.renderRadius(), body.apparentDiameterDegrees());
            bodies.add(new CelestialRenderState(
                    body.id(),
                    body.renderShape(),
                    direction,
                    body.renderRadius(),
                    renderSize,
                    body.red(),
                    body.green(),
                    body.blue(),
                    body.alpha(),
                    body.glowSize(),
                    body.glowEnabled()
            ));
        }

        return new SolarSystemFrame(absoluteTick, dayTick, dayFraction, List.copyOf(bodies));
    }

    private float computeRenderSize(float renderRadius, float apparentDiameterDegrees) {
        float halfAngleRadians = (float) Math.toRadians(apparentDiameterDegrees * 0.5F);
        return 2.0F * renderRadius * (float) Math.tan(halfAngleRadians);
    }

    private Vector3f computeSolarDirection(float tickOnDay) {
        float wrappedTick = tickOnDay % definition.dayLengthTicks();
        if (wrappedTick < 0.0F) {
            wrappedTick += definition.dayLengthTicks();
        }

        Vector3f direction;
        if (wrappedTick < definition.noonTick()) {
            direction = interpolate(EAST, UP, wrappedTick / (float) definition.noonTick());
        } else if (wrappedTick < definition.sunsetTick()) {
            direction = interpolate(UP, WEST, (wrappedTick - definition.noonTick()) / (float) (definition.sunsetTick() - definition.noonTick()));
        } else if (wrappedTick < definition.midnightTick()) {
            direction = interpolate(WEST, DOWN, (wrappedTick - definition.sunsetTick()) / (float) (definition.midnightTick() - definition.sunsetTick()));
        } else {
            direction = interpolate(DOWN, EAST, (wrappedTick - definition.midnightTick()) / (float) (definition.dayLengthTicks() - definition.midnightTick()));
        }

        float heightFactor = Math.max(0.0F, direction.y());
        direction.add(new Vector3f(NORTH).mul(heightFactor * definition.axialTilt()));
        return direction.normalize();
    }

    private Vector3f applySkyRotation(Vector3f baseDirection, float dayFraction, float phaseOffsetTicks, long orbitalPeriodTicks) {
        float dailyAngle = dayFraction * Mth.TWO_PI;
        float orbitalAngle = orbitalPeriodTicks > 0L
                ? ((phaseOffsetTicks / orbitalPeriodTicks) + (dayFraction * definition.dayLengthTicks() / orbitalPeriodTicks)) * Mth.TWO_PI
                : 0.0F;

        Vector3f rotated = new Vector3f(baseDirection);
        rotated.rotateY(orbitalAngle);
        rotated.rotateZ(dailyAngle);
        rotated.add(new Vector3f(NORTH).mul(rotated.y * definition.axialTilt()));
        return rotated.normalize();
    }

    private static Vector3f interpolate(Vector3f from, Vector3f to, float progress) {
        return new Vector3f(from).lerp(to, Mth.clamp(progress, 0.0F, 1.0F)).normalize();
    }

    private static Vector3f directionVector(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
