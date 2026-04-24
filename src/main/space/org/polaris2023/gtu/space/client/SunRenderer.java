package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class SunRenderer {
    private static final double EARTH_ORBIT_REFERENCE_METERS = 149_598_023_000.0;
    private static final double EARTH_APPARENT_DIAMETER_DEGREES = 0.53;

    public void render(BufferBuilder buffer, PoseStack.Pose pose, SunRenderInfo sun, float renderRadius) {
        if (sun == null || sun.intensity() <= 0.001F) {
            return;
        }

        Vector3d world = sun.worldPosition();
        double distance = Math.max(1.0, world.length());
        Vector3f direction = new Vector3f((float) world.x, (float) world.y, (float) world.z).normalize();
        double apparentDiameter = Math.clamp(
                EARTH_APPARENT_DIAMETER_DEGREES * (EARTH_ORBIT_REFERENCE_METERS / distance),
                0.08,
                2.0
        );
        float size = (float) clamp(
                2.0 * renderRadius * Math.tan(Math.toRadians(apparentDiameter * 0.5)),
                8.0,
                56.0
        );
        float intensity = Math.max(0.0F, sun.intensity());

        PlanetProxyRenderer.addBillboard(
                buffer,
                pose,
                direction,
                renderRadius,
                size * 1.9F,
                sun.red(),
                sun.green(),
                sun.blue(),
                0.12F * intensity
        );
        PlanetProxyRenderer.addBillboard(
                buffer,
                pose,
                direction,
                renderRadius,
                size * 1.3F,
                Math.min(1.0F, sun.red() * 1.04F),
                Math.min(1.0F, sun.green() * 1.02F),
                Math.min(1.0F, sun.blue()),
                0.24F * intensity
        );
        PlanetProxyRenderer.addBillboard(
                buffer,
                pose,
                direction,
                renderRadius,
                size,
                1.0F,
                Math.min(1.0F, sun.green() * 1.05F),
                Math.min(1.0F, sun.blue() * 0.98F + 0.02F),
                0.92F * intensity
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
