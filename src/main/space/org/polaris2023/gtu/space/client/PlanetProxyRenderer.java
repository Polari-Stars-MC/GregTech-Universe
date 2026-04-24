package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.network.SpaceSnapshotPacket;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlanetProxyRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetProxyRenderer.class);
    private static final Vector3f KEY_LIGHT_DIRECTION = new Vector3f(-0.35F, 0.74F, 0.58F).normalize();
    private static final Map<String, PlanetTextureSet> PLANET_TEXTURES = Map.ofEntries(
            Map.entry("earth", PlanetTextureSet.forPlanet("earth", true, true)),
            Map.entry("moon", PlanetTextureSet.forPlanet("moon", false, false)),
            Map.entry("mercury", PlanetTextureSet.forPlanet("mercury", false, false)),
            Map.entry("venus", PlanetTextureSet.forPlanet("venus", false, false)),
            Map.entry("mars", PlanetTextureSet.forPlanet("mars", false, false)),
            Map.entry("jupiter", PlanetTextureSet.forPlanet("jupiter", false, false)),
            Map.entry("saturn", PlanetTextureSet.forPlanet("saturn", false, false)),
            Map.entry("uranus", PlanetTextureSet.forPlanet("uranus", false, false)),
            Map.entry("neptune", PlanetTextureSet.forPlanet("neptune", false, false))
    );

    private PlanetProxyRenderer() {
    }

    public static void prewarmTextures() {
        for (PlanetTextureSet textureSet : PLANET_TEXTURES.values()) {
            textureSet.ensureLoaded();
        }
    }

    public static List<RenderBody> collectBodies(
            SpaceSnapshotPacket snapshot,
            SpaceStateSyncPacket state,
            SpaceSceneCompositor compositor,
            float renderRadius,
            double minimumDistance,
            double maximumDistance,
            float minimumSize
    ) {
        if (snapshot == null || state == null) {
            return List.of();
        }

        Map<String, SpaceSnapshotPacket.BodyData> bodyMap = new LinkedHashMap<>();
        for (SpaceSnapshotPacket.BodyData body : snapshot.bodies()) {
            bodyMap.put(body.id(), body);
        }

        Vec3 viewer = resolveViewer(snapshot, state, compositor, bodyMap);
        if (viewer == null) {
            return List.of();
        }

        List<RenderBody> renderBodies = new ArrayList<>();
        for (SpaceSnapshotPacket.BodyData body : snapshot.bodies()) {
            if (body.id().startsWith("asteroid_belt_")) {
                continue;
            }
            if (body.id().equals(state.bodyId()) && shouldHidePrimaryBody(compositor)) {
                continue;
            }

            Vec3 delta = new Vec3(body.posX() - viewer.x, body.posY() - viewer.y, body.posZ() - viewer.z);
            double distance = delta.length();
            if (distance < 1.0) {
                continue;
            }
            if (distance < minimumDistance || distance >= maximumDistance) {
                continue;
            }

            Vector3f direction = delta.normalizeF();
            Vector3f axis = new Vec3(body.axisX(), body.axisY(), body.axisZ()).normalizeF();
            double shellDistance = Math.max(distance - Math.max(body.radius(), 1.0), 1.0);
            double renderZoom = computeRenderZoom(distance, body.radius());
            Vector3f renderPosition = computeProxyRenderPosition(delta, distance, body.radius(), shellDistance, renderZoom, body.id().equals(state.bodyId()));
            double renderDistance = Math.max(renderPosition.length(), 1.0);
            Vector3f renderDirection = new Vector3f(renderPosition).normalize();
            float resolvedMinimumSize = body.kind() == KspBodyKind.STAR ? Math.max(12.0F, minimumSize * 1.8F) : minimumSize;
            float size = computeProxyRenderSize(distance, body.radius(), renderDistance, resolvedMinimumSize, body.id().equals(state.bodyId()));
            BodyColor color = colorFor(body.id());
            float distanceCompensation = clamp((float) (0.78 + Math.log10(Math.max(10.0, distance)) * 0.06), 0.72F, 1.0F);
            float alpha = (body.id().equals(state.bodyId()) ? 0.96F : 0.88F) * distanceCompensation;
            renderBodies.add(new RenderBody(
                    body.id(),
                    renderPosition,
                    renderDirection,
                    axis,
                    dominantStarDirection(body, snapshot.bodies()),
                    (float) renderDistance,
                    size,
                    color.red,
                    color.green,
                    color.blue,
                    alpha,
                    body.rotationPhaseRadians(),
                    body.kind() == KspBodyKind.STAR,
                    body.id().equals(state.bodyId()),
                    styleFor(body.id(), body.kind()),
                    distance
            ));
        }
        renderBodies.sort(Comparator.comparingDouble(RenderBody::viewerDistance).reversed());
        return List.copyOf(renderBodies);
    }

    public static float landingGroundAlpha(SpaceSceneCompositor compositor) {
        return compositor.isLanding() ? compositor.pdSource().alpha() : 0.0F;
    }

    public static void addBillboard(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            float radius,
            float size,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vector3f forward = new Vector3f(direction).normalize();
        Vector3f referenceUp = Math.abs(forward.y()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = forward.cross(referenceUp, new Vector3f()).normalize(size);
        Vector3f up = right.cross(forward, new Vector3f()).normalize(size);
        Vector3f center = new Vector3f(forward).mul(radius);

        Vector3f v0 = new Vector3f(center).sub(right).sub(up);
        Vector3f v1 = new Vector3f(center).add(right).sub(up);
        Vector3f v2 = new Vector3f(center).add(right).add(up);
        Vector3f v3 = new Vector3f(center).sub(right).add(up);

        buffer.addVertex(pose, v0.x(), v0.y(), v0.z()).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, v1.x(), v1.y(), v1.z()).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, v2.x(), v2.y(), v2.z()).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, v3.x(), v3.y(), v3.z()).setColor(red, green, blue, alpha);
    }

    public static void addSolidDisc(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            Vector3f keyLightDirection,
            float renderRadius,
            float size,
            float red,
            float green,
            float blue,
            float alpha,
            int radialSteps,
            int angularSteps
    ) {
        SphereFrame discFrame = sphereFrame(direction, renderRadius, size * 0.5F);
        Vector3f light = normalizedOrFallback(new Vector3f(keyLightDirection), KEY_LIGHT_DIRECTION);
        renderSolidDisc(
                buffer,
                pose,
                discFrame,
                light,
                red,
                green,
                blue,
                alpha,
                clamp(radialSteps, 2, 12),
                clamp(angularSteps, 8, 36)
        );
    }

    public static void addCube(BufferBuilder buffer, PoseStack.Pose pose, RenderBody body) {
        if (body.style() == BodyStyle.EARTH) {
            addEarthCube(buffer, pose, body);
            if ("saturn".equals(body.id())) {
                addSaturnRing(buffer, pose, body);
            }
            return;
        }
        if (body.style() == BodyStyle.TEXTURED) {
            PlanetTextureSet textures = PLANET_TEXTURES.get(body.id());
            if (textures == null) {
                addTexturedCube(buffer, pose, body);
            } else {
                textures.ensureLoaded();
                addTexturedDisc(buffer, pose, body, textures, false, false, hasAtmosphere(body.id()));
            }
            if ("saturn".equals(body.id())) {
                addSaturnRing(buffer, pose, body);
            }
            return;
        }
        addSolidCube(
                buffer,
                pose,
                body.direction(),
                body.axis(),
                body.keyLightDirection(),
                body.renderDistance(),
                body.size(),
                body.size() * 0.5F,
                body.red(),
                body.green(),
                body.blue(),
                body.alpha(),
                body.rotationPhaseRadians()
        );
        if ("saturn".equals(body.id())) {
            addSaturnRing(buffer, pose, body);
        }
    }

    public static boolean hasAtmosphere(String bodyId) {
        return switch (bodyId) {
            case "earth", "venus", "mars", "jupiter", "saturn", "uranus", "neptune" -> true;
            default -> false;
        };
    }

    public static void addCubeAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            RenderBody body,
            float scale,
            float alpha,
            Vector3f color
    ) {
        if (alpha <= 0.001F) {
            return;
        }
        float halfSpan = body.size() * 0.5F * scale;
        float halfDepth = halfSpan;
        CubeFrame frame = cubeFrame(body.direction(), body.axis(), body.renderDistance(), halfSpan, halfDepth, body.rotationPhaseRadians());
        Vector3f lightLocal = worldToLocal(frame, normalizedOrFallback(new Vector3f(body.keyLightDirection()), KEY_LIGHT_DIRECTION));
        Vector3f viewLocal = localViewDirection(frame);
        int subdivisions = clamp(resolveSubdivisions(body.viewerDistance()), 2, 6);

        for (CubeSide side : sortedSides(frame)) {
            float step = 2.0F / subdivisions;
            for (int y = 0; y < subdivisions; y++) {
                float v0 = -1.0F + y * step;
                float v1 = v0 + step;
                for (int x = 0; x < subdivisions; x++) {
                    float u0 = -1.0F + x * step;
                    float u1 = u0 + step;

                    addCubeAtmosphereVertex(buffer, pose, frame, side, u0, v0, lightLocal, viewLocal, alpha, color);
                    addCubeAtmosphereVertex(buffer, pose, frame, side, u1, v0, lightLocal, viewLocal, alpha, color);
                    addCubeAtmosphereVertex(buffer, pose, frame, side, u1, v1, lightLocal, viewLocal, alpha, color);
                    addCubeAtmosphereVertex(buffer, pose, frame, side, u0, v1, lightLocal, viewLocal, alpha, color);
                }
            }
        }
    }

    private static void addCubeAtmosphereVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            float u,
            float v,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alphaScale,
            Vector3f color
    ) {
        Vector3f local = side.local(u, v);
        Vector3f normal = side.normal();
        float viewDot = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float edge = clamp((Math.max(Math.abs(u), Math.abs(v)) - 0.35F) / 0.65F, 0.0F, 1.0F);
        float rim = (float) Math.pow(1.0F - viewDot, 2.4F) * (0.40F + edge * 0.60F);
        float sunAmount = 0.30F + clamp(normal.dot(lightLocal), 0.0F, 1.0F) * 0.70F;
        float vertexAlpha = alphaScale * rim * (0.04F + sunAmount * 0.18F);

        Vector3f position = cubePoint(frame, local);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(color.x() * sunAmount, 0.0F, 1.0F),
                clamp(color.y() * sunAmount, 0.0F, 1.0F),
                clamp(color.z() * sunAmount, 0.0F, 1.0F),
                clamp(vertexAlpha, 0.0F, 1.0F)
        );
    }

    private static void addSaturnRing(BufferBuilder buffer, PoseStack.Pose pose, RenderBody body) {
        float ringAlpha = clamp(body.alpha() * 0.55F, 0.08F, 0.62F);
        float inner = body.size() * 0.72F;
        float cassiniInner = body.size() * 0.93F;
        float cassiniOuter = body.size() * 1.03F;
        float outer = body.size() * 1.46F;

        Vector3f axis = new Vector3f(body.axis());
        if (axis.lengthSquared() < 1.0E-6F) {
            axis.set(0.0F, 1.0F, 0.0F);
        } else {
            axis.normalize();
        }
        Vector3f reference = Math.abs(axis.y()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f tangent = axis.cross(reference, new Vector3f());
        if (tangent.lengthSquared() < 1.0E-6F) {
            tangent = axis.cross(new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f());
        }
        tangent.normalize();
        Vector3f bitangent = axis.cross(tangent, new Vector3f()).normalize();
        Vector3f center = new Vector3f(body.direction()).normalize().mul(body.renderDistance());

        renderRingBand(buffer, pose, center, tangent, bitangent, inner, cassiniInner, 40, 0.84F, 0.79F, 0.67F, ringAlpha * 0.72F);
        renderRingBand(buffer, pose, center, tangent, bitangent, cassiniOuter, outer, 56, 0.78F, 0.73F, 0.61F, ringAlpha);
    }

    private static void renderRingBand(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f center,
            Vector3f tangent,
            Vector3f bitangent,
            float innerRadius,
            float outerRadius,
            int slices,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        for (int i = 0; i < slices; i++) {
            float t0 = (i / (float) slices) * Mth.TWO_PI;
            float t1 = ((i + 1) / (float) slices) * Mth.TWO_PI;

            Vector3f in0 = ringPoint(center, tangent, bitangent, innerRadius, t0);
            Vector3f in1 = ringPoint(center, tangent, bitangent, innerRadius, t1);
            Vector3f out1 = ringPoint(center, tangent, bitangent, outerRadius, t1);
            Vector3f out0 = ringPoint(center, tangent, bitangent, outerRadius, t0);

            float phase = (float) (0.72F + 0.28F * Math.sin((t0 + t1) * 0.5F * 4.0F));
            float segAlpha = alpha * phase;

            buffer.addVertex(pose, in0.x(), in0.y(), in0.z()).setColor(red, green, blue, segAlpha);
            buffer.addVertex(pose, in1.x(), in1.y(), in1.z()).setColor(red, green, blue, segAlpha);
            buffer.addVertex(pose, out1.x(), out1.y(), out1.z()).setColor(red, green, blue, segAlpha);
            buffer.addVertex(pose, out0.x(), out0.y(), out0.z()).setColor(red, green, blue, segAlpha);
        }
    }

    private static Vector3f ringPoint(
            Vector3f center,
            Vector3f tangent,
            Vector3f bitangent,
            float radius,
            float angle
    ) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        return new Vector3f(center)
                .add(new Vector3f(tangent).mul(radius * cos))
                .add(new Vector3f(bitangent).mul(radius * sin));
    }

    private static long nextEarthDiagNanos;

    private static void addEarthCube(BufferBuilder buffer, PoseStack.Pose pose, RenderBody body) {
        PlanetTextureSet textures = PLANET_TEXTURES.get("earth");
        if (textures == null) {
            long now = System.nanoTime();
            if (now >= nextEarthDiagNanos) { nextEarthDiagNanos = now + 3_000_000_000L; LOGGER.warn("[GTU-DIAG] addEarthCube: textures=null, fallback to solid"); }
            addSolidCube(
                    buffer,
                    pose,
                    body.direction(),
                    body.axis(),
                    body.keyLightDirection(),
                    body.renderDistance(),
                    body.size(),
                    body.size() * 0.5F,
                    body.red(),
                    body.green(),
                    body.blue(),
                    body.alpha(),
                    body.rotationPhaseRadians()
            );
            return;
        }
        textures.ensureLoaded();
        if (!textures.hasSurface()) {
            addSolidCube(
                    buffer,
                    pose,
                    body.direction(),
                    body.axis(),
                    body.keyLightDirection(),
                    body.renderDistance(),
                    body.size(),
                    body.size() * 0.5F,
                    body.red(),
                    body.green(),
                    body.blue(),
                    body.alpha(),
                    body.rotationPhaseRadians()
            );
            return;
        }

        float halfSpan = body.size() * 0.5F;
        float halfDepth = halfSpan;
        CubeFrame frame = cubeFrame(body.direction(), body.axis(), body.renderDistance(), halfSpan, halfDepth, body.rotationPhaseRadians());
        Vector3f lightLocal = worldToLocal(frame, normalizedOrFallback(new Vector3f(body.keyLightDirection()), KEY_LIGHT_DIRECTION));
        Vector3f viewLocal = localViewDirection(frame);
        int subdivisions = resolveSubdivisions(body.viewerDistance());

        renderEarthSurface(buffer, pose, frame, textures, lightLocal, viewLocal, body.alpha(), subdivisions);
        renderEarthClouds(buffer, pose, frame, textures, lightLocal, body.alpha(), subdivisions);
        renderEarthAtmosphere(buffer, pose, frame, lightLocal, viewLocal, body.alpha(), subdivisions);
    }

    private static void renderEarthSphereSurface(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame sphereFrame,
            BodyFrame bodyFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            int rings,
            int slices
    ) {
        for (int ring = rings - 1; ring >= 0; ring--) {
            float theta0 = (float) (ring * (Math.PI * 0.5) / rings);
            float theta1 = (float) ((ring + 1) * (Math.PI * 0.5) / rings);
            for (int slice = 0; slice < slices; slice++) {
                float phi0 = slice * Mth.TWO_PI / slices;
                float phi1 = (slice + 1) * Mth.TWO_PI / slices;

                addEarthSphereSurfaceVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta0, phi0);
                addEarthSphereSurfaceVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta0, phi1);
                addEarthSphereSurfaceVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta1, phi1);
                addEarthSphereSurfaceVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta1, phi0);
            }
        }
    }

    private static void addEarthSphereSurfaceVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame sphereFrame,
            BodyFrame bodyFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            float theta,
            float phi
    ) {
        Vector3f normal = sphereNormal(sphereFrame, theta, phi);
        float u = sphereTextureU(bodyFrame, normal);
        float v = sphereTextureV(bodyFrame, normal);
        TextureSample day = textures.sampleSurface(u, v);
        TextureSample night = textures.sampleNight(u, v);

        float lambert = clamp(normal.dot(light), 0.0F, 1.0F);
        float diffuse = 0.16F + lambert * 0.84F;
        float twilight = clamp(1.0F - lambert * 1.35F, 0.0F, 1.0F);
        float nightMask = night.luminance() * twilight * twilight * 1.2F;

        float red = clamp(day.red() * diffuse + nightMask * 1.04F, 0.0F, 1.0F);
        float green = clamp(day.green() * diffuse + nightMask * 0.85F, 0.0F, 1.0F);
        float blue = clamp(day.blue() * diffuse + nightMask * 0.56F, 0.0F, 1.0F);

        Vector3f position = spherePosition(sphereFrame, normal, 1.0F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(red, green, blue, alpha);
    }

    private static void renderEarthSphereClouds(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame sphereFrame,
            BodyFrame bodyFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            int rings,
            int slices
    ) {
        if (!textures.hasClouds()) {
            return;
        }

        for (int ring = rings - 1; ring >= 0; ring--) {
            float theta0 = (float) (ring * (Math.PI * 0.5) / rings);
            float theta1 = (float) ((ring + 1) * (Math.PI * 0.5) / rings);
            for (int slice = 0; slice < slices; slice++) {
                float phi0 = slice * Mth.TWO_PI / slices;
                float phi1 = (slice + 1) * Mth.TWO_PI / slices;

                addEarthSphereCloudVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta0, phi0);
                addEarthSphereCloudVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta0, phi1);
                addEarthSphereCloudVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta1, phi1);
                addEarthSphereCloudVertex(buffer, pose, sphereFrame, bodyFrame, textures, light, alpha, theta1, phi0);
            }
        }
    }

    private static void addEarthSphereCloudVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame sphereFrame,
            BodyFrame bodyFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            float theta,
            float phi
    ) {
        Vector3f normal = sphereNormal(sphereFrame, theta, phi);
        float u = sphereTextureU(bodyFrame, normal);
        float v = sphereTextureV(bodyFrame, normal);
        TextureSample cloud = textures.sampleCloud(u, v);
        float cloudMask = Math.max(cloud.alpha(), cloud.luminance());
        float lambert = clamp(normal.dot(light), 0.0F, 1.0F);
        float whiten = 0.72F + lambert * 0.28F;
        float cloudAlpha = alpha * cloudMask * (0.10F + lambert * 0.16F);

        Vector3f position = spherePosition(sphereFrame, normal, 1.018F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(whiten, 0.0F, 1.0F),
                clamp(whiten, 0.0F, 1.0F),
                clamp(whiten, 0.0F, 1.0F),
                clamp(cloudAlpha, 0.0F, 1.0F)
        );
    }

    private static void renderEarthSphereAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame sphereFrame,
            Vector3f light,
            float alpha,
            int rings,
            int slices
    ) {
        for (int ring = rings - 1; ring >= 0; ring--) {
            float theta0 = (float) (ring * (Math.PI * 0.5) / rings);
            float theta1 = (float) ((ring + 1) * (Math.PI * 0.5) / rings);
            for (int slice = 0; slice < slices; slice++) {
                float phi0 = slice * Mth.TWO_PI / slices;
                float phi1 = (slice + 1) * Mth.TWO_PI / slices;

                addEarthSphereAtmosphereVertex(buffer, pose, sphereFrame, light, alpha, theta0, phi0);
                addEarthSphereAtmosphereVertex(buffer, pose, sphereFrame, light, alpha, theta0, phi1);
                addEarthSphereAtmosphereVertex(buffer, pose, sphereFrame, light, alpha, theta1, phi1);
                addEarthSphereAtmosphereVertex(buffer, pose, sphereFrame, light, alpha, theta1, phi0);
            }
        }
    }

    private static void addEarthSphereAtmosphereVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame sphereFrame,
            Vector3f light,
            float alpha,
            float theta,
            float phi
    ) {
        Vector3f normal = sphereNormal(sphereFrame, theta, phi);
        float viewDot = clamp(normal.dot(sphereFrame.forward()), 0.0F, 1.0F);
        float rim = (float) Math.pow(1.0F - viewDot, 2.4F);
        float sunAmount = 0.28F + clamp(normal.dot(light), 0.0F, 1.0F) * 0.72F;
        float atmosphereAlpha = alpha * rim * (0.05F + sunAmount * 0.18F);
        float red = 0.22F + sunAmount * 0.16F;
        float green = 0.45F + sunAmount * 0.22F;
        float blue = 0.88F + sunAmount * 0.10F;

        Vector3f position = spherePosition(sphereFrame, normal, 1.06F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(red, 0.0F, 1.0F),
                clamp(green, 0.0F, 1.0F),
                clamp(blue, 0.0F, 1.0F),
                clamp(atmosphereAlpha, 0.0F, 1.0F)
        );
    }

    private static SphereFrame sphereFrame(Vector3f direction, float renderRadius, float radius) {
        Vector3f centerDirection = normalizedOrFallback(new Vector3f(direction), new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f forward = new Vector3f(centerDirection).negate();
        Vector3f referenceUp = Math.abs(forward.y()) > 0.92F
                ? new Vector3f(1.0F, 0.0F, 0.0F)
                : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = referenceUp.cross(forward, new Vector3f()).normalize();
        Vector3f up = forward.cross(right, new Vector3f()).normalize();
        Vector3f center = new Vector3f(centerDirection).mul(renderRadius);
        return new SphereFrame(center, right, up, forward, radius);
    }

    private static BodyFrame bodyFrame(Vector3f axisHint, double rotationPhaseRadians) {
        Vector3f north = normalizedOrFallback(new Vector3f(axisHint), new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f reference = stableReference(north);
        Vector3f east = reference.cross(north, new Vector3f()).normalize();
        Vector3f prime = north.cross(east, new Vector3f()).normalize();

        Quaternionf spin = new Quaternionf().fromAxisAngleRad(north.x(), north.y(), north.z(), (float) rotationPhaseRadians);
        spin.transform(east);
        spin.transform(prime);
        return new BodyFrame(east, north, prime);
    }

    private static Vector3f sphereNormal(SphereFrame frame, float theta, float phi) {
        float sinTheta = Mth.sin(theta);
        float cosTheta = Mth.cos(theta);
        float cosPhi = Mth.cos(phi);
        float sinPhi = Mth.sin(phi);
        return new Vector3f(frame.forward()).mul(cosTheta)
                .add(new Vector3f(frame.right()).mul(sinTheta * cosPhi))
                .add(new Vector3f(frame.up()).mul(sinTheta * sinPhi))
                .normalize();
    }

    private static Vector3f spherePosition(SphereFrame frame, Vector3f normal, float radiusScale) {
        return new Vector3f(frame.center()).add(new Vector3f(normal).mul(frame.radius() * radiusScale));
    }

    private static Vector3f discNormal(SphereFrame frame, float radial, float phi) {
        float x = radial * Mth.cos(phi);
        float y = radial * Mth.sin(phi);
        float z = Mth.sqrt(Math.max(0.0F, 1.0F - radial * radial));
        return new Vector3f(frame.right()).mul(x)
                .add(new Vector3f(frame.up()).mul(y))
                .add(new Vector3f(frame.forward()).mul(z))
                .normalize();
    }

    private static Vector3f discPosition(SphereFrame frame, float radial, float phi, float radiusScale, float depthScale) {
        float x = radial * Mth.cos(phi);
        float y = radial * Mth.sin(phi);
        return new Vector3f(frame.center())
                .add(new Vector3f(frame.right()).mul(x * frame.radius() * radiusScale))
                .add(new Vector3f(frame.up()).mul(y * frame.radius() * radiusScale))
                .add(new Vector3f(frame.forward()).mul(frame.radius() * depthScale));
    }

    private static float sphereTextureU(BodyFrame frame, Vector3f normal) {
        float longitude = (float) Math.atan2(normal.dot(frame.east()), normal.dot(frame.prime()));
        float u = 0.5F + longitude / Mth.TWO_PI;
        return u - (float) Math.floor(u);
    }

    private static float sphereTextureV(BodyFrame frame, Vector3f normal) {
        float latitude = clamp(normal.dot(frame.north()), -1.0F, 1.0F);
        return clamp(0.5F - (float) (Math.asin(latitude) / Math.PI), 0.0F, 1.0F);
    }

    private static void addTexturedDisc(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            RenderBody body,
            PlanetTextureSet textures,
            boolean includeNight,
            boolean includeClouds,
            boolean includeAtmosphere
    ) {
        SphereFrame discFrame = sphereFrame(body.direction(), body.renderDistance(), body.size() * 0.5F);
        BodyFrame textureFrame = bodyFrame(body.axis(), body.rotationPhaseRadians());
        Vector3f light = normalizedOrFallback(new Vector3f(body.keyLightDirection()), KEY_LIGHT_DIRECTION);
        int angularSteps = clamp((int) Math.round(body.size() / 34.0F), includeClouds ? 18 : 16, includeClouds ? 30 : 24);
        int radialSteps = clamp(angularSteps / 3, 5, 10);

        renderTexturedDiscSurface(buffer, pose, discFrame, textureFrame, textures, light, body.alpha(), radialSteps, angularSteps, includeNight);
        if (includeClouds && textures.hasClouds() && body.size() >= 48.0F) {
            renderTexturedDiscClouds(buffer, pose, discFrame, textureFrame, textures, light, body.alpha(), radialSteps, angularSteps);
        }
        if (includeAtmosphere && body.size() >= 56.0F) {
            renderTexturedDiscAtmosphere(buffer, pose, discFrame, light, body.alpha(), radialSteps, angularSteps);
        }
    }

    private static void renderSolidDisc(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            Vector3f light,
            float red,
            float green,
            float blue,
            float alpha,
            int radialSteps,
            int angularSteps
    ) {
        for (int ring = radialSteps - 1; ring >= 0; ring--) {
            float r0 = ring / (float) radialSteps;
            float r1 = (ring + 1) / (float) radialSteps;
            for (int slice = 0; slice < angularSteps; slice++) {
                float phi0 = slice * Mth.TWO_PI / angularSteps;
                float phi1 = (slice + 1) * Mth.TWO_PI / angularSteps;

                addSolidDiscVertex(buffer, pose, frame, light, red, green, blue, alpha, r0, phi0);
                addSolidDiscVertex(buffer, pose, frame, light, red, green, blue, alpha, r0, phi1);
                addSolidDiscVertex(buffer, pose, frame, light, red, green, blue, alpha, r1, phi1);
                addSolidDiscVertex(buffer, pose, frame, light, red, green, blue, alpha, r1, phi0);
            }
        }
    }

    private static void addSolidDiscVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            Vector3f light,
            float red,
            float green,
            float blue,
            float alpha,
            float radial,
            float phi
    ) {
        Vector3f normal = discNormal(frame, radial, phi);
        float lambert = clamp(normal.dot(light), 0.0F, 1.0F);
        float viewDot = clamp(normal.dot(frame.forward()), 0.0F, 1.0F);
        float diffuse = 0.18F + lambert * 0.76F;
        float rim = (float) Math.pow(1.0F - viewDot, 1.8F) * 0.08F;
        Vector3f position = discPosition(frame, radial, phi, 1.0F, 0.0F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(red * (diffuse + rim), 0.0F, 1.0F),
                clamp(green * (diffuse + rim), 0.0F, 1.0F),
                clamp(blue * (diffuse + rim), 0.0F, 1.0F),
                alpha
        );
    }

    private static void renderTexturedDiscSurface(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            BodyFrame textureFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            int radialSteps,
            int angularSteps,
            boolean includeNight
    ) {
        for (int ring = radialSteps - 1; ring >= 0; ring--) {
            float r0 = ring / (float) radialSteps;
            float r1 = (ring + 1) / (float) radialSteps;
            for (int slice = 0; slice < angularSteps; slice++) {
                float phi0 = slice * Mth.TWO_PI / angularSteps;
                float phi1 = (slice + 1) * Mth.TWO_PI / angularSteps;

                addTexturedDiscSurfaceVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r0, phi0, includeNight);
                addTexturedDiscSurfaceVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r0, phi1, includeNight);
                addTexturedDiscSurfaceVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r1, phi1, includeNight);
                addTexturedDiscSurfaceVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r1, phi0, includeNight);
            }
        }
    }

    private static void addTexturedDiscSurfaceVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            BodyFrame textureFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            float radial,
            float phi,
            boolean includeNight
    ) {
        Vector3f normal = discNormal(frame, radial, phi);
        float u = sphereTextureU(textureFrame, normal);
        float v = sphereTextureV(textureFrame, normal);
        TextureSample day = textures.sampleSurface(u, v);
        TextureSample night = includeNight ? textures.sampleNight(u, v) : new TextureSample(0.0F, 0.0F, 0.0F, 1.0F);

        float lambert = clamp(normal.dot(light), 0.0F, 1.0F);
        float diffuse = 0.16F + lambert * 0.84F;
        float twilight = clamp(1.0F - lambert * 1.35F, 0.0F, 1.0F);
        float nightMask = includeNight ? night.luminance() * twilight * twilight * 1.2F : 0.0F;

        Vector3f position = discPosition(frame, radial, phi, 1.0F, 0.0F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(day.red() * diffuse + nightMask * 1.04F, 0.0F, 1.0F),
                clamp(day.green() * diffuse + nightMask * 0.85F, 0.0F, 1.0F),
                clamp(day.blue() * diffuse + nightMask * 0.56F, 0.0F, 1.0F),
                alpha
        );
    }

    private static void renderTexturedDiscClouds(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            BodyFrame textureFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            int radialSteps,
            int angularSteps
    ) {
        for (int ring = radialSteps - 1; ring >= 0; ring--) {
            float r0 = ring / (float) radialSteps;
            float r1 = (ring + 1) / (float) radialSteps;
            for (int slice = 0; slice < angularSteps; slice++) {
                float phi0 = slice * Mth.TWO_PI / angularSteps;
                float phi1 = (slice + 1) * Mth.TWO_PI / angularSteps;

                addTexturedDiscCloudVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r0, phi0);
                addTexturedDiscCloudVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r0, phi1);
                addTexturedDiscCloudVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r1, phi1);
                addTexturedDiscCloudVertex(buffer, pose, frame, textureFrame, textures, light, alpha, r1, phi0);
            }
        }
    }

    private static void addTexturedDiscCloudVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            BodyFrame textureFrame,
            PlanetTextureSet textures,
            Vector3f light,
            float alpha,
            float radial,
            float phi
    ) {
        Vector3f normal = discNormal(frame, radial, phi);
        float u = sphereTextureU(textureFrame, normal);
        float v = sphereTextureV(textureFrame, normal);
        TextureSample cloud = textures.sampleCloud(u, v);
        float cloudMask = Math.max(cloud.alpha(), cloud.luminance());
        float lambert = clamp(normal.dot(light), 0.0F, 1.0F);
        float whiten = 0.72F + lambert * 0.28F;
        float cloudAlpha = alpha * cloudMask * (0.10F + lambert * 0.16F);

        Vector3f position = discPosition(frame, radial, phi, 1.018F, 0.004F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(whiten, 0.0F, 1.0F),
                clamp(whiten, 0.0F, 1.0F),
                clamp(whiten, 0.0F, 1.0F),
                clamp(cloudAlpha, 0.0F, 1.0F)
        );
    }

    private static void renderTexturedDiscAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            Vector3f light,
            float alpha,
            int radialSteps,
            int angularSteps
    ) {
        for (int ring = radialSteps - 1; ring >= 0; ring--) {
            float r0 = ring / (float) radialSteps;
            float r1 = (ring + 1) / (float) radialSteps;
            for (int slice = 0; slice < angularSteps; slice++) {
                float phi0 = slice * Mth.TWO_PI / angularSteps;
                float phi1 = (slice + 1) * Mth.TWO_PI / angularSteps;

                addTexturedDiscAtmosphereVertex(buffer, pose, frame, light, alpha, r0, phi0);
                addTexturedDiscAtmosphereVertex(buffer, pose, frame, light, alpha, r0, phi1);
                addTexturedDiscAtmosphereVertex(buffer, pose, frame, light, alpha, r1, phi1);
                addTexturedDiscAtmosphereVertex(buffer, pose, frame, light, alpha, r1, phi0);
            }
        }
    }

    private static void addTexturedDiscAtmosphereVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            SphereFrame frame,
            Vector3f light,
            float alpha,
            float radial,
            float phi
    ) {
        Vector3f normal = discNormal(frame, radial, phi);
        float viewDot = clamp(normal.dot(frame.forward()), 0.0F, 1.0F);
        float rim = (float) Math.pow(1.0F - viewDot, 2.4F);
        float sunAmount = 0.28F + clamp(normal.dot(light), 0.0F, 1.0F) * 0.72F;
        float atmosphereAlpha = alpha * rim * (0.05F + sunAmount * 0.18F);
        float red = 0.22F + sunAmount * 0.16F;
        float green = 0.45F + sunAmount * 0.22F;
        float blue = 0.88F + sunAmount * 0.10F;

        Vector3f position = discPosition(frame, radial, phi, 1.08F, 0.008F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(red, 0.0F, 1.0F),
                clamp(green, 0.0F, 1.0F),
                clamp(blue, 0.0F, 1.0F),
                clamp(atmosphereAlpha, 0.0F, 1.0F)
        );
    }

    private static void addTexturedCube(BufferBuilder buffer, PoseStack.Pose pose, RenderBody body) {
        PlanetTextureSet textures = PLANET_TEXTURES.get(body.id());
        if (textures == null) {
            addSolidCube(
                    buffer,
                pose,
                body.direction(),
                body.axis(),
                body.keyLightDirection(),
                body.renderDistance(),
                body.size(),
                body.size() * 0.5F,
                    body.red(),
                    body.green(),
                    body.blue(),
                    body.alpha(),
                    body.rotationPhaseRadians()
            );
            return;
        }

        textures.ensureLoaded();
        if (!textures.hasSurface()) {
            addSolidCube(
                    buffer,
                pose,
                body.direction(),
                body.axis(),
                body.keyLightDirection(),
                body.renderDistance(),
                body.size(),
                body.size() * 0.5F,
                    body.red(),
                    body.green(),
                    body.blue(),
                    body.alpha(),
                    body.rotationPhaseRadians()
            );
            return;
        }

        addTexturedCubeGeometry(buffer, pose, body, textures);
    }

    private static void addTexturedCubeGeometry(BufferBuilder buffer, PoseStack.Pose pose, RenderBody body, PlanetTextureSet textures) {
        float halfSpan = body.size() * 0.5F;
        float halfDepth = halfSpan;
        CubeFrame frame = cubeFrame(body.direction(), body.axis(), body.renderDistance(), halfSpan, halfDepth, body.rotationPhaseRadians());
        Vector3f lightLocal = worldToLocal(frame, normalizedOrFallback(new Vector3f(body.keyLightDirection()), KEY_LIGHT_DIRECTION));
        int subdivisions = resolveSubdivisions(body.viewerDistance());

        renderTexturedSurface(buffer, pose, frame, textures, lightLocal, body.alpha(), subdivisions);
    }

    private static int resolveSubdivisions(double viewerDistance) {
        if (viewerDistance < 2000.0) return 8;
        if (viewerDistance < 5000.0) return 4;
        if (viewerDistance < 10000.0) return 2;
        return 1;
    }

    private static void renderEarthSurface(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            PlanetTextureSet textures,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alpha,
            int subdivisions
    ) {
        for (CubeSide side : sortedSides(frame)) {
            float step = 2.0F / subdivisions;
            for (int y = 0; y < subdivisions; y++) {
                float v0 = -1.0F + y * step;
                float v1 = v0 + step;
                for (int x = 0; x < subdivisions; x++) {
                    float u0 = -1.0F + x * step;
                    float u1 = u0 + step;

                    addEarthSurfaceVertex(buffer, pose, frame, side, u0, v0, textures, lightLocal, viewLocal, alpha);
                    addEarthSurfaceVertex(buffer, pose, frame, side, u1, v0, textures, lightLocal, viewLocal, alpha);
                    addEarthSurfaceVertex(buffer, pose, frame, side, u1, v1, textures, lightLocal, viewLocal, alpha);
                    addEarthSurfaceVertex(buffer, pose, frame, side, u0, v1, textures, lightLocal, viewLocal, alpha);
                }
            }
        }
    }

    private static void addEarthSurfaceVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            float u,
            float v,
            PlanetTextureSet textures,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alpha
    ) {
        Vector3f local = side.local(u, v);
        float textureU = side.textureU(u, v);
        float textureV = side.textureV(u, v);
        TextureSample day = textures.sampleSurface(side, textureU, textureV);
        TextureSample night = textures.sampleNight(side, textureU, textureV);
        Vector3f normal = side.normal();

        float lambert = clamp(normal.dot(lightLocal), 0.0F, 1.0F);
        float viewDot = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float diffuse = 0.10F + lambert * 0.90F;
        float twilight = clamp(1.0F - lambert * 1.65F, 0.0F, 1.0F);
        float rim = (float) Math.pow(1.0F - viewDot, 2.35F);
        float oceanMask = clamp((day.blue() - Math.max(day.red(), day.green())) * 2.6F + 0.28F, 0.0F, 1.0F);
        float landMask = 1.0F - oceanMask;
        Vector3f halfVector = normalizedOrFallback(new Vector3f(lightLocal).add(viewLocal), lightLocal);
        float specular = oceanMask * (float) Math.pow(clamp(normal.dot(halfVector), 0.0F, 1.0F), 18.0F) * 0.42F;
        float nightMask = night.luminance() * twilight * twilight * 1.75F;

        float baseRed = day.red() * (0.86F + landMask * 0.10F);
        float baseGreen = day.green() * (0.88F + landMask * 0.16F);
        float baseBlue = day.blue() * (0.78F + oceanMask * 0.30F);

        float red = baseRed * diffuse + rim * (0.03F + oceanMask * 0.04F) + specular * 0.28F + nightMask * 1.18F;
        float green = baseGreen * diffuse + rim * (0.06F + oceanMask * 0.05F) + specular * 0.36F + nightMask * 0.92F;
        float blue = baseBlue * diffuse + rim * (0.12F + oceanMask * 0.10F) + specular * 0.70F + nightMask * 0.58F;

        Vector3f position = cubePoint(frame, local);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(red, 0.0F, 1.0F),
                clamp(green, 0.0F, 1.0F),
                clamp(blue, 0.0F, 1.0F),
                alpha
        );
    }

    private static void renderTexturedSurface(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            PlanetTextureSet textures,
            Vector3f lightLocal,
            float alpha,
            int subdivisions
    ) {
        for (CubeSide side : sortedSides(frame)) {
            float step = 2.0F / subdivisions;
            for (int y = 0; y < subdivisions; y++) {
                float v0 = -1.0F + y * step;
                float v1 = v0 + step;
                for (int x = 0; x < subdivisions; x++) {
                    float u0 = -1.0F + x * step;
                    float u1 = u0 + step;

                    addTexturedSurfaceVertex(buffer, pose, frame, side, u0, v0, textures, lightLocal, alpha);
                    addTexturedSurfaceVertex(buffer, pose, frame, side, u1, v0, textures, lightLocal, alpha);
                    addTexturedSurfaceVertex(buffer, pose, frame, side, u1, v1, textures, lightLocal, alpha);
                    addTexturedSurfaceVertex(buffer, pose, frame, side, u0, v1, textures, lightLocal, alpha);
                }
            }
        }
    }

    private static void addTexturedSurfaceVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            float u,
            float v,
            PlanetTextureSet textures,
            Vector3f lightLocal,
            float alpha
    ) {
        Vector3f local = side.local(u, v);
        TextureSample day = textures.sampleSurface(side, side.textureU(u, v), side.textureV(u, v));
        float lambert = clamp(side.normal().dot(lightLocal), 0.0F, 1.0F);
        float diffuse = 0.14F + lambert * 0.86F;

        Vector3f position = cubePoint(frame, local);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(day.red() * diffuse, 0.0F, 1.0F),
                clamp(day.green() * diffuse, 0.0F, 1.0F),
                clamp(day.blue() * diffuse, 0.0F, 1.0F),
                alpha
        );
    }

    private static void renderEarthClouds(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            PlanetTextureSet textures,
            Vector3f lightLocal,
            float alpha,
            int subdivisions
    ) {
        if (!textures.hasClouds()) {
            return;
        }

        for (CubeSide side : sortedSides(frame)) {
            float step = 2.0F / subdivisions;
            for (int y = 0; y < subdivisions; y++) {
                float v0 = -1.0F + y * step;
                float v1 = v0 + step;
                for (int x = 0; x < subdivisions; x++) {
                    float u0 = -1.0F + x * step;
                    float u1 = u0 + step;

                    addEarthCloudVertex(buffer, pose, frame, side, u0, v0, textures, lightLocal, alpha);
                    addEarthCloudVertex(buffer, pose, frame, side, u1, v0, textures, lightLocal, alpha);
                    addEarthCloudVertex(buffer, pose, frame, side, u1, v1, textures, lightLocal, alpha);
                    addEarthCloudVertex(buffer, pose, frame, side, u0, v1, textures, lightLocal, alpha);
                }
            }
        }
    }

    private static void addEarthCloudVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            float u,
            float v,
            PlanetTextureSet textures,
            Vector3f lightLocal,
            float alphaScale
    ) {
        Vector3f local = side.local(u, v);
        TextureSample cloudSample = textures.sampleCloud(side, side.textureU(u, v), side.textureV(u, v));
        float cloud = Math.max(cloudSample.luminance(), cloudSample.alpha());
        if (cloud <= 0.01F) {
            Vector3f position = cubeShellPoint(frame, local, 1.018F);
            buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(1.0F, 1.0F, 1.0F, 0.0F);
            return;
        }

        float lambert = clamp(side.normal().dot(lightLocal), 0.0F, 1.0F);
        float cloudAlpha = alphaScale * cloud * (0.08F + 0.26F * (0.30F + lambert * 0.70F));
        float whiten = 0.76F + lambert * 0.24F;
        Vector3f position = cubeShellPoint(frame, local, 1.022F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(whiten, 0.0F, 1.0F),
                clamp(whiten, 0.0F, 1.0F),
                clamp(whiten, 0.0F, 1.0F),
                clamp(cloudAlpha, 0.0F, 1.0F)
        );
    }

    private static void renderEarthAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alphaScale,
            int subdivisions
    ) {
        float[] shellScales = {1.018F, 1.032F, 1.048F, 1.066F, 1.086F};
        float[] shellAlpha = {0.26F, 0.22F, 0.18F, 0.13F, 0.09F};

        for (int layer = 0; layer < shellScales.length; layer++) {
            float shellScale = shellScales[layer];
            float shellAlphaScale = alphaScale * shellAlpha[layer];
            for (CubeSide side : sortedSides(frame)) {
                float step = 2.0F / subdivisions;
                for (int y = 0; y < subdivisions; y++) {
                    float v0 = -1.0F + y * step;
                    float v1 = v0 + step;
                    for (int x = 0; x < subdivisions; x++) {
                        float u0 = -1.0F + x * step;
                        float u1 = u0 + step;

                        addEarthAtmosphereVertex(buffer, pose, frame, side, u0, v0, lightLocal, viewLocal, shellAlphaScale, shellScale);
                        addEarthAtmosphereVertex(buffer, pose, frame, side, u1, v0, lightLocal, viewLocal, shellAlphaScale, shellScale);
                        addEarthAtmosphereVertex(buffer, pose, frame, side, u1, v1, lightLocal, viewLocal, shellAlphaScale, shellScale);
                        addEarthAtmosphereVertex(buffer, pose, frame, side, u0, v1, lightLocal, viewLocal, shellAlphaScale, shellScale);
                    }
                }
            }
        }
    }

    private static void addEarthAtmosphereVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            float u,
            float v,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alphaScale,
            float shellScale
    ) {
        Vector3f local = side.local(u, v);
        Vector3f normal = side.normal();
        float viewDot = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float edge = clamp((Math.max(Math.abs(u), Math.abs(v)) - 0.35F) / 0.65F, 0.0F, 1.0F);
        float rim = (float) Math.pow(1.0F - viewDot, 2.05F) * (0.48F + edge * 0.52F);
        float sunAmount = 0.30F + clamp(normal.dot(lightLocal), 0.0F, 1.0F) * 0.70F;
        float alpha = alphaScale * rim * (0.08F + sunAmount * 0.24F);
        float red = 0.20F + sunAmount * 0.16F;
        float green = 0.50F + sunAmount * 0.22F;
        float blue = 0.98F + sunAmount * 0.10F;

        Vector3f position = cubeShellPoint(frame, local, shellScale);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(red, 0.0F, 1.0F),
                clamp(green, 0.0F, 1.0F),
                clamp(blue, 0.0F, 1.0F),
                clamp(alpha, 0.0F, 1.0F)
        );
    }

    private static void addSolidCube(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            Vector3f axis,
            Vector3f keyLightDirection,
            float radius,
            float size,
            float halfDepth,
            float red,
            float green,
            float blue,
            float alpha,
            double rotationPhaseRadians
    ) {
        CubeFrame frame = cubeFrame(direction, axis, radius, size * 0.5F, halfDepth, rotationPhaseRadians);
        Vector3f[] corners = cubeCorners(frame);

        List<CubeFace> faces = new ArrayList<>(6);
        faces.add(face(corners, 0, 1, 2, 3, new Vector3f(frame.forward()).negate(), keyLightDirection, red, green, blue, alpha));
        faces.add(face(corners, 4, 5, 6, 7, new Vector3f(frame.forward()), keyLightDirection, red, green, blue, alpha));
        faces.add(face(corners, 3, 2, 6, 7, new Vector3f(frame.up()), keyLightDirection, red, green, blue, alpha));
        faces.add(face(corners, 0, 1, 5, 4, new Vector3f(frame.up()).negate(), keyLightDirection, red, green, blue, alpha));
        faces.add(face(corners, 1, 2, 6, 5, new Vector3f(frame.right()), keyLightDirection, red, green, blue, alpha));
        faces.add(face(corners, 0, 3, 7, 4, new Vector3f(frame.right()).negate(), keyLightDirection, red, green, blue, alpha));
        faces.sort(Comparator.comparingDouble(CubeFace::depth).reversed());

        for (CubeFace face : faces) {
            buffer.addVertex(pose, face.v0().x(), face.v0().y(), face.v0().z()).setColor(face.red(), face.green(), face.blue(), face.alpha());
            buffer.addVertex(pose, face.v1().x(), face.v1().y(), face.v1().z()).setColor(face.red(), face.green(), face.blue(), face.alpha());
            buffer.addVertex(pose, face.v2().x(), face.v2().y(), face.v2().z()).setColor(face.red(), face.green(), face.blue(), face.alpha());
            buffer.addVertex(pose, face.v3().x(), face.v3().y(), face.v3().z()).setColor(face.red(), face.green(), face.blue(), face.alpha());
        }
    }

    private static CubeFace face(
            Vector3f[] corners,
            int i0,
            int i1,
            int i2,
            int i3,
            Vector3f normal,
            Vector3f keyLightDirection,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float brightness = clamp(0.22F + Math.max(0.0F, normal.normalize().dot(normalizedOrFallback(keyLightDirection, KEY_LIGHT_DIRECTION))) * 0.78F, 0.18F, 1.0F);
        Vector3f center = new Vector3f(corners[i0]).add(corners[i1]).add(corners[i2]).add(corners[i3]).mul(0.25F);
        return new CubeFace(
                corners[i0],
                corners[i1],
                corners[i2],
                corners[i3],
                red * brightness,
                green * brightness,
                blue * brightness,
                alpha,
                center.lengthSquared()
        );
    }

    private static Vector3f[] cubeCorners(CubeFrame frame) {
        Vector3f spanRight = new Vector3f(frame.right()).mul(frame.halfSpan());
        Vector3f spanUp = new Vector3f(frame.up()).mul(frame.halfSpan());
        Vector3f spanForward = new Vector3f(frame.forward()).mul(frame.halfDepth());

        return new Vector3f[]{
                new Vector3f(frame.center()).sub(spanRight).sub(spanUp).sub(spanForward),
                new Vector3f(frame.center()).add(spanRight).sub(spanUp).sub(spanForward),
                new Vector3f(frame.center()).add(spanRight).add(spanUp).sub(spanForward),
                new Vector3f(frame.center()).sub(spanRight).add(spanUp).sub(spanForward),
                new Vector3f(frame.center()).sub(spanRight).sub(spanUp).add(spanForward),
                new Vector3f(frame.center()).add(spanRight).sub(spanUp).add(spanForward),
                new Vector3f(frame.center()).add(spanRight).add(spanUp).add(spanForward),
                new Vector3f(frame.center()).sub(spanRight).add(spanUp).add(spanForward)
        };
    }

    private static CubeFrame cubeFrame(
            Vector3f direction,
            Vector3f axisHint,
            float radius,
            float halfSpan,
            float halfDepth,
            double rotationPhaseRadians
    ) {
        Vector3f centerDirection = normalizedOrFallback(new Vector3f(direction), new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f up = normalizedOrFallback(new Vector3f(axisHint), new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f reference = stableReference(up);
        Vector3f right = reference.cross(up, new Vector3f()).normalize();
        Vector3f forward = up.cross(right, new Vector3f()).normalize();

        Quaternionf spin = new Quaternionf().fromAxisAngleRad(up.x(), up.y(), up.z(), (float) rotationPhaseRadians);
        spin.transform(right);
        spin.transform(forward);

        return new CubeFrame(new Vector3f(centerDirection).mul(radius), right, up, forward, halfSpan, halfDepth);
    }

    private static Vector3f stableReference(Vector3f axis) {
        Vector3f reference = Math.abs(axis.z()) > 0.92F
                ? new Vector3f(1.0F, 0.0F, 0.0F)
                : new Vector3f(0.0F, 0.0F, 1.0F);
        if (reference.cross(axis, new Vector3f()).lengthSquared() < 1.0E-6F) {
            reference.set(0.0F, 1.0F, 0.0F);
        }
        return reference;
    }

    private static Vector3f cubePoint(CubeFrame frame, Vector3f local) {
        return new Vector3f(frame.center())
                .add(new Vector3f(frame.right()).mul(local.x() * frame.halfSpan()))
                .add(new Vector3f(frame.up()).mul(local.y() * frame.halfSpan()))
                .add(new Vector3f(frame.forward()).mul(local.z() * frame.halfDepth()));
    }

    private static Vector3f cubeShellPoint(CubeFrame frame, Vector3f local, float scale) {
        return cubePoint(frame, new Vector3f(local).mul(scale));
    }

    private static Vector3f worldToLocal(CubeFrame frame, Vector3f world) {
        return new Vector3f(world.dot(frame.right()), world.dot(frame.up()), world.dot(frame.forward()));
    }

    private static Vector3f localViewDirection(CubeFrame frame) {
        return normalizedOrFallback(worldToLocal(frame, new Vector3f(frame.center()).negate()), new Vector3f(0.0F, 0.0F, -1.0F));
    }

    private static Vector3f normalizedOrFallback(Vector3f vector, Vector3f fallback) {
        if (vector.lengthSquared() < 1.0E-6F) {
            return new Vector3f(fallback).normalize();
        }
        return vector.normalize();
    }

    private static List<CubeSide> sortedSides(CubeFrame frame) {
        List<CubeSideDepth> sides = new ArrayList<>(6);
        for (CubeSide side : CubeSide.values()) {
            Vector3f center = cubePoint(frame, side.center());
            sides.add(new CubeSideDepth(side, center.lengthSquared()));
        }
        sides.sort(Comparator.comparingDouble(CubeSideDepth::depth).reversed());

        List<CubeSide> result = new ArrayList<>(sides.size());
        for (CubeSideDepth side : sides) {
            result.add(side.side());
        }
        return result;
    }

    private static BodyStyle styleFor(String id, KspBodyKind kind) {
        if (kind != KspBodyKind.STAR && "earth".equals(id)) {
            return BodyStyle.EARTH;
        }
        if (kind != KspBodyKind.STAR && PLANET_TEXTURES.containsKey(id)) {
            return BodyStyle.TEXTURED;
        }
        return BodyStyle.SOLID;
    }

    private static Vector3f dominantStarDirection(
            SpaceSnapshotPacket.BodyData body,
            List<SpaceSnapshotPacket.BodyData> bodies
    ) {
        double strongestFlux = 0.0;
        Vector3f strongestDirection = null;
        for (SpaceSnapshotPacket.BodyData candidate : bodies) {
            if (candidate.kind() != KspBodyKind.STAR || candidate.id().equals(body.id())) {
                continue;
            }
            double dx = candidate.posX() - body.posX();
            double dy = candidate.posY() - body.posY();
            double dz = candidate.posZ() - body.posZ();
            double distanceSquared = Math.max(1.0, dx * dx + dy * dy + dz * dz);
            double flux = (candidate.radius() * candidate.radius()) / distanceSquared;
            if (flux <= strongestFlux) {
                continue;
            }
            strongestFlux = flux;
            strongestDirection = new Vec3(dx, dy, dz).normalizeF();
        }
        if (strongestDirection != null) {
            return strongestDirection;
        }
        return new Vector3f(KEY_LIGHT_DIRECTION);
    }

    private static Vec3 resolveViewer(
            SpaceSnapshotPacket snapshot,
            SpaceStateSyncPacket state,
            SpaceSceneCompositor compositor,
            Map<String, SpaceSnapshotPacket.BodyData> bodyMap
    ) {
        if (compositor.transition() != null && compositor.transition().active()) {
            SpaceSnapshotPacket.BodyData currentBody = bodyMap.get(state.bodyId());
            if (currentBody == null) {
                return null;
            }

            double altitude = SpaceSceneCompositor.altitudeAt(compositor.transition(), compositor.estimatedServerTick());
            SurfaceFrame frame = surfaceFrame(currentBody);
            double radius = currentBody.radius() + Math.max(1.0, altitude);
            return new Vec3(
                    currentBody.posX() + frame.surfaceDirection.x * radius,
                    currentBody.posY() + frame.surfaceDirection.y * radius,
                    currentBody.posZ() + frame.surfaceDirection.z * radius
            );
        }

        if (state.authorityVesselId() != null) {
            for (SpaceSnapshotPacket.VesselData vessel : snapshot.vessels()) {
                if (state.authorityVesselId().equals(vessel.id())) {
                    return new Vec3(vessel.posX(), vessel.posY(), vessel.posZ());
                }
            }
        }

        SpaceSnapshotPacket.BodyData currentBody = bodyMap.get(state.bodyId());
        if (currentBody == null) {
            return null;
        }

        double altitude = state.vesselAltitudeMeters();
        if (compositor.transition() != null && compositor.transition().active()) {
            altitude = SpaceSceneCompositor.altitudeAt(compositor.transition(), compositor.estimatedServerTick());
        }
        SurfaceFrame frame = surfaceFrame(currentBody);
        double radius = currentBody.radius() + Math.max(1.0, altitude);
        return new Vec3(
                currentBody.posX() + frame.surfaceDirection.x * radius,
                currentBody.posY() + frame.surfaceDirection.y * radius,
                currentBody.posZ() + frame.surfaceDirection.z * radius
        );
    }

    private static boolean shouldHidePrimaryBody(SpaceSceneCompositor compositor) {
        return compositor.transition() != null
                && compositor.transition().active()
                && compositor.isTakeoff()
                && !compositor.transition().cutoverApplied();
    }

    private static SurfaceFrame surfaceFrame(SpaceSnapshotPacket.BodyData body) {
        Vec3 axis = new Vec3(body.axisX(), body.axisY(), body.axisZ()).normalize();
        Vec3 reference = Math.abs(axis.x) > 0.92 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 east = axis.cross(reference).normalize();
        Vec3 north = east.cross(axis).normalize();
        double phase = body.rotationPhaseRadians();
        Vec3 surface = north.scale(Math.cos(phase)).add(east.scale(Math.sin(phase))).normalize();
        return new SurfaceFrame(surface);
    }

    private static BodyColor colorFor(String id) {
        return switch (id) {
            case "sun" -> new BodyColor(1.0F, 0.93F, 0.72F);
            case "earth" -> new BodyColor(0.39F, 0.68F, 1.0F);
            case "moon" -> new BodyColor(0.86F, 0.88F, 0.93F);
            case "mercury" -> new BodyColor(0.76F, 0.70F, 0.60F);
            case "venus" -> new BodyColor(0.88F, 0.70F, 0.50F);
            case "mars" -> new BodyColor(0.82F, 0.45F, 0.33F);
            case "jupiter" -> new BodyColor(0.79F, 0.63F, 0.48F);
            case "saturn" -> new BodyColor(0.81F, 0.76F, 0.53F);
            case "uranus" -> new BodyColor(0.55F, 0.83F, 0.83F);
            case "neptune" -> new BodyColor(0.36F, 0.49F, 0.87F);
            default -> new BodyColor(0.92F, 0.94F, 0.98F);
        };
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double computeRenderZoom(double distance, double radius) {
        double shellDistance = Math.max(distance - Math.max(radius, 1.0), 1.0);
        double compressedDistance = positionCompression(shellDistance, 65_536.0, 262_144.0);
        return compressedDistance / shellDistance;
    }

    private static Vector3f computeProxyRenderPosition(Vec3 delta, double distance, double radius, double shellDistance, double renderZoom, boolean primaryBody) {
        if (primaryBody) {
            return new Vector3f(0.0F, -1360.0F, 0.0F);
        }
        Vector3f compressed = new Vector3f((float) (delta.x * renderZoom), (float) (delta.y * renderZoom), (float) (delta.z * renderZoom));
        double compressedShellDistance = positionCompression(shellDistance, 4_096.0, 24_576.0);
        double compressedCenterDistance = Math.max(compressed.length(), Math.max(radius * renderZoom, 1.0));
        double horizonFloor = primaryBody
                ? 920.0 + Math.min(compressedShellDistance * 0.10, 240.0)
                : 1_040.0 + Math.min(compressedShellDistance * 0.08, 320.0);
        double minDistance = primaryBody ? 920.0 : 1_040.0;
        double maxDistance = primaryBody ? 1_780.0 : 3_072.0;
        double targetDistance = clamp(Math.max(compressedCenterDistance, horizonFloor), minDistance, maxDistance);

        if (compressed.lengthSquared() < 1.0E-6F) {
            compressed.set(0.0F, primaryBody ? -targetDistance : 0.0F, primaryBody ? -0.25F * (float) targetDistance : -targetDistance);
        } else if (Math.abs(targetDistance - compressedCenterDistance) > 1.0E-3) {
            compressed.mul((float) (targetDistance / compressedCenterDistance));
        }

        return compressed;
    }

    private static float computeProxyRenderSize(double distance, double radius, double renderDistance, float minimumSize, boolean primaryBody) {
        double renderZoom = renderDistance / Math.max(distance, 1.0);
        double compressedDiameter = Math.max(radius * 2.0 * renderZoom, minimumSize);
        double angularDiameter = 2.0 * Math.atan2(radius, Math.max(distance, 1.0));
        double projectedSize = 2.0 * renderDistance * Math.tan(angularDiameter * 0.5);
        double naturalSize = primaryBody
                ? projectedSize
                : Math.max(compressedDiameter, projectedSize);
        double maxSize = primaryBody ? renderDistance * 4.0 : renderDistance * 0.36;
        double minSize = primaryBody ? Math.max(minimumSize, renderDistance * 0.10) : minimumSize;
        return (float) clamp(naturalSize, minSize, maxSize);
    }

    private static double positionCompression(double x, double near, double far) {
        if (x <= near) {
            return x;
        }
        return far - (far - near) * Math.exp(-(x - near) / (far - near));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record RenderBody(
            String id,
            Vector3f renderPosition,
            Vector3f direction,
            Vector3f axis,
            Vector3f keyLightDirection,
            float renderDistance,
            float size,
            float red,
            float green,
            float blue,
            float alpha,
            double rotationPhaseRadians,
            boolean star,
            boolean primaryBody,
            BodyStyle style,
            double viewerDistance
    ) {
    }

    public enum BodyStyle {
        SOLID,
        TEXTURED,
        EARTH
    }

    private record BodyColor(float red, float green, float blue) {
    }

    private record BodyFrame(Vector3f east, Vector3f north, Vector3f prime) {
    }

    private record SurfaceFrame(Vec3 surfaceDirection) {
    }

    private record SphereFrame(
            Vector3f center,
            Vector3f right,
            Vector3f up,
            Vector3f forward,
            float radius
    ) {
    }

    private record CubeFace(
            Vector3f v0,
            Vector3f v1,
            Vector3f v2,
            Vector3f v3,
            float red,
            float green,
            float blue,
            float alpha,
            double depth
    ) {
    }

    private record CubeFrame(
            Vector3f center,
            Vector3f right,
            Vector3f up,
            Vector3f forward,
            float halfSpan,
            float halfDepth
    ) {
    }

    private record CubeSideDepth(CubeSide side, double depth) {
    }

    private record TextureSample(float red, float green, float blue, float alpha) {
        private float luminance() {
            return red * 0.2126F + green * 0.7152F + blue * 0.0722F;
        }
    }

    private enum CubeSide {
        NEAR("north", new Vector3f(0.0F, 0.0F, -1.0F)) {
            @Override
            Vector3f local(float u, float v) {
                return new Vector3f(u, v, -1.0F);
            }
        },
        FAR("south", new Vector3f(0.0F, 0.0F, 1.0F)) {
            @Override
            Vector3f local(float u, float v) {
                return new Vector3f(-u, v, 1.0F);
            }
        },
        UP("up", new Vector3f(0.0F, 1.0F, 0.0F)) {
            @Override
            Vector3f local(float u, float v) {
                return new Vector3f(u, 1.0F, v);
            }
        },
        DOWN("down", new Vector3f(0.0F, -1.0F, 0.0F)) {
            @Override
            Vector3f local(float u, float v) {
                return new Vector3f(u, -1.0F, -v);
            }
        },
        EAST("east", new Vector3f(1.0F, 0.0F, 0.0F)) {
            @Override
            Vector3f local(float u, float v) {
                return new Vector3f(1.0F, v, -u);
            }
        },
        WEST("west", new Vector3f(-1.0F, 0.0F, 0.0F)) {
            @Override
            Vector3f local(float u, float v) {
                return new Vector3f(-1.0F, v, u);
            }
        };

        private final String fileName;
        private final Vector3f normal;

        CubeSide(String fileName, Vector3f normal) {
            this.fileName = fileName;
            this.normal = normal;
        }

        abstract Vector3f local(float u, float v);

        Vector3f center() {
            return local(0.0F, 0.0F);
        }

        Vector3f normal() {
            return new Vector3f(this.normal);
        }

        float textureU(float u, float v) {
            return clamp(u * 0.5F + 0.5F, 0.0F, 1.0F);
        }

        float textureV(float u, float v) {
            return clamp(0.5F - v * 0.5F, 0.0F, 1.0F);
        }

        String fileName() {
            return fileName;
        }
    }

    private static final class PlanetTextureSet {
        private final ResourceLocation surfaceLocation;
        private final ResourceLocation nightLocation;
        private final ResourceLocation cloudLocation;
        private final EnumMap<CubeSide, ResourceLocation> surfaceFaceLocations;
        private final EnumMap<CubeSide, ResourceLocation> nightFaceLocations;
        private final EnumMap<CubeSide, ResourceLocation> cloudFaceLocations;

        private BufferedImage surface;
        private BufferedImage night;
        private BufferedImage cloud;
        private final EnumMap<CubeSide, BufferedImage> surfaceFaces = new EnumMap<>(CubeSide.class);
        private final EnumMap<CubeSide, BufferedImage> nightFaces = new EnumMap<>(CubeSide.class);
        private final EnumMap<CubeSide, BufferedImage> cloudFaces = new EnumMap<>(CubeSide.class);
        private boolean loaded;

        private PlanetTextureSet(
                ResourceLocation surfaceLocation,
                ResourceLocation nightLocation,
                ResourceLocation cloudLocation,
                EnumMap<CubeSide, ResourceLocation> surfaceFaceLocations,
                EnumMap<CubeSide, ResourceLocation> nightFaceLocations,
                EnumMap<CubeSide, ResourceLocation> cloudFaceLocations
        ) {
            this.surfaceLocation = surfaceLocation;
            this.nightLocation = nightLocation;
            this.cloudLocation = cloudLocation;
            this.surfaceFaceLocations = surfaceFaceLocations;
            this.nightFaceLocations = nightFaceLocations;
            this.cloudFaceLocations = cloudFaceLocations;
        }

        private static PlanetTextureSet forPlanet(String planetId, boolean hasNight, boolean hasClouds) {
            String basePath = "textures/celestial_body/planet/" + planetId + "/";
            return new PlanetTextureSet(
                    GregtechUniverseSpace.id(basePath + "surface.png"),
                    hasNight ? GregtechUniverseSpace.id(basePath + "surface_night.png") : null,
                    hasClouds ? GregtechUniverseSpace.id(basePath + "cloud.png") : null,
                    faceLocations(basePath, "faces"),
                    hasNight ? faceLocations(basePath, "faces_night") : new EnumMap<>(CubeSide.class),
                    hasClouds ? faceLocations(basePath, "faces_cloud") : new EnumMap<>(CubeSide.class)
            );
        }

        private static EnumMap<CubeSide, ResourceLocation> faceLocations(String basePath, String folder) {
            EnumMap<CubeSide, ResourceLocation> locations = new EnumMap<>(CubeSide.class);
            for (CubeSide side : CubeSide.values()) {
                locations.put(side, GregtechUniverseSpace.id(basePath + folder + "/" + side.fileName() + ".png"));
            }
            return locations;
        }

        private void ensureLoaded() {
            if (loaded) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getResourceManager() == null) {
                return;
            }
            loaded = true;
            surface = load(surfaceLocation);
            night = load(nightLocation);
            cloud = load(cloudLocation);
            loadFaceSet(surfaceFaces, surfaceFaceLocations);
            loadFaceSet(nightFaces, nightFaceLocations);
            loadFaceSet(cloudFaces, cloudFaceLocations);
        }

        private boolean hasSurface() {
            return surface != null || !surfaceFaces.isEmpty();
        }

        private boolean hasClouds() {
            return cloud != null || !cloudFaces.isEmpty();
        }

        private TextureSample sampleSurface(CubeSide side, float u, float v) {
            return sample(surfaceFaces.get(side), surface, u, v, new TextureSample(0.18F, 0.31F, 0.60F, 1.0F));
        }

        private TextureSample sampleSurface(float u, float v) {
            return sample(null, surface, u, v, new TextureSample(0.18F, 0.31F, 0.60F, 1.0F));
        }

        private TextureSample sampleNight(CubeSide side, float u, float v) {
            return sample(nightFaces.get(side), night, u, v, new TextureSample(0.0F, 0.0F, 0.0F, 1.0F));
        }

        private TextureSample sampleNight(float u, float v) {
            return sample(null, night, u, v, new TextureSample(0.0F, 0.0F, 0.0F, 1.0F));
        }

        private TextureSample sampleCloud(CubeSide side, float u, float v) {
            if (cloudFaces.containsKey(side)) {
                return sample(cloudFaces.get(side), null, u, v, new TextureSample(0.0F, 0.0F, 0.0F, 1.0F));
            }
            return sampleAnimatedCloud(cloud, u, v, new TextureSample(0.0F, 0.0F, 0.0F, 1.0F));
        }

        private TextureSample sampleCloud(float u, float v) {
            return sampleAnimatedCloud(cloud, u, v, new TextureSample(0.0F, 0.0F, 0.0F, 1.0F));
        }

        private static TextureSample sample(
                BufferedImage faceImage,
                BufferedImage fallbackImage,
                float u,
                float v,
                TextureSample fallback
        ) {
            BufferedImage image = faceImage != null ? faceImage : fallbackImage;
            if (image == null) {
                return fallback;
            }

            float sampleU = clamp(u, 0.0F, 1.0F);
            float sampleV = clamp(v, 0.0F, 1.0F);

            int x = clamp(Math.round(sampleU * (image.getWidth() - 1)), 0, image.getWidth() - 1);
            int y = clamp(Math.round(sampleV * (image.getHeight() - 1)), 0, image.getHeight() - 1);
            int argb = image.getRGB(x, y);

            float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
            float red = ((argb >>> 16) & 0xFF) / 255.0F;
            float green = ((argb >>> 8) & 0xFF) / 255.0F;
            float blue = (argb & 0xFF) / 255.0F;
            return new TextureSample(red, green, blue, alpha);
        }

        private static TextureSample sampleAnimatedCloud(
                BufferedImage atlas,
                float u,
                float v,
                TextureSample fallback
        ) {
            if (atlas == null) {
                return fallback;
            }
            if (atlas.getHeight() <= atlas.getWidth()) {
                return sample(null, atlas, u, v, fallback);
            }

            int frameSize = atlas.getWidth();
            int frames = Math.max(1, atlas.getHeight() / Math.max(frameSize, 1));
            if (frames <= 1) {
                return sample(null, atlas, u, v, fallback);
            }

            Minecraft minecraft = Minecraft.getInstance();
            long gameTime = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0L;
            int frameIndex = Math.floorMod((int) (gameTime / 2L), frames);
            float sampleU = clamp(u, 0.0F, 1.0F);
            float sampleV = clamp(v, 0.0F, 1.0F);
            int x = clamp(Math.round(sampleU * (atlas.getWidth() - 1)), 0, atlas.getWidth() - 1);
            int y = clamp(Math.round(sampleV * (frameSize - 1)), 0, frameSize - 1) + frameIndex * frameSize;
            int argb = atlas.getRGB(x, clamp(y, 0, atlas.getHeight() - 1));

            float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
            float red = ((argb >>> 16) & 0xFF) / 255.0F;
            float green = ((argb >>> 8) & 0xFF) / 255.0F;
            float blue = (argb & 0xFF) / 255.0F;
            return new TextureSample(red, green, blue, alpha);
        }

        private static void loadFaceSet(EnumMap<CubeSide, BufferedImage> faceImages, EnumMap<CubeSide, ResourceLocation> locations) {
            for (Map.Entry<CubeSide, ResourceLocation> entry : locations.entrySet()) {
                BufferedImage image = load(entry.getValue());
                // Ignore tiny block-face textures so the planetary renderer keeps using the higher-fidelity
                // Cosmic Horizons surface/night/cloud maps when available.
                if (image != null && image.getWidth() >= 128 && image.getHeight() >= 128) {
                    faceImages.put(entry.getKey(), image);
                }
            }
        }

        private static BufferedImage load(ResourceLocation location) {
            if (location == null) {
                return null;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return null;
            }
            try (InputStream stream = minecraft.getResourceManager().open(location)) {
                return ImageIO.read(stream);
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    private record Vec3(double x, double y, double z) {
        private double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        private Vec3 normalize() {
            double length = length();
            if (length < 1.0E-9) {
                return new Vec3(0.0, 1.0, 0.0);
            }
            return scale(1.0 / length);
        }

        private Vector3f normalizeF() {
            Vec3 normalized = normalize();
            return new Vector3f((float) normalized.x, (float) normalized.y, (float) normalized.z);
        }

        private Vec3 scale(double value) {
            return new Vec3(x * value, y * value, z * value);
        }

        private Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        private Vec3 cross(Vec3 other) {
            return new Vec3(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }
    }
}
