package org.polaris2023.gtu.space.client;

import org.joml.Vector3d;

import java.util.List;

public interface IEntityProvider {
    List<VesselRenderInfo> getNearbyVessels();

    VesselRenderInfo getPlayerVessel();

    Vector3d getVesselPosition(VesselRenderInfo vessel);

    float getVesselRotation(VesselRenderInfo vessel);
}
