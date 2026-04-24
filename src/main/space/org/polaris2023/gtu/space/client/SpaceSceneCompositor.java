package org.polaris2023.gtu.space.client;

import net.minecraft.client.Minecraft;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.network.SpaceTransitionPacket;
import org.polaris2023.gtu.space.runtime.PlanetDomainDefinition;
import org.polaris2023.gtu.space.runtime.SpacePlayerState;
import org.polaris2023.gtu.space.runtime.SpaceTransitionDirection;

public record SpaceSceneCompositor(
        boolean active,
        PdSceneSource pdSource,
        SdSceneSource sdSource,
        double estimatedServerTick,
        SpaceTransitionPacket transition
) {
    public static SpaceSceneCompositor resolve(Minecraft minecraft) {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (minecraft.player == null || minecraft.level == null || state == null) {
            return new SpaceSceneCompositor(false, new PdSceneSource(false, 0.0f), new SdSceneSource(false, 0.0f), 0.0, null);
        }

        SpaceTransitionPacket transition = ClientSpaceCache.transition();
        double estimatedTick = ClientSpaceCache.estimatedServerTickExact();
        float pdAlpha = 0.0f;
        float sdAlpha = 0.0f;

        if (state.mode() == SpacePlayerState.Mode.SPACE) {
            sdAlpha = 1.0f;
        } else if (transition != null && transition.active()) {
            PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(transition.bodyId());
            double altitude = altitudeAt(transition, estimatedTick);
            if (transition.direction() == SpaceTransitionDirection.TAKEOFF) {
                double preCutoverProgress = Math.clamp(
                        (altitude - definition.takeoffPrewarmAltitudeMeters())
                                / Math.max(1.0, definition.sdCutoverAltitudeMeters() - definition.takeoffPrewarmAltitudeMeters()),
                        0.0,
                        1.0
                );
                double postCutoverProgress = Math.clamp(
                        (altitude - definition.sdCutoverAltitudeMeters())
                                / Math.max(1.0, definition.sdFadeCompleteAltitudeMeters() - definition.sdCutoverAltitudeMeters()),
                        0.0,
                        1.0
                );
                if (transition.cutoverApplied()) {
                    sdAlpha = (float) (0.88 + 0.12 * smoothstep(postCutoverProgress));
                    pdAlpha = 0.0f;
                } else {
                    sdAlpha = (float) (0.72 * smoothstep(preCutoverProgress));
                    pdAlpha = 1.0f;
                }
            } else {
                double landingProgress = Math.clamp(
                        (definition.landingPrewarmAltitudeMeters() - altitude)
                                / Math.max(1.0, definition.landingPrewarmAltitudeMeters()),
                        0.0,
                        1.0
                );
                pdAlpha = (float) smoothstep(landingProgress);
                sdAlpha = 1.0f;
            }
        }

        String currentDimension = minecraft.level.dimension().location().toString();
        if (pdAlpha <= 0.001f && sdAlpha <= 0.001f && ClientSpaceCache.consumeSeamlessCompositorHold(currentDimension)) {
            if (state.sdSlotDimension() != null && state.sdSlotDimension().equals(currentDimension)) {
                sdAlpha = 1.0f;
            } else {
                pdAlpha = 1.0f;
            }
        }

        boolean active = sdAlpha > 0.001f || pdAlpha > 0.001f;
        return new SpaceSceneCompositor(
                active,
                new PdSceneSource(pdAlpha > 0.001f, pdAlpha),
                new SdSceneSource(sdAlpha > 0.001f, sdAlpha),
                estimatedTick,
                transition != null && transition.active() ? transition : null
        );
    }

    public boolean isTakeoff() {
        return transition != null && transition.direction() == SpaceTransitionDirection.TAKEOFF;
    }

    public boolean isLanding() {
        return transition != null && transition.direction() == SpaceTransitionDirection.LANDING;
    }

    public static double altitudeAt(SpaceTransitionPacket transition, double tick) {
        if (transition == null || !transition.active()) {
            return 0.0;
        }
        if (transition.completeTick() <= transition.startTick()) {
            return transition.completeAltitudeMeters();
        }
        double progress = Math.clamp((tick - transition.startTick()) / (double) (transition.completeTick() - transition.startTick()), 0.0, 1.0);
        return transition.startAltitudeMeters() + (transition.completeAltitudeMeters() - transition.startAltitudeMeters()) * progress;
    }

    private static double smoothstep(double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
