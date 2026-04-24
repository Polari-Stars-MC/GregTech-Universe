package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

import java.util.List;

public final class VesselRenderer {
    public void render(BufferBuilder buffer, PoseStack.Pose pose, List<VesselRenderInfo> vessels, SpaceRenderConfig config) {
        for (VesselRenderInfo vessel : vessels) {
            if (vessel.playerControlled() && vessel.firstPerson()) {
                continue;
            }
            renderRemoteVessel(buffer, pose, vessel, config);
        }
    }

    public void renderRemoteVessel(BufferBuilder buffer, PoseStack.Pose pose, VesselRenderInfo vessel, SpaceRenderConfig config) {
        double distance = vessel.distanceToCamera();
        if (distance > config.space().maxVesselRenderDistance()) {
            return;
        }

        Vector3f direction = new Vector3f((float) vessel.position().x(), (float) vessel.position().y(), (float) vessel.position().z()).normalize();
        float radius = 860.0F;
        float size;
        if (distance < 500.0) {
            size = 5.5F;
        } else if (distance < 2000.0) {
            size = 3.0F;
        } else {
            PlanetProxyRenderer.addBillboard(buffer, pose, direction, radius, 2.2F, 0.75F, 0.92F, 1.0F, 0.95F);
            return;
        }

        PlanetProxyRenderer.addCube(buffer, pose, new PlanetProxyRenderer.RenderBody(
                "vessel_" + vessel.authorityId(),
                new Vector3f(direction).mul(radius),
                direction,
                new Vector3f(0.0F, 1.0F, 0.0F),
                new Vector3f(-0.35F, 0.74F, 0.58F).normalize(),
                radius,
                size,
                0.70F,
                0.76F,
                0.82F,
                0.95F,
                Math.toRadians(vessel.yawDegrees()),
                false,
                false,
                PlanetProxyRenderer.BodyStyle.SOLID,
                distance
        ));
    }

    public void renderOwnVessel(BufferBuilder buffer, PoseStack.Pose pose, VesselRenderInfo vessel, boolean isFirstPerson, SpaceRenderConfig config) {
        if (vessel == null || isFirstPerson) {
            return;
        }
        renderRemoteVessel(buffer, pose, vessel, config);
    }
}
