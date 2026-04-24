package org.polaris2023.gtu.space.client;

import org.joml.Vector3d;

public final class SeamlessRenderManager implements IRendererProvider {
    private final SpaceRenderConfig config;

    private RenderMode renderMode = RenderMode.GROUND;
    private SkyboxType skyboxType = SkyboxType.PLANET_BLUE;
    private boolean planetVisible;
    private float atmosphereDensity = 1.0F;
    private float starIntensity;
    private float planetAlpha;
    private float transitionProgress;
    private float starfieldBlend;
    private Vector3d gravityDirection = new Vector3d(0.0, -1.0, 0.0);

    public SeamlessRenderManager(SpaceRenderConfig config) {
        this.config = config;
    }

    public void update(double playerAltitude) {
        SpaceRenderConfig.Transition transition = config.transition();
        if (playerAltitude < transition.atmosphereStart()) {
            setRenderMode(RenderMode.GROUND);
            updateSkybox(SkyboxType.PLANET_BLUE);
            updateAtmosphere(1.0F);
            setPlanetVisibility(false);
            starIntensity = 0.08F;
            planetAlpha = 0.0F;
            updateTransitionState(0.0F, starIntensity);
            return;
        }

        if (playerAltitude < transition.atmosphereEnd()) {
            float t = smooth(normalized(playerAltitude, transition.atmosphereStart(), transition.atmosphereEnd()));
            setRenderMode(RenderMode.TRANSITION);
            updateSkybox(SkyboxType.TRANSITION);
            updateAtmosphere(1.0F - t);
            setPlanetVisibility(false);
            starIntensity = 0.08F + t * 0.32F;
            planetAlpha = 0.0F;
            updateTransitionState(t * 0.5F, starIntensity);
            return;
        }

        if (playerAltitude < transition.orbitStart()) {
            float t = smooth(normalized(playerAltitude, transition.atmosphereEnd(), transition.orbitStart()));
            setRenderMode(RenderMode.TRANSITION);
            updateSkybox(SkyboxType.TRANSITION);
            updateAtmosphere(0.18F * (1.0F - t));
            setPlanetVisibility(t > 0.001F);
            starIntensity = 0.40F + t * 0.60F;
            planetAlpha = smooth(t);
            updateTransitionState(0.5F + t * 0.5F, starIntensity);
            return;
        }

        setRenderMode(RenderMode.SPACE);
        updateSkybox(SkyboxType.DEEP_SPACE);
        updateAtmosphere(0.0F);
        setPlanetVisibility(true);
        starIntensity = 1.0F;
        planetAlpha = 1.0F;
        updateTransitionState(1.0F, 1.0F);
    }

    public void setGravityDirection(Vector3d gravityDirection) {
        this.gravityDirection = gravityDirection == null ? new Vector3d(0.0, -1.0, 0.0) : new Vector3d(gravityDirection);
    }

    public RenderMode renderMode() {
        return renderMode;
    }

    public SkyboxType skyboxType() {
        return skyboxType;
    }

    public boolean planetVisible() {
        return planetVisible;
    }

    public float atmosphereDensity() {
        return atmosphereDensity;
    }

    public float starIntensity() {
        return starIntensity;
    }

    public float planetAlpha() {
        return planetAlpha;
    }

    public Vector3d gravityDirection() {
        return new Vector3d(gravityDirection);
    }

    public float transitionProgress() {
        return transitionProgress;
    }

    public float starfieldBlend() {
        return starfieldBlend;
    }

    @Override
    public void setRenderMode(RenderMode mode) {
        this.renderMode = mode;
    }

    @Override
    public void updateSkybox(SkyboxType type) {
        this.skyboxType = type;
    }

    @Override
    public void setPlanetVisibility(boolean visible) {
        this.planetVisible = visible;
    }

    @Override
    public void updateAtmosphere(float density) {
        this.atmosphereDensity = clamp01(density);
    }

    public void updateTransitionState(float transitionProgress, float starfieldBlend) {
        this.transitionProgress = clamp01(transitionProgress);
        this.starfieldBlend = clamp01(starfieldBlend);
    }

    private static float normalized(double value, double start, double end) {
        if (end <= start) {
            return 1.0F;
        }
        return clamp01((float) ((value - start) / (end - start)));
    }

    private static float smooth(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
