package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.client.cosmic.CosmicEarthRenderer;
import org.polaris2023.gtu.space.client.cosmic.CosmicPlanetRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CubePlanetRenderer {
    private static final double HALF_SIZE = 5000.0;
    private static final int FACES = 6;

    private final PlanetFace[] faces;
    private final SpaceRenderConfig config;

    public CubePlanetRenderer(SpaceRenderConfig config) {
        this.config = config;
        this.faces = new PlanetFace[]{
                new PlanetFace(0, new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(0.28F, 0.42F, 0.86F)),
                new PlanetFace(1, new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f(0.34F, 0.58F, 0.32F)),
                new PlanetFace(2, new Vector3f(-1.0F, 0.0F, 0.0F), new Vector3f(0.62F, 0.54F, 0.30F)),
                new PlanetFace(3, new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.78F, 0.66F, 0.40F)),
                new PlanetFace(4, new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(0.88F, 0.89F, 0.92F)),
                new PlanetFace(5, new Vector3f(0.0F, -1.0F, 0.0F), new Vector3f(0.16F, 0.18F, 0.22F))
        };
    }

    public void render(BufferBuilder buffer, PoseStack.Pose pose, PlanetRenderInfo planet, SpaceRenderRuntime runtime, float renderRadius) {
        if (planet.alpha() <= 0.001F) {
            return;
        }
        double[] lodDistances = config.planet().lodDistances();
        double realDistance = Math.max(planet.realDistanceToCamera(), 1.0);
        float centerDistance = (float) Math.max(planet.distanceToCamera(), 1.0);
        int lod = resolveLod(realDistance, lodDistances);
        RenderData renderData = faces[0].getLODData(realDistance, lodDistances);
        float size = resolvePlanetSize(planet, centerDistance, renderData.minimumScreenSize());

        Vector3f direction = resolveDisplayDirection(planet);
        Vector3f axis = new Vector3f((float) planet.rotationAxis().x(), (float) planet.rotationAxis().y(), (float) planet.rotationAxis().z()).normalize();
        PlanetProxyRenderer.RenderBody body = new PlanetProxyRenderer.RenderBody(
                planet.id(),
                new Vector3f(direction).mul(centerDistance),
                direction,
                axis,
                new Vector3f(-0.35F, 0.74F, 0.58F).normalize(),
                centerDistance,
                size,
                0.75F,
                0.80F,
                0.92F,
                planet.alpha(),
                planet.rotationPhaseRadians(),
                false,
                planet.primary(),
                "earth".equals(planet.id()) ? PlanetProxyRenderer.BodyStyle.EARTH : PlanetProxyRenderer.BodyStyle.TEXTURED,
                realDistance
        );

        if (lod <= 1) {
            PlanetProxyRenderer.addCube(buffer, pose, body);
        } else {
            renderSolidCube(buffer, pose, direction, axis, size, centerDistance, planet.alpha(), lod);
        }

        if (planet.atmosphere() && runtime.renderer().atmosphereDensity() > 0.001F) {
            renderAtmosphere(buffer, pose, direction, axis, size * 1.08F, centerDistance, planet.alpha() * runtime.renderer().atmosphereDensity(), runtime.atmosphereColor());
        }
    }

    public void renderCosmic(PoseStack poseStack, Matrix4f projectionMatrix, PlanetRenderInfo planet, SpaceRenderRuntime runtime, float renderRadius) {
        if (planet.alpha() <= 0.001F) {
            return;
        }

        double[] lodDistances = config.planet().lodDistances();
        double realDistance = Math.max(planet.realDistanceToCamera(), 1.0);
        float centerDistance = (float) Math.max(planet.distanceToCamera(), 1.0);
        RenderData renderData = faces[0].getLODData(realDistance, lodDistances);
        float size = resolvePlanetSize(planet, centerDistance, renderData.minimumScreenSize());
        Vector3f direction = resolveDisplayDirection(planet);
        Vector3f center = new Vector3f(direction).mul(centerDistance);
        float phaseDegrees = (float) Math.toDegrees(planet.rotationPhaseRadians());

        if ("earth".equals(planet.id())) {
            CosmicEarthRenderer.render(poseStack, center, size, phaseDegrees, planet.alpha(), realDistance);
            return;
        }

        CosmicPlanetRenderer.render(
                poseStack,
                planet.id(),
                center,
                new Vector3f((float) planet.rotationAxis().x(), (float) planet.rotationAxis().y(), (float) planet.rotationAxis().z()),
                size,
                phaseDegrees,
                planet.alpha(),
                realDistance
        );
    }

    private static float resolvePlanetSize(PlanetRenderInfo planet, float centerDistance, float minimumSize) {
        float renderedSize = (float) Math.max(planet.radius() * 2.0, 1.0);
        if (planet.primary()) {
            float minSize = Math.max(minimumSize, centerDistance * 0.22F);
            float maxSize = centerDistance * 0.92F;
            return (float) clamp(renderedSize, minSize, maxSize);
        }
        double fovDegrees = 70.0;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null) {
            fovDegrees = minecraft.options.fov().get();
        }
        double fov = Math.toRadians(Math.max(10.0, fovDegrees));
        double viewingAngle = Math.atan2(renderedSize, Math.max(centerDistance, 1.0F));
        double screenRatio = viewingAngle / Math.max(fov, 1.0E-3);
        float targetSize = (float) Math.max(renderedSize, centerDistance * screenRatio * 1.18);
        float maxSize = planet.primary() ? centerDistance * 0.58F : centerDistance * 0.62F;
        float minSize = planet.primary() ? Math.max(minimumSize, centerDistance * 0.12F) : minimumSize;
        return (float) clamp(targetSize, minSize, maxSize);
    }

    private static Vector3f resolveDisplayDirection(PlanetRenderInfo planet) {
        Vector3f direction = new Vector3f(
                (float) planet.worldPosition().x(),
                (float) planet.worldPosition().y(),
                (float) planet.worldPosition().z()
        );
        if (direction.lengthSquared() < 1.0E-6F) {
            return new Vector3f(0.0F, -1.0F, 0.0F);
        }
        return direction.normalize();
    }

    private void renderSolidCube(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            Vector3f axis,
            float size,
            float renderRadius,
            float alpha,
            int lod
    ) {
        List<PlanetFace> orderedFaces = new ArrayList<>(List.of(faces));
        orderedFaces.sort(Comparator.comparingDouble(face -> -face.normal().dot(direction)));
        float halfSpan = size * 0.5F;
        Vector3f up = new Vector3f(axis);
        if (up.lengthSquared() < 1.0E-6F) {
            up.set(0.0F, 1.0F, 0.0F);
        }
        Vector3f reference = Math.abs(up.z()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f right = reference.cross(up, new Vector3f()).normalize();
        Vector3f forward = up.cross(right, new Vector3f()).normalize();
        Vector3f center = new Vector3f(direction).mul(renderRadius);

        for (PlanetFace face : orderedFaces) {
            RenderData renderData = face.getLODData(0.0, config.planet().lodDistances());
            float brightness = Math.max(0.22F, face.normal().dot(new Vector3f(-0.35F, 0.74F, 0.58F).normalize()) * 0.5F + 0.5F);
            Vector3f color = new Vector3f(renderData.red(), renderData.green(), renderData.blue()).mul(brightness);
            Vector3f normal = face.normal();
            Vector3f faceCenter = new Vector3f(center)
                    .add(new Vector3f(right).mul(normal.x() * halfSpan))
                    .add(new Vector3f(up).mul(normal.y() * halfSpan))
                    .add(new Vector3f(forward).mul(normal.z() * halfSpan));

            Vector3f axisA = Math.abs(normal.y()) > 0.5F ? right : up;
            Vector3f axisB = Math.abs(normal.y()) > 0.5F ? forward : (Math.abs(normal.x()) > 0.5F ? up : right);
            axisA = new Vector3f(axisA).normalize(halfSpan);
            axisB = new Vector3f(axisB).normalize(halfSpan);

            Vector3f v0 = new Vector3f(faceCenter).sub(axisA).sub(axisB);
            Vector3f v1 = new Vector3f(faceCenter).add(axisA).sub(axisB);
            Vector3f v2 = new Vector3f(faceCenter).add(axisA).add(axisB);
            Vector3f v3 = new Vector3f(faceCenter).sub(axisA).add(axisB);

            float faceAlpha = alpha * (lod >= 3 ? 0.92F : 1.0F);
            buffer.addVertex(pose, v0.x(), v0.y(), v0.z()).setColor(color.x(), color.y(), color.z(), faceAlpha);
            buffer.addVertex(pose, v1.x(), v1.y(), v1.z()).setColor(color.x(), color.y(), color.z(), faceAlpha);
            buffer.addVertex(pose, v2.x(), v2.y(), v2.z()).setColor(color.x(), color.y(), color.z(), faceAlpha);
            buffer.addVertex(pose, v3.x(), v3.y(), v3.z()).setColor(color.x(), color.y(), color.z(), faceAlpha);
        }
    }

    private void renderAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            Vector3f axis,
            float size,
            float renderRadius,
            float alpha,
            Vector3f color
    ) {
        if (alpha <= 0.001F) {
            return;
        }
        PlanetProxyRenderer.RenderBody atmosphere = new PlanetProxyRenderer.RenderBody(
                "atmosphere",
                new Vector3f(direction).mul(renderRadius),
                direction,
                axis,
                new Vector3f(-0.35F, 0.74F, 0.58F).normalize(),
                renderRadius,
                size,
                color.x,
                color.y,
                color.z,
                alpha * config.planet().atmosphereIntensity(),
                0.0,
                false,
                false,
                PlanetProxyRenderer.BodyStyle.SOLID,
                HALF_SIZE
        );
        PlanetProxyRenderer.addCube(buffer, pose, atmosphere);
    }

    private static int resolveLod(double distance, double[] lodDistances) {
        for (int i = 0; i < Math.min(FACES - 2, lodDistances.length); i++) {
            if (distance < lodDistances[i]) {
                return i;
            }
        }
        return 3;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PlanetFace(int index, Vector3f normal, Vector3f baseColor) {
        RenderData getLODData(double distance, double[] lodDistances) {
            int lod = resolveLod(distance, lodDistances);
            return switch (lod) {
                case 0 -> new RenderData(baseColor.x, baseColor.y, baseColor.z, 14.0F);
                case 1 -> new RenderData(baseColor.x * 0.94F, baseColor.y * 0.94F, baseColor.z * 0.94F, 11.0F);
                case 2 -> new RenderData(baseColor.x * 0.82F, baseColor.y * 0.82F, baseColor.z * 0.82F, 8.0F);
                default -> new RenderData(baseColor.x * 0.68F, baseColor.y * 0.68F, baseColor.z * 0.68F, 5.0F);
            };
        }
    }

    private record RenderData(float red, float green, float blue, float minimumScreenSize) {
    }
}
