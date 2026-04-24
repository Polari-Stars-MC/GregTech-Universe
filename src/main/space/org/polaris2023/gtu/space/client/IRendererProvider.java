package org.polaris2023.gtu.space.client;

public interface IRendererProvider {
    void setRenderMode(RenderMode mode);

    void updateSkybox(SkyboxType type);

    void setPlanetVisibility(boolean visible);

    void updateAtmosphere(float density);
}
