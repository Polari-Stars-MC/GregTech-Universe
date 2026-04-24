package org.polaris2023.gtu.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.polaris2023.gtu.space.network.SpaceSnapshotPacket;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.runtime.SpacePlayerState;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SpaceRenderRuntime implements IPhysicsProvider, IEntityProvider, RenderDataProvider {
    private static final Vector3d FALLBACK_GRAVITY = new Vector3d(0.0, -1.0, 0.0);
    private static SpaceRenderRuntime instance;

    private final SeamlessRenderManager renderer;
    private final SpaceRenderConfig config;

    private SpaceRenderRuntime(SpaceRenderConfig config) {
        this.config = config;
        this.renderer = new SeamlessRenderManager(config);
    }

    public static synchronized SpaceRenderRuntime init() {
        if (instance == null) {
            instance = new SpaceRenderRuntime(SpaceRenderConfig.load());
        }
        return instance;
    }

    public static SpaceRenderRuntime get() {
        return instance == null ? init() : instance;
    }

    public static void tickClient() {
        SpaceRenderRuntime runtime = get();
        runtime.renderer.setGravityDirection(runtime.getGravityDirection());
        runtime.renderer.update(runtime.getPlayerAltitude());
    }

    public void onPlayerAltitudeChange(double oldY, double newY) {
        renderer.update(newY);
    }

    public void onEnterSpace() {
        renderer.setRenderMode(RenderMode.SPACE);
        renderer.updateSkybox(SkyboxType.DEEP_SPACE);
        renderer.setPlanetVisibility(true);
        renderer.updateAtmosphere(0.0F);
        renderer.updateTransitionState(1.0F, 1.0F);
    }

    public void onExitSpace() {
        renderer.setRenderMode(RenderMode.GROUND);
        renderer.updateSkybox(SkyboxType.PLANET_BLUE);
        renderer.setPlanetVisibility(false);
        renderer.updateAtmosphere(1.0F);
        renderer.updateTransitionState(0.0F, 0.0F);
    }

    public void onVesselCreated(VesselRenderInfo vessel) {
    }

    public void onVesselDestroyed(VesselRenderInfo vessel) {
    }

    public SeamlessRenderManager renderer() {
        return renderer;
    }

    public SpaceRenderConfig config() {
        return config;
    }

    @Override
    public Vector3d getPlayerPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        return player == null ? new Vector3d() : new Vector3d(player.getX(), player.getY(), player.getZ());
    }

    @Override
    public double getPlayerAltitude() {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (state != null && state.mode() != SpacePlayerState.Mode.PLANET) {
            return state.vesselAltitudeMeters();
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        return player == null ? 0.0 : Math.max(0.0, player.getY());
    }

    @Override
    public boolean isInSpace(Player player) {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        return state != null && state.mode().isSpaceLike();
    }

    @Override
    public Vector3d getGravityDirection() {
        return FALLBACK_GRAVITY;
    }

    @Override
    public List<VesselRenderInfo> getNearbyVessels() {
        SpaceSnapshotPacket snapshot = ClientSpaceCache.snapshot();
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (snapshot == null || state == null) {
            return List.of();
        }

        Vector3d camera = getCameraPosition();
        List<VesselRenderInfo> result = new ArrayList<>();
        for (SpaceSnapshotPacket.VesselData vessel : snapshot.vessels()) {
            Vector3d position = new Vector3d(vessel.posX(), vessel.posY(), vessel.posZ());
            double distance = position.distance(camera);
            if (distance > config.space().maxVesselRenderDistance()) {
                continue;
            }
            result.add(new VesselRenderInfo(
                    UUID.nameUUIDFromBytes(vessel.id().getBytes(StandardCharsets.UTF_8)),
                    vessel.id(),
                    vessel.primaryBodyId(),
                    position,
                    0.0F,
                    distance,
                    vessel.id().equals(state.authorityVesselId()),
                    Minecraft.getInstance().options.getCameraType().isFirstPerson()
            ));
        }
        result.sort(Comparator.comparingDouble(VesselRenderInfo::distanceToCamera));
        return List.copyOf(result);
    }

    @Override
    public VesselRenderInfo getPlayerVessel() {
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (state == null || state.authorityVesselId() == null) {
            return null;
        }
        for (VesselRenderInfo vessel : getNearbyVessels()) {
            if (state.authorityVesselId().equals(vessel.authorityId())) {
                return vessel;
            }
        }
        return null;
    }

    @Override
    public Vector3d getVesselPosition(VesselRenderInfo vessel) {
        return vessel == null ? new Vector3d() : new Vector3d(vessel.position());
    }

    @Override
    public float getVesselRotation(VesselRenderInfo vessel) {
        return vessel == null ? 0.0F : vessel.yawDegrees();
    }

    @Override
    public Vector3d getCameraPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
            return getPlayerPosition();
        }
        net.minecraft.world.phys.Vec3 position = minecraft.gameRenderer.getMainCamera().getPosition();
        return new Vector3d(position.x, position.y, position.z);
    }

    @Override
    public Quaternionf getCameraRotation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
            return new Quaternionf();
        }
        return new Quaternionf(minecraft.gameRenderer.getMainCamera().rotation());
    }

    @Override
    public double getPlayerY() {
        Vector3d position = getPlayerPosition();
        return position.y;
    }

    @Override
    public boolean isInEVA() {
        Minecraft minecraft = Minecraft.getInstance();
        return isInSpace(minecraft.player) && !isInVessel();
    }

    @Override
    public boolean isInVessel() {
        return getPlayerVessel() != null;
    }

    @Override
    public VesselRenderInfo getCurrentVessel() {
        return getPlayerVessel();
    }

    @Override
    public List<PlanetRenderInfo> getPlanets() {
        SpaceSnapshotPacket snapshot = ClientSpaceCache.snapshot();
        SpaceStateSyncPacket state = ClientSpaceCache.state();
        if (snapshot == null || state == null) {
            return List.of();
        }

        Vector3d camera = getCameraPosition();
        List<PlanetRenderInfo> result = new ArrayList<>();
        for (SpaceSnapshotPacket.BodyData body : snapshot.bodies()) {
            if (body.kind() == org.polaris2023.gtu.space.runtime.ksp.KspBodyKind.STAR) {
                continue;
            }
            Vector3d absolutePosition = new Vector3d(body.posX(), body.posY(), body.posZ());
            double distance = absolutePosition.distance(camera);
            Vector3d relativePosition = new Vector3d(absolutePosition).sub(camera);
            result.add(new PlanetRenderInfo(
                    body.id(),
                    body.id(),
                    relativePosition,
                    new Vector3d(body.axisX(), body.axisY(), body.axisZ()),
                    body.radius(),
                    distance,
                    distance,
                    getPlayerAltitude(),
                    body.id().equals(state.bodyId()),
                    PlanetProxyRenderer.hasAtmosphere(body.id()),
                    body.id().equals(state.bodyId()) ? renderer.planetAlpha() : 1.0F,
                    body.rotationPhaseRadians()
            ));
        }
        result.sort(Comparator.comparingDouble(PlanetRenderInfo::distanceToCamera));
        return List.copyOf(result);
    }

    @Override
    public SunRenderInfo getSun() {
        SpaceSnapshotPacket snapshot = ClientSpaceCache.snapshot();
        if (snapshot == null) {
            return null;
        }
        for (SpaceSnapshotPacket.BodyData body : snapshot.bodies()) {
            if (body.kind() == org.polaris2023.gtu.space.runtime.ksp.KspBodyKind.STAR) {
                return new SunRenderInfo(
                        body.id(),
                        new Vector3d(body.posX(), body.posY(), body.posZ()),
                        renderer.starIntensity(),
                        1.0F,
                        0.93F,
                        0.72F
                );
            }
        }
        return null;
    }

    public Vector3f atmosphereColor() {
        float[] color = config.planet().atmosphereColor();
        return new Vector3f(color[0], color[1], color[2]);
    }
}
