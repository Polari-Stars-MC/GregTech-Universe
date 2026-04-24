package org.polaris2023.gtu.space.client;

import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.List;

public interface RenderDataProvider {
    Vector3d getCameraPosition();

    Quaternionf getCameraRotation();

    double getPlayerY();

    boolean isInEVA();

    boolean isInVessel();

    VesselRenderInfo getCurrentVessel();

    List<PlanetRenderInfo> getPlanets();

    SunRenderInfo getSun();
}
