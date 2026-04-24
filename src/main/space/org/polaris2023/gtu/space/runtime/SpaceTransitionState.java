package org.polaris2023.gtu.space.runtime;

import java.util.UUID;

public final class SpaceTransitionState {
    private final UUID transitionId;
    private final SpaceTransitionDirection direction;
    private final SpaceDomain sourceDomain;
    private final SpaceDomain targetDomain;
    private final String bodyId;
    private final String sourceDimension;
    private final String targetDimension;
    private final long startTick;
    private final long cutoverTick;
    private final long completeTick;
    private final double startAltitudeMeters;
    private final double cutoverAltitudeMeters;
    private final double completeAltitudeMeters;
    private boolean cutoverApplied;

    public SpaceTransitionState(
            UUID transitionId,
            SpaceTransitionDirection direction,
            SpaceDomain sourceDomain,
            SpaceDomain targetDomain,
            String bodyId,
            String sourceDimension,
            String targetDimension,
            long startTick,
            long cutoverTick,
            long completeTick,
            double startAltitudeMeters,
            double cutoverAltitudeMeters,
            double completeAltitudeMeters,
            boolean cutoverApplied
    ) {
        this.transitionId = transitionId;
        this.direction = direction;
        this.sourceDomain = sourceDomain;
        this.targetDomain = targetDomain;
        this.bodyId = bodyId;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.startTick = startTick;
        this.cutoverTick = cutoverTick;
        this.completeTick = completeTick;
        this.startAltitudeMeters = startAltitudeMeters;
        this.cutoverAltitudeMeters = cutoverAltitudeMeters;
        this.completeAltitudeMeters = completeAltitudeMeters;
        this.cutoverApplied = cutoverApplied;
    }

    public UUID transitionId() {
        return transitionId;
    }

    public SpaceTransitionDirection direction() {
        return direction;
    }

    public SpaceDomain sourceDomain() {
        return sourceDomain;
    }

    public SpaceDomain targetDomain() {
        return targetDomain;
    }

    public String bodyId() {
        return bodyId;
    }

    public String sourceDimension() {
        return sourceDimension;
    }

    public String targetDimension() {
        return targetDimension;
    }

    public long startTick() {
        return startTick;
    }

    public long cutoverTick() {
        return cutoverTick;
    }

    public long completeTick() {
        return completeTick;
    }

    public double startAltitudeMeters() {
        return startAltitudeMeters;
    }

    public double cutoverAltitudeMeters() {
        return cutoverAltitudeMeters;
    }

    public double completeAltitudeMeters() {
        return completeAltitudeMeters;
    }

    public boolean cutoverApplied() {
        return cutoverApplied;
    }

    public void markCutoverApplied() {
        this.cutoverApplied = true;
    }

    public boolean shouldCutover(long tick) {
        return !cutoverApplied && tick >= cutoverTick;
    }

    public boolean isComplete(long tick) {
        return tick >= completeTick;
    }

    public double altitudeAt(long tick) {
        if (completeTick <= startTick) {
            return completeAltitudeMeters;
        }
        double progress = Math.clamp((tick - startTick) / (double) (completeTick - startTick), 0.0, 1.0);
        return startAltitudeMeters + (completeAltitudeMeters - startAltitudeMeters) * progress;
    }

    public double sdBlendAt(long tick) {
        if (direction == SpaceTransitionDirection.LANDING) {
            return Math.clamp((altitudeAt(tick) - completeAltitudeMeters)
                    / Math.max(1.0, startAltitudeMeters - completeAltitudeMeters), 0.0, 1.0);
        }
        if (cutoverTick <= startTick) {
            return 1.0;
        }
        return Math.clamp((tick - startTick) / (double) (cutoverTick - startTick), 0.0, 1.0);
    }

    public double pdBlendAt(long tick) {
        return 1.0 - sdBlendAt(tick);
    }
}
