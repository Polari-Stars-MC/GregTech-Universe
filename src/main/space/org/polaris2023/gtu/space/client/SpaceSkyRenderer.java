package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.client.portal.PortalRenderer;
import org.polaris2023.gtu.space.client.render.CosmicShapeBuffers;
import org.polaris2023.gtu.space.client.render.CosmicShapeRenderer;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.polaris2023.gtu.space.network.SpaceSnapshotPacket;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.simulation.PlanetDomainDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class SpaceSkyRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceSkyRenderer.class);
    private static final String PRIMARY_BODY_ID = "earth";
    private static final float SKY_RADIUS = 2048.0F;
    private static final float NEAR_BODY_RADIUS = 1160.0F;
    private static final float FAR_BODY_RADIUS = 1480.0F;
    private static final float DEEP_BODY_RADIUS = 1780.0F;
    private static final double PRIMARY_REAL_RADIUS_METERS = 6_371_000.0;
    private static final double PRIMARY_RENDER_DISTANCE_LOW = 780.0;
    private static final double PRIMARY_RENDER_DISTANCE_HIGH = 1320.0;
    private static final float STAR_RADIUS = 1810.0F;
    private static final float SUN_RADIUS = 1700.0F;

    private static final StarfieldRenderer STARFIELD = new StarfieldRenderer(900, 0x475455370BL);
    private static final VesselRenderer VESSELS = new VesselRenderer();
    private static final SunRenderer SUN = new SunRenderer();
    private static long nextPlanetDiagNanos;

    private SpaceSkyRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        SpaceSceneCompositor compositor = SpaceSceneCompositor.resolve(minecraft);
        SpaceRenderRuntime runtime = SpaceRenderRuntime.get();
        CubePlanetRenderer planets = new CubePlanetRenderer(runtime.config());

        if (!shouldRenderSpaceScene(minecraft, compositor)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(camera.rotation()).conjugate());
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.PI));

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        renderSkyShell(poseStack, runtime.renderer());
        renderStars(poseStack, runtime);
        renderSun(poseStack, runtime);
        renderPlanets(poseStack, event.getProjectionMatrix(), runtime, compositor, planets);
        renderVessels(poseStack, runtime);
        renderLandingGroundProxy(poseStack, compositor);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        poseStack.popPose();

        PortalRenderer.renderPortals(poseStack, camera, event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    private static void renderSkyShell(PoseStack poseStack, SeamlessRenderManager renderer) {
        CosmicShapeRenderer.renderShape(
                CosmicShapeBuffers.solidCube(),
                poseStack,
                new org.joml.Matrix4f(RenderSystem.getProjectionMatrix()),
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                SKY_RADIUS,
                SKY_RADIUS,
                SKY_RADIUS,
                0xFF000000
        );
    }

    private static void renderStars(PoseStack poseStack, SpaceRenderRuntime runtime) {
    }

    private static void renderSun(PoseStack poseStack, SpaceRenderRuntime runtime) {
    }

    private static void renderPlanets(
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            SpaceRenderRuntime runtime,
            SpaceSceneCompositor compositor,
            CubePlanetRenderer planets
    ) {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (state == null) {
            return;
        }
        boolean spaceLike = state.mode().isSpaceLike();

        PlanetRenderInfo primaryPlanet = resolvePrimaryPlanetInfo(runtime);
        List<PlanetRenderInfo> secondaryPlanets = resolveSecondaryPlanetInfos(runtime, compositor);
        if (primaryPlanet == null && secondaryPlanets.isEmpty()) {
            return;
        }

        logPlanetTrace(spaceLike, runtime, primaryPlanet, secondaryPlanets);

        float sdAlpha = spaceLike ? 1.0F : Math.max(0.35F, compositor.sdSource().alpha());
        renderPlanetLayer(poseStack, projectionMatrix, runtime, planets, DEEP_BODY_RADIUS, secondaryPlanets, 500_000.0, Double.POSITIVE_INFINITY, 0.94F * sdAlpha);
        renderPlanetLayer(poseStack, projectionMatrix, runtime, planets, FAR_BODY_RADIUS, secondaryPlanets, 50_000.0, 500_000.0, 0.98F * sdAlpha);
        renderPlanetLayer(poseStack, projectionMatrix, runtime, planets, NEAR_BODY_RADIUS, secondaryPlanets, 0.0, 50_000.0, spaceLike ? 1.0F : Math.max(sdAlpha, runtime.renderer().planetAlpha()));
        if (primaryPlanet != null) {
            float primaryAlpha = spaceLike ? 1.0F : Math.max(sdAlpha, runtime.renderer().planetAlpha());
            planets.renderCosmic(poseStack, projectionMatrix, withScaledAlpha(primaryPlanet, primaryAlpha), runtime, NEAR_BODY_RADIUS);
        }
    }

    private static void renderPlanetLayer(
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            SpaceRenderRuntime runtime,
            CubePlanetRenderer planetsRenderer,
            float renderRadius,
            List<PlanetRenderInfo> planets,
            double minimumDistance,
            double maximumDistance,
            float alphaScale
    ) {
        if (alphaScale <= 0.001F || planets.isEmpty()) {
            return;
        }

        for (PlanetRenderInfo planet : planets) {
            if (planet.realDistanceToCamera() < minimumDistance || planet.realDistanceToCamera() >= maximumDistance) {
                continue;
            }

            planetsRenderer.renderCosmic(poseStack, projectionMatrix, new PlanetRenderInfo(
                    planet.id(),
                    planet.displayName(),
                    planet.worldPosition(),
                    planet.rotationAxis(),
                    planet.radius(),
                    planet.distanceToCamera(),
                    planet.realDistanceToCamera(),
                    planet.viewerAltitude(),
                    planet.primary(),
                    planet.atmosphere(),
                    planet.alpha() * alphaScale,
                    planet.rotationPhaseRadians()
            ), runtime, renderRadius);
        }
    }

    private static List<PlanetRenderInfo> resolveSecondaryPlanetInfos(SpaceRenderRuntime runtime, SpaceSceneCompositor compositor) {
        SpaceSnapshotPacket snapshot = ClientSpaceCache.snapshot();
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (snapshot == null || state == null) {
            return runtime.getPlanets().stream().filter(planet -> !planet.primary()).toList();
        }

        List<PlanetProxyRenderer.RenderBody> renderBodies = PlanetProxyRenderer.collectBodies(
                snapshot,
                state,
                compositor,
                NEAR_BODY_RADIUS,
                0.0,
                Double.POSITIVE_INFINITY,
                6.0F
        );
        if (renderBodies.isEmpty()) {
            return runtime.getPlanets();
        }

        return renderBodies.stream()
                .filter(body -> !body.primaryBody())
                .filter(body -> !body.star())
                .map(body -> {
                    return new PlanetRenderInfo(
                            body.id(),
                            body.id(),
                            new Vector3d(
                                    body.renderPosition().x(),
                                    body.renderPosition().y(),
                                    body.renderPosition().z()
                            ),
                            new Vector3d(body.axis().x(), body.axis().y(), body.axis().z()),
                            Math.max(body.size() * 0.5F, 1.0),
                            body.renderDistance(),
                            body.viewerDistance(),
                            runtime.getPlayerAltitude(),
                            false,
                            PlanetProxyRenderer.hasAtmosphere(body.id()),
                            body.alpha(),
                            body.rotationPhaseRadians()
                    );
                })
                .toList();
    }

    private static PlanetRenderInfo resolvePrimaryPlanetInfo(SpaceRenderRuntime runtime) {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (state == null) {
            return null;
        }

        SpaceSnapshotPacket snapshot = ClientSpaceCache.snapshot();
        SpaceSnapshotPacket.BodyData primaryBody = null;
        if (snapshot != null) {
            for (SpaceSnapshotPacket.BodyData body : snapshot.bodies()) {
                if (PRIMARY_BODY_ID.equals(body.id())) {
                    primaryBody = body;
                    break;
                }
            }
        }

        double viewerAltitude = Math.max(runtime.getPlayerAltitude(), 0.0);
        double renderDistance = PRIMARY_RENDER_DISTANCE_HIGH;
        double radius = primaryBody == null ? PRIMARY_REAL_RADIUS_METERS : primaryBody.radius();
        double rotationPhase = resolvePrimaryRotationPhase(state, radius);
        Vector3d rotationAxis = new Vector3d(0.0, 1.0, 0.0);
        if (primaryBody != null) {
            rotationAxis = new Vector3d(primaryBody.axisX(), primaryBody.axisY(), primaryBody.axisZ());
        }
        Vector3d worldPosition = resolvePrimaryWorldPosition(state, renderDistance, radius);

        return new PlanetRenderInfo(
                PRIMARY_BODY_ID,
                "earth",
                worldPosition,
                rotationAxis,
                radius,
                renderDistance,
                radius + viewerAltitude,
                viewerAltitude,
                true,
                true,
                1.0F,
                rotationPhase
        );
    }

    private static Vector3d resolvePrimaryWorldPosition(SpaceStateSyncPacket state, double renderDistance, double radius) {
        Vector3d anchoredSurfaceNormal = resolveAnchoredSurfaceNormal(state, radius);
        if (anchoredSurfaceNormal.lengthSquared() < 1.0E-6) {
            return new Vector3d(0.0, -renderDistance, 0.0);
        }
        return anchoredSurfaceNormal.mul(-renderDistance, new Vector3d());
    }

    private static double resolvePrimaryRotationPhase(SpaceStateSyncPacket state, double radius) {
        if (radius <= 1.0E-6) {
            return 0.0;
        }
        return normalizeRadians(state.stableX() / radius);
    }

    private static Vector3d resolveAnchoredSurfaceNormal(SpaceStateSyncPacket state, double radius) {
        if (radius <= 1.0E-6) {
            return new Vector3d(0.0, 1.0, 0.0);
        }

        double longitude = state.stableX() / radius;
        double latitude = clamp(-state.stableZ() / radius, -Math.PI * 0.48, Math.PI * 0.48);
        double cosLatitude = Math.cos(latitude);
        double sinLatitude = Math.sin(latitude);
        double cosLongitude = Math.cos(longitude);
        double sinLongitude = Math.sin(longitude);
        return new Vector3d(
                sinLongitude * cosLatitude,
                cosLongitude * cosLatitude,
                sinLatitude
        ).normalize();
    }

    private static void renderVessels(PoseStack poseStack, SpaceRenderRuntime runtime) {
        List<VesselRenderInfo> vessels = runtime.getNearbyVessels();
        if (vessels.isEmpty()) {
            return;
        }
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        VESSELS.render(buffer, poseStack.last(), vessels, runtime.config());
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static boolean shouldRenderSpaceScene(Minecraft minecraft, SpaceSceneCompositor compositor) {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        return compositor.active() || (state != null && state.mode().isSpaceLike() && minecraft.level != null);
    }

    private static void renderLandingGroundProxy(PoseStack poseStack, SpaceSceneCompositor compositor) {
        float alpha = PlanetProxyRenderer.landingGroundAlpha(compositor);
        if (alpha <= 0.001F) {
            return;
        }
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        PoseStack.Pose pose = poseStack.last();
        float radius = 900.0F;
        float y = -620.0F;
        buffer.addVertex(pose, -radius, y, -radius).setColor(0.03F, 0.04F, 0.03F, alpha * 0.34F);
        buffer.addVertex(pose, radius, y, -radius).setColor(0.03F, 0.04F, 0.03F, alpha * 0.34F);
        buffer.addVertex(pose, radius, y, radius).setColor(0.06F, 0.07F, 0.06F, alpha * 0.22F);
        buffer.addVertex(pose, -radius, y, radius).setColor(0.06F, 0.07F, 0.06F, alpha * 0.22F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void logPlanetTrace(
            boolean spaceLike,
            SpaceRenderRuntime runtime,
            PlanetRenderInfo primaryPlanet,
            List<PlanetRenderInfo> planetInfos
    ) {
        long now = System.nanoTime();
        if (now < nextPlanetDiagNanos) {
            return;
        }
        nextPlanetDiagNanos = now + 2_000_000_000L;

        PlanetRenderInfo primary = primaryPlanet != null
                ? primaryPlanet
                : planetInfos.stream().filter(PlanetRenderInfo::primary).findFirst().orElse(planetInfos.getFirst());
        LOGGER.info(
                "Space planet trace: mode={} visible={} alpha={} planets={} primary={} renderDistance={} realDistance={} position=({}, {}, {})",
                spaceLike ? "SPACE" : "TRANSITION",
                runtime.renderer().planetVisible(),
                runtime.renderer().planetAlpha(),
                planetInfos.size() + (primaryPlanet == null ? 0 : 1),
                primary.id(),
                primary.distanceToCamera(),
                primary.realDistanceToCamera(),
                primary.worldPosition().x(),
                primary.worldPosition().y(),
                primary.worldPosition().z()
        );
    }

    private static PlanetRenderInfo withScaledAlpha(PlanetRenderInfo planet, float alphaScale) {
        return new PlanetRenderInfo(
                planet.id(),
                planet.displayName(),
                planet.worldPosition(),
                planet.rotationAxis(),
                planet.radius(),
                planet.distanceToCamera(),
                planet.realDistanceToCamera(),
                planet.viewerAltitude(),
                planet.primary(),
                planet.atmosphere(),
                planet.alpha() * alphaScale,
                planet.rotationPhaseRadians()
        );
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double normalizeRadians(double value) {
        double tau = Math.PI * 2.0;
        double normalized = value % tau;
        return normalized < 0.0 ? normalized + tau : normalized;
    }

    private static void addCubeFace(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            float radius,
            AxisFace face,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float r = radius;
        switch (face) {
            case UP -> {
                buffer.addVertex(pose, -r, r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, r, -r).setColor(red, green, blue, alpha);
            }
            case DOWN -> {
                buffer.addVertex(pose, -r, -r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, -r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, -r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, -r, r).setColor(red, green, blue, alpha);
            }
            case NORTH -> {
                buffer.addVertex(pose, -r, -r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, -r, -r).setColor(red, green, blue, alpha);
            }
            case SOUTH -> {
                buffer.addVertex(pose, -r, -r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, -r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, r, r).setColor(red, green, blue, alpha);
            }
            case EAST -> {
                buffer.addVertex(pose, r, -r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, r, -r, r).setColor(red, green, blue, alpha);
            }
            case WEST -> {
                buffer.addVertex(pose, -r, -r, -r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, -r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, r, r).setColor(red, green, blue, alpha);
                buffer.addVertex(pose, -r, r, -r).setColor(red, green, blue, alpha);
            }
        }
    }

    private enum AxisFace {
        UP,
        DOWN,
        NORTH,
        SOUTH,
        EAST,
        WEST
    }
}
