package org.polaris2023.gtu.space.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.polaris2023.gtu.space.network.SpaceSeamlessTeleportPacket;
import org.polaris2023.gtu.space.network.SpacePortalSyncPacket;
import org.polaris2023.gtu.space.network.SpaceSnapshotPacket;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.network.SpaceTransitionPacket;
import org.polaris2023.gtu.space.portal.Portal;
import org.polaris2023.gtu.space.portal.PortalOrientation;
import org.polaris2023.gtu.space.portal.PortalType;
import org.polaris2023.gtu.space.portal.impl.CubeCoordinateMapper;
import org.polaris2023.gtu.space.portal.impl.SpacePortalManager;
import org.polaris2023.gtu.space.portal.impl.SpaceSyncManager;
import org.polaris2023.gtu.space.portal.impl.SpaceTransitionManager;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyKind;
import org.polaris2023.gtu.space.runtime.ksp.KspBackgroundSystem;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyState;
import org.polaris2023.gtu.space.runtime.ksp.KspSaveData;
import org.polaris2023.gtu.space.runtime.ksp.KspSnapshot;
import org.polaris2023.gtu.space.runtime.ksp.KspSystemDefinition;
import org.polaris2023.gtu.space.runtime.ksp.KspVesselState;
import org.polaris2023.gtu.space.runtime.math.SpaceCoordinate;
import org.polaris2023.gtu.space.runtime.math.SpaceVector;
import org.polaris2023.gtu.space.runtime.math.UniverseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceManager.class);
    private static final LevelResource KSP_RESOURCE = new LevelResource("gtu_ksp");
    private static final String SAVE_FILE = "ksp_state.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SD_STARTUP_CLEAR_CHUNK_RADIUS = 2;
    private static final int SD_RUNTIME_CLEAR_CHUNK_RADIUS = 1;
    private static final int SD_RUNTIME_CLEAR_INTERVAL_TICKS = 20;
    private static final long SEAMLESS_TELEPORT_TTL_TICKS = 40L;
    private static final int LANDING_HORIZONTAL_OFFSET_BLOCKS = 768;
    private static final int LANDING_SEARCH_RADIUS_BLOCKS = 128;

    private static SpaceManager INSTANCES = null;

    private final MinecraftServer server;
    private final SpacePhysicsEngine physicsEngine = new SpacePhysicsEngine();
    private final KspBackgroundSystem kspSystem = new KspBackgroundSystem(KspSystemDefinition.solarSystem());
    private final Map<UUID, SpacePlayerState> playerStates = new ConcurrentHashMap<>();
    private final Map<UUID, SpaceVesselState> vesselStates = new ConcurrentHashMap<>();
    private final Map<UUID, SpaceTransitionState> transitionStates = new ConcurrentHashMap<>();
    private final Map<String, UUID> slotAssignments = new ConcurrentHashMap<>();
    private final SpacePortalManager portalManager = new SpacePortalManager();
    private final CubeCoordinateMapper coordinateMapper = new CubeCoordinateMapper();
    private SpaceTransitionManager transitionManager;
    private SpaceSyncManager syncManager;

    private long serverTick;
    private int syncTick;

    private SpaceManager(MinecraftServer server) {
        this.server = server;
        this.transitionManager = new SpaceTransitionManager(portalManager);
        this.syncManager = new SpaceSyncManager(server);
    }

    public static SpaceManager get(MinecraftServer server) {
        if (INSTANCES == null) {
            INSTANCES = new SpaceManager(server);
        }
        return INSTANCES;
    }

    public static void shutdown(MinecraftServer server) {
        if (INSTANCES != null) {
            INSTANCES.close();
        }
        INSTANCES = null;
    }

    public void tick() {
        serverTick++;
        transitionManager.setServerTick(serverTick);
        captureAltitudeTransitions();
        physicsEngine.stepSimulation(1.0f / 20.0f);
        kspSystem.tick();
        updateEarthDayTime();
        refreshSpaceVesselsFromSnapshot();
        applyManualLandingDescent();
        captureApproachTransitions();
        processTransitions();
        rebindOnlinePlayers();
        if (serverTick % SD_RUNTIME_CLEAR_INTERVAL_TICKS == 0L) {
            scrubSdPlayers();
        }
        bridgePhysicsState();
        cleanupOrphanVessels();
        portalManager.tick();
        syncTick++;
        if (syncTick >= 5) {
            syncTick = 0;
            syncPlayers();
        }
    }

    private void captureAltitudeTransitions() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpacePlayerState state = getOrCreate(player);
            if (state.mode() != SpacePlayerState.Mode.PLANET) {
                continue;
            }
            if (!PlanetDomainDefinition.hasDefinition(state.bodyId())) {
                continue;
            }
            PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(state.bodyId());
            String planetDimension = definition.planetDimension();
            if (!player.level().dimension().location().toString().equals(planetDimension)) {
                continue;
            }

            double altitude = Math.max(0.0, player.getY());
            if (altitude < definition.takeoffPrewarmAltitudeMeters()) {
                captureStablePlanetState(player);
                continue;
            }
            if (altitude >= definition.sdFadeCompleteAltitudeMeters()) {
                enterSpace(player, altitude);
                continue;
            }
            if (altitude >= definition.takeoffPrewarmAltitudeMeters()) {
                beginTakeoff(player, altitude);
            }
        }
    }

    public long serverTick() {
        return serverTick;
    }

    public KspSnapshot latestKspSnapshot() {
        return kspSystem.latestSnapshot();
    }

    public void applyBodyThrust(String bodyId, SpaceVector thrustAcceleration) {
        kspSystem.applyBodyThrust(bodyId, thrustAcceleration);
    }

    public void clearBodyThrust(String bodyId) {
        kspSystem.clearBodyThrust(bodyId);
    }

    public java.util.Set<String> availableBodyIds() {
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return java.util.Set.of("earth");
        }
        return new java.util.TreeSet<>(snapshot.bodies().keySet());
    }

    public SpacePlayerState debugFocusBody(ServerPlayer player, String bodyId) {
        if (bodyId == null || bodyId.isBlank()) {
            throw new IllegalStateException("Body id cannot be empty.");
        }
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null || !snapshot.bodies().containsKey(bodyId)) {
            throw new IllegalStateException("Unknown body id: " + bodyId);
        }

        SpacePlayerState state = getOrCreate(player);
        if (state.mode() != SpacePlayerState.Mode.SPACE) {
            state = enterSpace(player);
        }

        SpaceVesselState vessel = state.vesselId() == null ? null : vesselStates.get(state.vesselId());
        if (vessel == null) {
            vessel = getOrCreateVessel(player, bodyId);
        }
        vessel.setBodyId(bodyId);
        vessel.setLandingBodyId(bodyId);
        upsertAuthorityVessel(vessel, Math.max(vessel.altitudeMeters(), PlanetDomainDefinition.earth().sdFadeCompleteAltitudeMeters()));
        synchronizePlayerStatesToVessel(vessel, bodyId);
        SpacePlayerState next = playerStates.get(player.getUUID());
        return next == null ? state : next;
    }

    public SpacePlayerState getOrCreate(ServerPlayer player) {
        return playerStates.computeIfAbsent(player.getUUID(), ignored -> createPlanetState(player));
    }

    public SpacePlayerState playerState(UUID playerId) {
        return playerStates.get(playerId);
    }

    public SpaceVesselState vesselState(UUID vesselId) {
        return vesselStates.get(vesselId);
    }

    public SpaceTransitionState transitionState(UUID transitionId) {
        return transitionStates.get(transitionId);
    }

    public SpacePlayerState enterSpace(ServerPlayer player) {
        SpacePlayerState current = getOrCreate(player);
        return enterSpace(player, PlanetDomainDefinition.forBody(current.bodyId()).sdFadeCompleteAltitudeMeters());
    }

    public SpacePlayerState enterSpace(ServerPlayer player, double altitudeMeters) {
        SpacePlayerState current = getOrCreate(player);
        if (current.mode() == SpacePlayerState.Mode.SPACE) {
            return current;
        }

        PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(current.bodyId());
        captureStablePlanetState(player);
        current = getOrCreate(player);
        SpaceVesselState vessel = getOrCreateVessel(player, current.bodyId());
        double resolvedAltitude = Math.max(definition.sdFadeCompleteAltitudeMeters(), altitudeMeters);
        upsertAuthorityVessel(vessel, resolvedAltitude);
        vessel.setAltitudeMeters(resolvedAltitude);
        vessel.setLandingDescentActive(false);

        ServerLevel targetLevel = requireSdLevel(vessel.sdSlotDimension());
        ensureShellLevel(targetLevel);
        teleportToSdShell(player, targetLevel, vessel, null);

        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.SPACE,
                current.bodyId(),
                current.planetDimension(),
                current.stablePlanetDimension(),
                current.stableX(),
                current.stableY(),
                current.stableZ(),
                current.stableYRot(),
                current.stableXRot(),
                vessel.sdSlotDimension(),
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                vessel.vesselId(),
                null
        );
        playerStates.put(player.getUUID(), next);
        return next;
    }

    public SpacePlayerState beginTakeoff(ServerPlayer player) {
        return beginTakeoff(player, 0.0);
    }

    public SpacePlayerState beginTakeoff(ServerPlayer player, double startAltitudeMeters) {
        SpacePlayerState current = getOrCreate(player);
        if (current.mode() != SpacePlayerState.Mode.PLANET) {
            throw new IllegalStateException("Player is not in a planet-surface state.");
        }

        PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(current.bodyId());
        double startAltitude = Math.max(0.0, startAltitudeMeters);
        if (startAltitude >= definition.sdFadeCompleteAltitudeMeters()) {
            return enterSpace(player, startAltitude);
        }

        captureStablePlanetState(player);
        current = getOrCreate(player);
        SpaceVesselState vessel = getOrCreateVessel(player, definition.bodyId());
        vessel.setLandingDescentActive(false);
        UUID transitionId = UUID.randomUUID();
        long cutoverTick = serverTick + Math.max(0L, Math.round(
                (definition.sdCutoverAltitudeMeters() - startAltitude) / definition.takeoffMetersPerTick()
        ));
        long completeTick = serverTick + Math.max(0L, Math.round(
                (definition.sdFadeCompleteAltitudeMeters() - startAltitude) / definition.takeoffMetersPerTick()
        ));
        SpaceTransitionState transition = new SpaceTransitionState(
                transitionId,
                SpaceTransitionDirection.TAKEOFF,
                SpaceDomain.PD,
                SpaceDomain.SD,
                definition.bodyId(),
                current.planetDimension(),
                vessel.sdSlotDimension(),
                serverTick,
                cutoverTick,
                completeTick,
                startAltitude,
                definition.sdCutoverAltitudeMeters(),
                definition.sdFadeCompleteAltitudeMeters(),
                startAltitude >= definition.sdCutoverAltitudeMeters()
        );
        transitionStates.put(transitionId, transition);
        createTakeoffPortal(player, definition, current.planetDimension(), vessel.sdSlotDimension());

        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.TAKEOFF_TRANSITION,
                definition.bodyId(),
                current.planetDimension(),
                current.stablePlanetDimension(),
                current.stableX(),
                current.stableY(),
                current.stableZ(),
                current.stableYRot(),
                current.stableXRot(),
                vessel.sdSlotDimension(),
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                vessel.vesselId(),
                transitionId
        );
        playerStates.put(player.getUUID(), next);
        vessel.setAltitudeMeters(startAltitude);
        if (startAltitude >= definition.sdCutoverAltitudeMeters()) {
            ServerLevel targetLevel = requireSdLevel(vessel.sdSlotDimension());
            ensureShellLevel(targetLevel);
            teleportToSdShell(player, targetLevel, vessel, transitionId);
            upsertAuthorityVessel(vessel, startAltitude);
        }
        return next;
    }

    public SpacePlayerState beginLanding(ServerPlayer player) {
        SpacePlayerState current = getOrCreate(player);
        if (current.mode() != SpacePlayerState.Mode.SPACE) {
            throw new IllegalStateException("Player must already be in SD/SPACE to begin landing.");
        }
        if (current.vesselId() == null) {
            throw new IllegalStateException("Player is not attached to an active vessel session.");
        }

        SpaceVesselState vessel = requireVessel(current.vesselId());
        String landingBodyId = vessel.landingBodyId() != null ? vessel.landingBodyId() : vessel.bodyId();
        if (!PlanetDomainDefinition.hasDefinition(landingBodyId)) {
            throw new IllegalStateException("No seamless landing definition is available for body " + landingBodyId + ".");
        }
        PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(landingBodyId);
        LandingTarget landingTarget = resolveDynamicLandingTarget(player, current, vessel, definition);
        double startAltitude = Math.max(vessel.altitudeMeters(), 1.0);
        long duration = Math.max(1L, Math.round(startAltitude / definition.landingMetersPerTick()));
        UUID transitionId = UUID.randomUUID();
        SpaceTransitionState transition = new SpaceTransitionState(
                transitionId,
                SpaceTransitionDirection.LANDING,
                SpaceDomain.SD,
                SpaceDomain.PD,
                landingBodyId,
                current.sdSlotDimension(),
                definition.planetDimension(),
                serverTick,
                serverTick + duration,
                serverTick + duration,
                startAltitude,
                0.0,
                0.0,
                false
        );
        transitionStates.put(transitionId, transition);

        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.LANDING_TRANSITION,
                landingBodyId,
                definition.planetDimension(),
                landingTarget.dimension(),
                landingTarget.x(),
                landingTarget.y(),
                landingTarget.z(),
                landingTarget.yRot(),
                landingTarget.xRot(),
                vessel.sdSlotDimension(),
                current.universeId(),
                current.galaxyId(),
                current.systemId(),
                current.universeAnchorPosition(),
                current.coordinate(),
                current.vesselId(),
                transitionId
        );
        playerStates.put(player.getUUID(), next);
        vessel.setBodyId(landingBodyId);
        vessel.setLandingBodyId(landingBodyId);
        vessel.setAltitudeMeters(startAltitude);
        vessel.setLandingDescentActive(false);
        return next;
    }

    public void setLandingDescent(ServerPlayer player, boolean active) {
        SpacePlayerState state = getOrCreate(player);
        if (state.mode() != SpacePlayerState.Mode.SPACE || state.vesselId() == null || state.transitionId() != null) {
            return;
        }
        SpaceVesselState vessel = vesselStates.get(state.vesselId());
        if (vessel == null) {
            return;
        }
        vessel.setLandingDescentActive(active);
    }

    public SpacePlayerState leaveSpace(ServerPlayer player) {
        SpacePlayerState current = getOrCreate(player);
        if (current.vesselId() != null) {
            SpaceVesselState vessel = vesselStates.get(current.vesselId());
            if (vessel != null) {
                vessel.setLandingDescentActive(false);
            }
        }
        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.PLANET,
                current.bodyId(),
                current.stablePlanetDimension(),
                current.stablePlanetDimension(),
                current.stableX(),
                current.stableY(),
                current.stableZ(),
                current.stableYRot(),
                current.stableXRot(),
                null,
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                null,
                null
        );
        playerStates.put(player.getUUID(), next);
        cleanupPlayerVessel(current);

        ServerLevel target = requireLevel(current.stablePlanetDimension());
        teleportPlayerSeamlessly(player, target, current.stableX(), current.stableY(), current.stableZ(), current.stableYRot(), current.stableXRot(), null);
        return next;
    }

    public String statusSummary(ServerPlayer player) {
        SpacePlayerState state = getOrCreate(player);
        String transition = state.transitionId() == null ? "-" : state.transitionId().toString();
        String vessel = state.vesselId() == null ? "-" : state.vesselId().toString();
        return "mode=" + state.mode()
                + ", body=" + state.bodyId()
                + ", pd=" + state.planetDimension()
                + ", stablePd=" + state.stablePlanetDimension()
                + ", sd=" + state.sdSlotDimension()
                + ", vessel=" + vessel
                + ", transition=" + transition;
    }

    public void syncPlayer(ServerPlayer player) {
        SpacePlayerState state = getOrCreate(player);
        PacketDistributor.sendToPlayer(player, buildStatePacket(state));
        SpaceTransitionState transition = state.transitionId() == null ? null : transitionStates.get(state.transitionId());
        PacketDistributor.sendToPlayer(player, buildTransitionPacket(transition));
        if (state.mode().isSpaceLike() || state.isTransitioning()) {
            PacketDistributor.sendToPlayer(player, buildSnapshotPacket());
        }
    }

    public void save() {
        try {
            Path dir = server.getWorldPath(KSP_RESOURCE);
            Files.createDirectories(dir);
            Path file = dir.resolve(SAVE_FILE);
            KspSaveData data = kspSystem.exportState();
            data.playerStates = exportPlayerStates();
            data.spaceVessels = exportSpaceVessels();
            data.spaceTransitions = exportSpaceTransitions();
            data.spaceServerTick = serverTick;
            Files.writeString(file, GSON.toJson(data));
            LOGGER.info("KSP/space state saved to {}", file);
        } catch (IOException e) {
            LOGGER.error("Failed to save KSP state", e);
        }
    }

    public void load() {
        resetSdSlotStorage();
        Path file = server.getWorldPath(KSP_RESOURCE).resolve(SAVE_FILE);
        if (!Files.exists(file)) {
            LOGGER.info("No KSP save file found, starting fresh");
            return;
        }
        try {
            KspSaveData data = GSON.fromJson(Files.readString(file), KspSaveData.class);
            kspSystem.importState(data);
            importPlayerStates(data.playerStates);
            importSpaceVessels(data.spaceVessels);
            importSpaceTransitions(data.spaceTransitions);
            serverTick = Math.max(serverTick, data.spaceServerTick);
            LOGGER.info("KSP/space state loaded from {}", file);
        } catch (IOException e) {
            LOGGER.error("Failed to load KSP state", e);
        }
    }

    private SpacePlayerState createPlanetState(ServerPlayer player) {
        return new SpacePlayerState(
                SpacePlayerState.Mode.PLANET,
                "earth",
                player.level().dimension().location().toString(),
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                null,
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                null,
                null
        );
    }

    private void captureStablePlanetState(ServerPlayer player) {
        SpacePlayerState current = getOrCreate(player);
        PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(current.bodyId());
        boolean preserveExistingStable = current.stablePlanetDimension() != null
                && current.stablePlanetDimension().equals(player.level().dimension().location().toString())
                && player.getY() > definition.takeoffPrewarmAltitudeMeters();
        SpacePlayerState next = new SpacePlayerState(
                current.mode(),
                current.bodyId(),
                player.level().dimension().location().toString(),
                preserveExistingStable ? current.stablePlanetDimension() : player.level().dimension().location().toString(),
                preserveExistingStable ? current.stableX() : player.getX(),
                preserveExistingStable ? current.stableY() : player.getY(),
                preserveExistingStable ? current.stableZ() : player.getZ(),
                preserveExistingStable ? current.stableYRot() : player.getYRot(),
                preserveExistingStable ? current.stableXRot() : player.getXRot(),
                current.sdSlotDimension(),
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                current.coordinate(),
                current.vesselId(),
                current.transitionId()
        );
        playerStates.put(player.getUUID(), next);
    }

    private SpaceVesselState getOrCreateVessel(ServerPlayer player, String bodyId) {
        SpacePlayerState current = getOrCreate(player);
        if (current.vesselId() != null) {
            SpaceVesselState existing = vesselStates.get(current.vesselId());
            if (existing != null) {
                existing.addCrew(player.getUUID());
                return existing;
            }
        }

        UUID vesselId = UUID.randomUUID();
        ResourceKey<Level> slot = allocateSdSlot(vesselId);
        SpaceVesselState vessel = new SpaceVesselState(
                vesselId,
                "session_" + vesselId,
                bodyId,
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                slot.location().toString(),
                SpaceCoordinate.origin(),
                0.5,
                SpaceDimensions.SD_SHELL_Y + 1.0,
                0.5,
                0.0,
                bodyId
        );
        vessel.addCrew(player.getUUID());
        vesselStates.put(vesselId, vessel);
        return vessel;
    }

    private SpaceVesselState requireVessel(UUID vesselId) {
        SpaceVesselState vessel = vesselStates.get(vesselId);
        if (vessel == null) {
            throw new IllegalStateException("Missing vessel state for " + vesselId);
        }
        return vessel;
    }

    private ResourceKey<Level> allocateSdSlot(UUID vesselId) {
        for (Map.Entry<String, UUID> entry : slotAssignments.entrySet()) {
            if (entry.getValue().equals(vesselId)) {
                return SpaceDimensions.dimensionKey(entry.getKey());
            }
        }
        for (ResourceKey<Level> key : SpaceDimensions.sdSlots()) {
            String location = key.location().toString();
            if (!slotAssignments.containsKey(location)) {
                slotAssignments.put(location, vesselId);
                return key;
            }
        }
        throw new IllegalStateException("No free SD slot is available.");
    }

    private void releaseSdSlot(UUID vesselId) {
        slotAssignments.entrySet().removeIf(entry -> entry.getValue().equals(vesselId));
    }

    private ServerLevel requireSdLevel(String location) {
        return requireLevel(location);
    }

    private ServerLevel requireLevel(String location) {
        ServerLevel level = server.getLevel(SpaceDimensions.dimensionKey(location));
        if (level == null && Level.OVERWORLD.location().toString().equals(location)) {
            level = server.overworld();
        }
        if (level == null) {
            throw new IllegalStateException("Missing target level for " + location);
        }
        return level;
    }

    private void ensureShellLevel(ServerLevel level) {
        clearSdArea(level, 0.5, SpaceDimensions.SD_SHELL_Y + 1.0, 0.5, SD_STARTUP_CLEAR_CHUNK_RADIUS);
    }

    private void teleportToSdShell(ServerPlayer player, ServerLevel targetLevel, SpaceVesselState vessel, UUID transitionId) {
        clearSdArea(targetLevel, vessel.shellX(), vessel.shellY(), vessel.shellZ(), SD_STARTUP_CLEAR_CHUNK_RADIUS);
        teleportPlayerSeamlessly(player, targetLevel, vessel.shellX(), vessel.shellY(), vessel.shellZ(), player.getYRot(), player.getXRot(), transitionId);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
    }

    private void teleportPlayerSeamlessly(
            ServerPlayer player,
            ServerLevel targetLevel,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            UUID transitionId
    ) {
        if (!player.level().dimension().equals(targetLevel.dimension())) {
            PacketDistributor.sendToPlayer(player, new SpaceSeamlessTeleportPacket(
                    transitionId,
                    targetLevel.dimension().location().toString(),
                    x,
                    y,
                    z,
                    yRot,
                    xRot,
                    serverTick + SEAMLESS_TELEPORT_TTL_TICKS
            ));
        }
        player.teleportTo(targetLevel, x, y, z, yRot, xRot);
    }

    private void scrubSdPlayers() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpacePlayerState state = getOrCreate(player);
            SpaceTransitionState transition = state.transitionId() == null ? null : transitionStates.get(state.transitionId());
            boolean shouldScrub = switch (state.mode()) {
                case PLANET -> false;
                case SPACE, LANDING_TRANSITION -> true;
                case TAKEOFF_TRANSITION -> transition != null && transition.cutoverApplied();
            };
            if (!shouldScrub || state.sdSlotDimension() == null) {
                continue;
            }
            if (!player.level().dimension().location().toString().equals(state.sdSlotDimension())) {
                continue;
            }
            clearSdArea(player.serverLevel(), player.getX(), player.getY(), player.getZ(), SD_RUNTIME_CLEAR_CHUNK_RADIUS);
        }
    }

    private void clearSdArea(ServerLevel level, double centerX, double centerY, double centerZ, int chunkRadius) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int centerChunkX = BlockPos.containing(centerX, centerY, centerZ).getX() >> 4;
        int centerChunkZ = BlockPos.containing(centerX, centerY, centerZ).getZ() >> 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
                int startX = chunkX << 4;
                int startZ = chunkZ << 4;
                for (int x = startX; x < startX + 16; x++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            cursor.set(x, y, z);
                            if (!level.getBlockState(cursor).isAir()) {
                                level.setBlockAndUpdate(cursor, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
    }

    private void resetSdSlotStorage() {
        Path saveRoot = server.getWorldPath(KSP_RESOURCE).getParent();
        if (saveRoot == null) {
            return;
        }
        Path sdRoot = saveRoot.resolve("dimensions").resolve("gtu_space");
        for (ResourceKey<Level> key : SpaceDimensions.sdSlots()) {
            Path slotDir = sdRoot.resolve(key.location().getPath());
            if (!Files.exists(slotDir)) {
                continue;
            }
            try {
                deleteRecursively(slotDir);
                LOGGER.info("Reset SD slot storage at {}", slotDir);
            } catch (IOException e) {
                LOGGER.warn("Failed to reset SD slot storage at {}", slotDir, e);
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void refreshSpaceVesselsFromSnapshot() {
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return;
        }

        for (SpaceVesselState vessel : vesselStates.values()) {
            KspVesselState authority = snapshot.vessels().get(vessel.authorityVesselId());
            if (authority == null) {
                continue;
            }
            KspBodyState primaryBody = snapshot.bodies().get(authority.primaryBodyId());
            if (primaryBody == null) {
                continue;
            }

            double altitude = Math.max(
                    0.0,
                    authority.absolutePosition().subtract(primaryBody.absolutePosition()).length() - primaryBody.definition().radius()
            );
            vessel.setAltitudeMeters(altitude);
            vessel.setBodyId(primaryBody.definition().id());
            if (PlanetDomainDefinition.hasDefinition(primaryBody.definition().id())) {
                vessel.setLandingBodyId(primaryBody.definition().id());
            }
            vessel.setUniverseAnchorPosition(
                    authority.universeId(),
                    authority.galaxyId(),
                    authority.systemId(),
                    authority.universeAnchorPosition()
            );
            synchronizePlayerStatesToVessel(vessel, primaryBody.definition().id());
        }
    }

    private void captureApproachTransitions() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpacePlayerState state = getOrCreate(player);
            if (state.mode() != SpacePlayerState.Mode.SPACE || state.transitionId() != null || state.vesselId() == null) {
                continue;
            }

            SpaceVesselState vessel = vesselStates.get(state.vesselId());
            if (vessel == null) {
                continue;
            }
            String landingBodyId = vessel.landingBodyId() != null ? vessel.landingBodyId() : vessel.bodyId();
            if (!PlanetDomainDefinition.hasDefinition(landingBodyId)) {
                continue;
            }

            PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(landingBodyId);
            if (!definition.planetDimension().equals(state.stablePlanetDimension())) {
                continue;
            }
            if (vessel.altitudeMeters() <= definition.landingPrewarmAltitudeMeters()) {
                vessel.setLandingDescentActive(false);
                beginLanding(player);
            }
        }
    }

    private void applyManualLandingDescent() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpacePlayerState state = getOrCreate(player);
            if (state.mode() != SpacePlayerState.Mode.SPACE || state.transitionId() != null || state.vesselId() == null) {
                continue;
            }

            SpaceVesselState vessel = vesselStates.get(state.vesselId());
            if (vessel == null || !vessel.landingDescentActive()) {
                continue;
            }

            String landingBodyId = vessel.landingBodyId() != null ? vessel.landingBodyId() : vessel.bodyId();
            if (!PlanetDomainDefinition.hasDefinition(landingBodyId)) {
                vessel.setLandingDescentActive(false);
                continue;
            }

            PlanetDomainDefinition definition = PlanetDomainDefinition.forBody(landingBodyId);
            if (!definition.planetDimension().equals(state.stablePlanetDimension())) {
                vessel.setLandingDescentActive(false);
                continue;
            }
            double threshold = definition.landingPrewarmAltitudeMeters();
            double nextAltitude = Math.max(threshold, vessel.altitudeMeters() - definition.landingMetersPerTick());
            if (nextAltitude < vessel.altitudeMeters() - 1.0E-3) {
                upsertAuthorityVessel(vessel, nextAltitude);
                syncPlayer(player);
            } else {
                vessel.setLandingDescentActive(false);
            }
        }
    }

    private void processTransitions() {
        for (Map.Entry<UUID, SpacePlayerState> entry : new LinkedHashMap<>(playerStates).entrySet()) {
            SpacePlayerState state = entry.getValue();
            if (state.transitionId() == null) {
                continue;
            }
            SpaceTransitionState transition = transitionStates.get(state.transitionId());
            if (transition == null) {
                continue;
            }
            SpaceVesselState vessel = state.vesselId() == null ? null : vesselStates.get(state.vesselId());
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            double altitude = transition.altitudeAt(serverTick);
            if (vessel != null) {
                vessel.setAltitudeMeters(Math.max(0.0, altitude));
            }

            if (transition.direction() == SpaceTransitionDirection.TAKEOFF && transition.shouldCutover(serverTick)) {
                handleTakeoffCutover(player, state, transition, vessel);
            }

            if (vessel != null && (transition.direction() == SpaceTransitionDirection.LANDING || transition.cutoverApplied())) {
                upsertAuthorityVessel(vessel, Math.max(0.0, altitude));
            }

            if (transition.isComplete(serverTick)) {
                if (transition.direction() == SpaceTransitionDirection.TAKEOFF) {
                    completeTakeoff(player, state, transition, vessel);
                } else {
                    completeLanding(player, state, transition, vessel);
                }
            }
        }
    }

    private void handleTakeoffCutover(ServerPlayer player, SpacePlayerState state, SpaceTransitionState transition, SpaceVesselState vessel) {
        if (vessel == null) {
            return;
        }
        upsertAuthorityVessel(vessel, transition.cutoverAltitudeMeters());
        ServerLevel target = requireSdLevel(vessel.sdSlotDimension());
        ensureShellLevel(target);
        if (player != null) {
            teleportToSdShell(player, target, vessel, transition.transitionId());
        }
        transition.markCutoverApplied();
    }

    private void completeTakeoff(ServerPlayer player, SpacePlayerState state, SpaceTransitionState transition, SpaceVesselState vessel) {
        if (vessel != null) {
            vessel.setLandingDescentActive(false);
        }
        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.SPACE,
                state.bodyId(),
                state.planetDimension(),
                state.stablePlanetDimension(),
                state.stableX(),
                state.stableY(),
                state.stableZ(),
                state.stableYRot(),
                state.stableXRot(),
                vessel == null ? state.sdSlotDimension() : vessel.sdSlotDimension(),
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                state.vesselId(),
                null
        );
        playerStates.put(player == null ? findPlayerId(state) : player.getUUID(), next);
        transitionStates.remove(transition.transitionId());
        cleanupPortalsForTransition(transition.transitionId());
    }

    private void completeLanding(ServerPlayer player, SpacePlayerState state, SpaceTransitionState transition, SpaceVesselState vessel) {
        UUID playerId = player == null ? findPlayerId(state) : player.getUUID();
        if (vessel != null) {
            vessel.setLandingDescentActive(false);
        }
        String targetDimension = transition.targetDimension();
        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.PLANET,
                state.bodyId(),
                targetDimension,
                targetDimension,
                state.stableX(),
                state.stableY(),
                state.stableZ(),
                state.stableYRot(),
                state.stableXRot(),
                null,
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                null,
                null
        );
        playerStates.put(playerId, next);
        transitionStates.remove(transition.transitionId());
        cleanupPortalsForTransition(transition.transitionId());

        if (player != null) {
            ServerLevel target = requireLevel(targetDimension);
            teleportPlayerSeamlessly(player, target, state.stableX(), state.stableY(), state.stableZ(), state.stableYRot(), state.stableXRot(), transition.transitionId());
        }
        if (vessel != null) {
            cleanupVessel(vessel);
        }
    }

    private UUID findPlayerId(SpacePlayerState state) {
        for (Map.Entry<UUID, SpacePlayerState> entry : playerStates.entrySet()) {
            if (entry.getValue() == state) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Unable to recover player UUID for state");
    }

    private void rebindOnlinePlayers() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpacePlayerState state = getOrCreate(player);
            SpaceTransitionState transition = state.transitionId() == null ? null : transitionStates.get(state.transitionId());
            boolean sdPhysics = switch (state.mode()) {
                case PLANET -> false;
                case SPACE, LANDING_TRANSITION -> true;
                case TAKEOFF_TRANSITION -> transition != null && transition.cutoverApplied();
            };
            player.setNoGravity(sdPhysics);
            player.fallDistance = 0.0F;
            if (state.mode() == SpacePlayerState.Mode.PLANET) {
                if (!player.level().dimension().location().toString().equals(state.stablePlanetDimension())) {
                    ServerLevel target = requireLevel(state.stablePlanetDimension());
                    teleportPlayerSeamlessly(player, target, state.stableX(), state.stableY(), state.stableZ(), state.stableYRot(), state.stableXRot(), state.transitionId());
                }
                continue;
            }
            if (state.mode() == SpacePlayerState.Mode.TAKEOFF_TRANSITION) {
                if (transition != null && !transition.cutoverApplied()) {
                    if (!player.level().dimension().location().toString().equals(state.stablePlanetDimension())) {
                        ServerLevel target = requireLevel(state.stablePlanetDimension());
                        teleportPlayerSeamlessly(player, target, state.stableX(), state.stableY(), state.stableZ(), state.stableYRot(), state.stableXRot(), state.transitionId());
                    }
                    continue;
                }
            }
            if (state.sdSlotDimension() == null) {
                continue;
            }
            if (!player.level().dimension().location().toString().equals(state.sdSlotDimension())) {
                SpaceVesselState vessel = state.vesselId() == null ? null : vesselStates.get(state.vesselId());
                if (vessel == null) {
                    continue;
                }
                ServerLevel target = requireSdLevel(state.sdSlotDimension());
                ensureShellLevel(target);
                teleportToSdShell(player, target, vessel, state.transitionId());
            }
        }
    }

    private void cleanupOrphanVessels() {
        Set<UUID> referencedVessels = playerStates.values().stream()
                .map(SpacePlayerState::vesselId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        for (Map.Entry<UUID, SpaceVesselState> entry : new LinkedHashMap<>(vesselStates).entrySet()) {
            if (!referencedVessels.contains(entry.getKey())) {
                cleanupVessel(entry.getValue());
            }
        }
    }

    private void cleanupPlayerVessel(SpacePlayerState state) {
        if (state.vesselId() == null) {
            return;
        }
        SpaceVesselState vessel = vesselStates.get(state.vesselId());
        if (vessel == null) {
            return;
        }
        cleanupVessel(vessel);
    }

    private void cleanupVessel(SpaceVesselState vessel) {
        kspSystem.removeVessel(vessel.authorityVesselId());
        releaseSdSlot(vessel.vesselId());
        vesselStates.remove(vessel.vesselId());
        transitionStates.values().removeIf(transition -> vessel.vesselId().equals(resolveTransitionVessel(transition.transitionId())));
    }

    private void synchronizePlayerStatesToVessel(SpaceVesselState vessel, String bodyId) {
        for (Map.Entry<UUID, SpacePlayerState> entry : new LinkedHashMap<>(playerStates).entrySet()) {
            SpacePlayerState state = entry.getValue();
            if (!vessel.vesselId().equals(state.vesselId())) {
                continue;
            }

            String planetDimension = PlanetDomainDefinition.hasDefinition(bodyId)
                    ? PlanetDomainDefinition.forBody(bodyId).planetDimension()
                    : state.planetDimension();

            SpacePlayerState next = new SpacePlayerState(
                    state.mode(),
                    bodyId,
                    planetDimension,
                    state.stablePlanetDimension(),
                    state.stableX(),
                    state.stableY(),
                    state.stableZ(),
                    state.stableYRot(),
                    state.stableXRot(),
                    vessel.sdSlotDimension(),
                    vessel.universeId(),
                    vessel.galaxyId(),
                    vessel.systemId(),
                    vessel.universeAnchorPosition(),
                    state.coordinate(),
                    state.vesselId(),
                    state.transitionId()
            );
            playerStates.put(entry.getKey(), next);
        }
    }

    private UUID resolveTransitionVessel(UUID transitionId) {
        for (SpacePlayerState value : playerStates.values()) {
            if (transitionId.equals(value.transitionId())) {
                return value.vesselId();
            }
        }
        return null;
    }

    private void upsertAuthorityVessel(SpaceVesselState vessel, double altitudeMeters) {
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return;
        }
        KspBodyState body = snapshot.bodies().get(vessel.bodyId());
        if (body == null) {
            return;
        }
        SurfaceFrame frame = surfaceFrame(body);
        double orbitalRadius = body.definition().radius() + Math.max(1.0, altitudeMeters);
        double circularSpeed = Math.sqrt(body.definition().gravitationalParameter() / orbitalRadius);
        SpaceVector absolutePosition = body.absolutePosition().add(frame.surfaceDirection().scale(orbitalRadius));
        SpaceVector absoluteVelocity = body.absoluteVelocity().add(frame.progradeDirection().scale(circularSpeed));
        vessel.setAltitudeMeters(altitudeMeters);
        vessel.setUniverseAnchorPosition(
                snapshot.universe().universeId(),
                snapshot.universe().galaxy().galaxyId(),
                currentSystemId(),
                snapshot.universe().systemUniversePosition()
        );
        kspSystem.upsertVessel(new KspVesselState(
                vessel.authorityVesselId(),
                "Session Vessel " + vessel.vesselId().toString().substring(0, 8),
                snapshot.universe().universeId(),
                snapshot.universe().galaxy().galaxyId(),
                currentSystemId(),
                snapshot.universe().systemUniversePosition(),
                vessel.bodyId(),
                2_000.0,
                absolutePosition,
                absoluteVelocity
        ));
    }

    private LandingTarget resolveDynamicLandingTarget(
            ServerPlayer player,
            SpacePlayerState current,
            SpaceVesselState vessel,
            PlanetDomainDefinition definition
    ) {
        ServerLevel targetLevel = requireLevel(definition.planetDimension());
        double targetX = current.stableX();
        double targetZ = current.stableZ();

        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return fallbackLandingTarget(current, definition);
        }

        KspVesselState authority = snapshot.vessels().get(vessel.authorityVesselId());
        String landingBodyId = vessel.landingBodyId() != null ? vessel.landingBodyId() : vessel.bodyId();
        KspBodyState body = snapshot.bodies().get(landingBodyId);
        if (authority == null || body == null) {
            return fallbackLandingTarget(current, definition);
        }

        SpaceVector relative = authority.absolutePosition().subtract(body.absolutePosition());
        double maxComponent = Math.max(Math.abs(relative.x()), Math.max(Math.abs(relative.y()), Math.abs(relative.z())));
        if (maxComponent < 1.0E-6) {
            return fallbackLandingTarget(current, definition);
        }

        double localX = clamp(relative.x() / maxComponent, -1.0, 1.0);
        double localZ = clamp(relative.z() / maxComponent, -1.0, 1.0);
        targetX += localX * LANDING_HORIZONTAL_OFFSET_BLOCKS;
        targetZ += localZ * LANDING_HORIZONTAL_OFFSET_BLOCKS;
        BlockPos preferred = BlockPos.containing(targetX, current.stableY(), targetZ);
        BlockPos fallback = BlockPos.containing(current.stableX(), current.stableY(), current.stableZ());
        BlockPos safePos = usesNaturalSurfaceLanding(definition)
                ? findNaturalSurfaceLandingPos(targetLevel, preferred, fallback)
                : findSafeLandingPos(targetLevel, dominantCubeFace(relative), preferred, fallback);

        return new LandingTarget(
                definition.planetDimension(),
                safePos.getX() + 0.5,
                safePos.getY(),
                safePos.getZ() + 0.5,
                player == null ? current.stableYRot() : player.getYRot(),
                player == null ? current.stableXRot() : player.getXRot()
        );
    }

    private LandingTarget fallbackLandingTarget(SpacePlayerState current, PlanetDomainDefinition definition) {
        ServerLevel targetLevel = requireLevel(definition.planetDimension());
        BlockPos preferred = BlockPos.containing(current.stableX(), current.stableY(), current.stableZ());
        BlockPos safePos = usesNaturalSurfaceLanding(definition)
                ? findNaturalSurfaceLandingPos(targetLevel, preferred, preferred)
                : findSafeLandingPos(targetLevel, 4, preferred, preferred);
        return new LandingTarget(
                definition.planetDimension(),
                safePos.getX() + 0.5,
                safePos.getY(),
                safePos.getZ() + 0.5,
                current.stableYRot(),
                current.stableXRot()
        );
    }

    private BlockPos findSafeLandingPos(ServerLevel level, int faceIndex, BlockPos preferred, BlockPos fallback) {
        List<BlockPos> searchCenters = List.of(preferred, fallback);
        for (BlockPos center : searchCenters) {
            BlockPos direct = safeSurfaceAt(level, faceIndex, center.getX(), center.getZ());
            if (isSafeLandingPos(level, direct)) {
                return direct;
            }
            for (int radius = 8; radius <= LANDING_SEARCH_RADIUS_BLOCKS; radius += 8) {
                for (int dx = -radius; dx <= radius; dx += 8) {
                    BlockPos north = safeSurfaceAt(level, faceIndex, center.getX() + dx, center.getZ() - radius);
                    if (isSafeLandingPos(level, north)) {
                        return north;
                    }
                    BlockPos south = safeSurfaceAt(level, faceIndex, center.getX() + dx, center.getZ() + radius);
                    if (isSafeLandingPos(level, south)) {
                        return south;
                    }
                }
                for (int dz = -radius + 8; dz <= radius - 8; dz += 8) {
                    BlockPos west = safeSurfaceAt(level, faceIndex, center.getX() - radius, center.getZ() + dz);
                    if (isSafeLandingPos(level, west)) {
                        return west;
                    }
                    BlockPos east = safeSurfaceAt(level, faceIndex, center.getX() + radius, center.getZ() + dz);
                    if (isSafeLandingPos(level, east)) {
                        return east;
                    }
                }
            }
        }
        return safeSurfaceAt(level, faceIndex, fallback.getX(), fallback.getZ());
    }

    private BlockPos findNaturalSurfaceLandingPos(ServerLevel level, BlockPos preferred, BlockPos fallback) {
        List<BlockPos> searchCenters = List.of(preferred, fallback, level.getSharedSpawnPos());
        for (BlockPos center : searchCenters) {
            BlockPos direct = naturalSurfaceAt(level, center.getX(), center.getZ());
            if (isSafeLandingPos(level, direct)) {
                return direct;
            }
            for (int radius = 8; radius <= LANDING_SEARCH_RADIUS_BLOCKS; radius += 8) {
                for (int dx = -radius; dx <= radius; dx += 8) {
                    BlockPos north = naturalSurfaceAt(level, center.getX() + dx, center.getZ() - radius);
                    if (isSafeLandingPos(level, north)) {
                        return north;
                    }
                    BlockPos south = naturalSurfaceAt(level, center.getX() + dx, center.getZ() + radius);
                    if (isSafeLandingPos(level, south)) {
                        return south;
                    }
                }
                for (int dz = -radius + 8; dz <= radius - 8; dz += 8) {
                    BlockPos west = naturalSurfaceAt(level, center.getX() - radius, center.getZ() + dz);
                    if (isSafeLandingPos(level, west)) {
                        return west;
                    }
                    BlockPos east = naturalSurfaceAt(level, center.getX() + radius, center.getZ() + dz);
                    if (isSafeLandingPos(level, east)) {
                        return east;
                    }
                }
            }
        }
        return naturalSurfaceAt(level, fallback.getX(), fallback.getZ());
    }

    private BlockPos safeSurfaceAt(ServerLevel level, int faceIndex, int x, int z) {
        int minY = level.getMinBuildHeight();
        int surfaceY = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), minY + 2);
        BlockPos candidate = new BlockPos(x, surfaceY + 1, z);
        BlockPos safe = coordinateMapper.findSafeSpawnLocation(faceIndex, candidate, level);
        if (isSafeLandingPos(level, safe)) {
            return safe;
        }

        int oceanFloorY = Math.max(level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), minY + 2);
        BlockPos oceanFloorCandidate = new BlockPos(x, oceanFloorY + 1, z);
        safe = coordinateMapper.findSafeSpawnLocation(faceIndex, oceanFloorCandidate, level);
        if (isSafeLandingPos(level, safe)) {
            return safe;
        }
        return candidate;
    }

    private BlockPos naturalSurfaceAt(ServerLevel level, int x, int z) {
        int minY = level.getMinBuildHeight();
        int surfaceY = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), minY + 2);
        surfaceY = Math.max(surfaceY, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        surfaceY = Math.max(surfaceY, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, surfaceY + 8, z);

        while (cursor.getY() > minY + 2 && isReplaceableForLanding(level, cursor)) {
            cursor.move(0, -1, 0);
        }
        if (!isReplaceableForLanding(level, cursor)) {
            cursor.move(0, 1, 0);
        }

        BlockPos candidate = cursor.immutable();
        if (isSafeLandingPos(level, candidate)) {
            return candidate;
        }

        BlockPos spawn = level.getSharedSpawnPos();
        return new BlockPos(spawn.getX(), Math.max(spawn.getY(), minY + 3), spawn.getZ());
    }

    private boolean usesNaturalSurfaceLanding(PlanetDomainDefinition definition) {
        return Level.OVERWORLD.location().toString().equals(definition.planetDimension()) || "earth".equals(definition.bodyId());
    }

    private boolean isReplaceableForLanding(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                || level.getBlockState(pos).canBeReplaced()
                || !level.getFluidState(pos).isEmpty();
    }

    private boolean isSafeLandingPos(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() + 2) {
            return false;
        }
        if (level.getBlockState(pos.below()).is(Blocks.BEDROCK)) {
            return false;
        }
        if (!level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }
        return !level.getBlockState(pos.below()).isAir()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    private static int dominantCubeFace(SpaceVector vector) {
        double absX = Math.abs(vector.x());
        double absY = Math.abs(vector.y());
        double absZ = Math.abs(vector.z());
        if (absZ >= absX && absZ >= absY) {
            return vector.z() < 0.0 ? 0 : 1;
        }
        if (absX >= absY) {
            return vector.x() < 0.0 ? 2 : 3;
        }
        return vector.y() >= 0.0 ? 4 : 5;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateEarthDayTime() {
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return;
        }
        KspBodyState earth = snapshot.bodies().get("earth");
        if (earth == null) {
            return;
        }
        double fraction = normalizeTurns(earth.rotationPhaseRadians() / (Math.PI * 2.0));
        long dayTime = Math.round(fraction * 24000.0);
        server.overworld().setDayTime(dayTime);
    }

    private void bridgePhysicsState() {
        KspSnapshot snapshot = currentSnapshot();
        SpacePhysicsBridge bridge = SpacePhysicsBridgeRegistry.bridge();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpacePlayerState state = getOrCreate(player);
            SpaceVesselState vessel = state.vesselId() == null ? null : vesselStates.get(state.vesselId());
            if (vessel != null) {
                SpaceTransitionState transition = state.transitionId() == null ? null : transitionStates.get(state.transitionId());
                SpaceDomain currentDomain = switch (state.mode()) {
                    case PLANET -> SpaceDomain.PD;
                    case SPACE, LANDING_TRANSITION -> SpaceDomain.SD;
                    case TAKEOFF_TRANSITION -> transition != null && transition.cutoverApplied() ? SpaceDomain.SD : SpaceDomain.PD;
                };
                bridge.publishPlayerState(player, new SpacePhysicsOutput(
                        currentDomain,
                        state.bodyId(),
                        vessel.sdSlotDimension(),
                        vessel.shellCoordinate(),
                        vessel.shellX(),
                        vessel.shellY(),
                        vessel.shellZ()
                ));
                SpacePhysicsInput input = bridge.readPlayerInput(player, state, vessel, snapshot);
                if (input.landingContact() && state.mode() == SpacePlayerState.Mode.LANDING_TRANSITION) {
                    if (transition != null && !transition.isComplete(serverTick)) {
                        transitionStates.put(transition.transitionId(), new SpaceTransitionState(
                                transition.transitionId(),
                                transition.direction(),
                                transition.sourceDomain(),
                                transition.targetDomain(),
                                transition.bodyId(),
                                transition.sourceDimension(),
                                transition.targetDimension(),
                                transition.startTick(),
                                transition.cutoverTick(),
                                serverTick,
                                transition.startAltitudeMeters(),
                                transition.cutoverAltitudeMeters(),
                                0.0,
                                true
                        ));
                    }
                }
            }
        }
    }

    private void syncPlayers() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    private SpaceStateSyncPacket buildStatePacket(SpacePlayerState state) {
        SpaceVesselState vessel = state.vesselId() == null ? null : vesselStates.get(state.vesselId());
        return new SpaceStateSyncPacket(
                state.mode(),
                state.bodyId(),
                state.planetDimension(),
                state.stablePlanetDimension(),
                state.sdSlotDimension(),
                state.stableX(),
                state.stableY(),
                state.stableZ(),
                state.stableYRot(),
                state.stableXRot(),
                state.vesselId(),
                vessel == null ? null : vessel.authorityVesselId(),
                vessel == null ? 0.0 : vessel.altitudeMeters(),
                state.transitionId()
        );
    }

    private SpaceTransitionPacket buildTransitionPacket(SpaceTransitionState transition) {
        if (transition == null) {
            return SpaceTransitionPacket.inactive(serverTick);
        }
        return new SpaceTransitionPacket(
                true,
                serverTick,
                transition.transitionId(),
                transition.direction(),
                transition.sourceDomain(),
                transition.targetDomain(),
                transition.bodyId(),
                transition.sourceDimension(),
                transition.targetDimension(),
                transition.startTick(),
                transition.cutoverTick(),
                transition.completeTick(),
                transition.startAltitudeMeters(),
                transition.cutoverAltitudeMeters(),
                transition.completeAltitudeMeters(),
                transition.cutoverApplied()
        );
    }

    private SpaceSnapshotPacket buildSnapshotPacket() {
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return new SpaceSnapshotPacket(0L, 0.0, java.util.List.of(), java.util.List.of());
        }
        java.util.List<SpaceSnapshotPacket.BodyData> bodies = new java.util.ArrayList<>(snapshot.bodies().size());
        for (KspBodyState body : snapshot.bodies().values()) {
            bodies.add(new SpaceSnapshotPacket.BodyData(
                    body.definition().id(),
                    body.referenceBodyId(),
                    body.definition().bodyKind(),
                    body.definition().radius(),
                    body.absolutePosition().x(),
                    body.absolutePosition().y(),
                    body.absolutePosition().z(),
                    body.absoluteVelocity().x(),
                    body.absoluteVelocity().y(),
                    body.absoluteVelocity().z(),
                    body.definition().rotationAxis().x(),
                    body.definition().rotationAxis().y(),
                    body.definition().rotationAxis().z(),
                    body.rotationPhaseRadians()
            ));
        }
        java.util.List<SpaceSnapshotPacket.VesselData> vessels = new java.util.ArrayList<>(snapshot.vessels().size());
        for (KspVesselState vessel : snapshot.vessels().values()) {
            vessels.add(new SpaceSnapshotPacket.VesselData(
                    vessel.id(),
                    vessel.primaryBodyId(),
                    vessel.absolutePosition().x(),
                    vessel.absolutePosition().y(),
                    vessel.absolutePosition().z(),
                    vessel.absoluteVelocity().x(),
                    vessel.absoluteVelocity().y(),
                    vessel.absoluteVelocity().z()
            ));
        }
        return new SpaceSnapshotPacket(snapshot.simulationTick(), snapshot.simulationSeconds(), java.util.List.copyOf(bodies), java.util.List.copyOf(vessels));
    }

    private Map<String, KspSaveData.PlayerStateData> exportPlayerStates() {
        Map<String, KspSaveData.PlayerStateData> result = new LinkedHashMap<>();
        for (Map.Entry<UUID, SpacePlayerState> entry : playerStates.entrySet()) {
            SpacePlayerState state = entry.getValue();
            KspSaveData.PlayerStateData data = new KspSaveData.PlayerStateData();
            data.mode = state.mode().name();
            data.bodyId = state.bodyId();
            data.planetDimension = state.planetDimension();
            data.stablePlanetDimension = state.stablePlanetDimension();
            data.stableX = state.stableX();
            data.stableY = state.stableY();
            data.stableZ = state.stableZ();
            data.stableYRot = state.stableYRot();
            data.stableXRot = state.stableXRot();
            data.sdSlotDimension = state.sdSlotDimension();
            data.universeId = state.universeId();
            data.galaxyId = state.galaxyId();
            data.systemId = state.systemId();
            UniverseVector uv = state.universeAnchorPosition();
            data.universeAnchorPosition = new String[]{
                    uv.x().toPlainString(),
                    uv.y().toPlainString(),
                    uv.z().toPlainString()
            };
            data.coordinate = new double[]{
                    state.coordinate().sectorX(),
                    state.coordinate().sectorY(),
                    state.coordinate().sectorZ(),
                    state.coordinate().local().x(),
                    state.coordinate().local().y(),
                    state.coordinate().local().z()
            };
            data.vesselId = state.vesselId() == null ? null : state.vesselId().toString();
            data.transitionId = state.transitionId() == null ? null : state.transitionId().toString();
            result.put(entry.getKey().toString(), data);
        }
        return result;
    }

    private void importPlayerStates(Map<String, KspSaveData.PlayerStateData> data) {
        if (data == null) {
            return;
        }
        for (Map.Entry<String, KspSaveData.PlayerStateData> entry : data.entrySet()) {
            UUID playerId = UUID.fromString(entry.getKey());
            KspSaveData.PlayerStateData stateData = entry.getValue();
            UniverseVector uv = stateData.universeAnchorPosition == null
                    ? UniverseVector.zero()
                    : new UniverseVector(
                    new BigDecimal(stateData.universeAnchorPosition[0]),
                    new BigDecimal(stateData.universeAnchorPosition[1]),
                    new BigDecimal(stateData.universeAnchorPosition[2])
            );
            SpaceCoordinate coordinate = stateData.coordinate != null && stateData.coordinate.length >= 6
                    ? new SpaceCoordinate(
                    (long) stateData.coordinate[0],
                    (long) stateData.coordinate[1],
                    (long) stateData.coordinate[2],
                    new SpaceVector(stateData.coordinate[3], stateData.coordinate[4], stateData.coordinate[5])
            )
                    : SpaceCoordinate.origin();
            playerStates.put(playerId, new SpacePlayerState(
                    SpacePlayerState.Mode.valueOf(stateData.mode),
                    stateData.bodyId == null ? "earth" : stateData.bodyId,
                    stateData.planetDimension == null ? Level.OVERWORLD.location().toString() : stateData.planetDimension,
                    stateData.stablePlanetDimension == null ? Level.OVERWORLD.location().toString() : stateData.stablePlanetDimension,
                    stateData.stableX,
                    stateData.stableY,
                    stateData.stableZ,
                    stateData.stableYRot,
                    stateData.stableXRot,
                    stateData.sdSlotDimension,
                    stateData.universeId,
                    stateData.galaxyId,
                    stateData.systemId,
                    uv,
                    coordinate,
                    stateData.vesselId == null ? null : UUID.fromString(stateData.vesselId),
                    stateData.transitionId == null ? null : UUID.fromString(stateData.transitionId)
            ));
        }
    }

    private Map<String, KspSaveData.SpaceVesselData> exportSpaceVessels() {
        Map<String, KspSaveData.SpaceVesselData> result = new LinkedHashMap<>();
        for (Map.Entry<UUID, SpaceVesselState> entry : vesselStates.entrySet()) {
            SpaceVesselState vessel = entry.getValue();
            KspSaveData.SpaceVesselData data = new KspSaveData.SpaceVesselData();
            data.vesselId = vessel.vesselId().toString();
            data.authorityVesselId = vessel.authorityVesselId();
            data.bodyId = vessel.bodyId();
            data.universeId = vessel.universeId();
            data.galaxyId = vessel.galaxyId();
            data.systemId = vessel.systemId();
            data.universeAnchorPosition = new String[]{
                    vessel.universeAnchorPosition().x().toPlainString(),
                    vessel.universeAnchorPosition().y().toPlainString(),
                    vessel.universeAnchorPosition().z().toPlainString()
            };
            data.sdSlotDimension = vessel.sdSlotDimension();
            data.shellCoordinate = new double[]{
                    vessel.shellCoordinate().sectorX(),
                    vessel.shellCoordinate().sectorY(),
                    vessel.shellCoordinate().sectorZ(),
                    vessel.shellCoordinate().local().x(),
                    vessel.shellCoordinate().local().y(),
                    vessel.shellCoordinate().local().z()
            };
            data.shellX = vessel.shellX();
            data.shellY = vessel.shellY();
            data.shellZ = vessel.shellZ();
            data.altitudeMeters = vessel.altitudeMeters();
            data.landingBodyId = vessel.landingBodyId();
            data.crew = vessel.crew().stream().map(UUID::toString).toArray(String[]::new);
            result.put(entry.getKey().toString(), data);
        }
        return result;
    }

    private void importSpaceVessels(Map<String, KspSaveData.SpaceVesselData> data) {
        if (data == null) {
            return;
        }
        for (Map.Entry<String, KspSaveData.SpaceVesselData> entry : data.entrySet()) {
            KspSaveData.SpaceVesselData vesselData = entry.getValue();
            UniverseVector uv = vesselData.universeAnchorPosition == null
                    ? UniverseVector.zero()
                    : new UniverseVector(
                    new BigDecimal(vesselData.universeAnchorPosition[0]),
                    new BigDecimal(vesselData.universeAnchorPosition[1]),
                    new BigDecimal(vesselData.universeAnchorPosition[2])
            );
            SpaceCoordinate coordinate = vesselData.shellCoordinate != null && vesselData.shellCoordinate.length >= 6
                    ? new SpaceCoordinate(
                    (long) vesselData.shellCoordinate[0],
                    (long) vesselData.shellCoordinate[1],
                    (long) vesselData.shellCoordinate[2],
                    new SpaceVector(vesselData.shellCoordinate[3], vesselData.shellCoordinate[4], vesselData.shellCoordinate[5])
            )
                    : SpaceCoordinate.origin();
            SpaceVesselState vessel = new SpaceVesselState(
                    UUID.fromString(vesselData.vesselId),
                    vesselData.authorityVesselId,
                    vesselData.bodyId,
                    vesselData.universeId,
                    vesselData.galaxyId,
                    vesselData.systemId,
                    uv,
                    vesselData.sdSlotDimension,
                    coordinate,
                    vesselData.shellX,
                    vesselData.shellY,
                    vesselData.shellZ,
                    vesselData.altitudeMeters,
                    vesselData.landingBodyId
            );
            if (vesselData.crew != null) {
                for (String crewId : vesselData.crew) {
                    vessel.addCrew(UUID.fromString(crewId));
                }
            }
            vesselStates.put(vessel.vesselId(), vessel);
            if (vessel.sdSlotDimension() != null) {
                slotAssignments.put(vessel.sdSlotDimension(), vessel.vesselId());
            }
        }
    }

    private Map<String, KspSaveData.SpaceTransitionData> exportSpaceTransitions() {
        Map<String, KspSaveData.SpaceTransitionData> result = new LinkedHashMap<>();
        for (Map.Entry<UUID, SpaceTransitionState> entry : transitionStates.entrySet()) {
            SpaceTransitionState transition = entry.getValue();
            KspSaveData.SpaceTransitionData data = new KspSaveData.SpaceTransitionData();
            data.transitionId = transition.transitionId().toString();
            data.direction = transition.direction().name();
            data.sourceDomain = transition.sourceDomain().name();
            data.targetDomain = transition.targetDomain().name();
            data.bodyId = transition.bodyId();
            data.sourceDimension = transition.sourceDimension();
            data.targetDimension = transition.targetDimension();
            data.startTick = transition.startTick();
            data.cutoverTick = transition.cutoverTick();
            data.completeTick = transition.completeTick();
            data.startAltitudeMeters = transition.startAltitudeMeters();
            data.cutoverAltitudeMeters = transition.cutoverAltitudeMeters();
            data.completeAltitudeMeters = transition.completeAltitudeMeters();
            data.cutoverApplied = transition.cutoverApplied();
            result.put(entry.getKey().toString(), data);
        }
        return result;
    }

    private void importSpaceTransitions(Map<String, KspSaveData.SpaceTransitionData> data) {
        if (data == null) {
            return;
        }
        for (Map.Entry<String, KspSaveData.SpaceTransitionData> entry : data.entrySet()) {
            KspSaveData.SpaceTransitionData transitionData = entry.getValue();
            transitionStates.put(UUID.fromString(entry.getKey()), new SpaceTransitionState(
                    UUID.fromString(transitionData.transitionId),
                    SpaceTransitionDirection.valueOf(transitionData.direction),
                    SpaceDomain.valueOf(transitionData.sourceDomain),
                    SpaceDomain.valueOf(transitionData.targetDomain),
                    transitionData.bodyId,
                    transitionData.sourceDimension,
                    transitionData.targetDimension,
                    transitionData.startTick,
                    transitionData.cutoverTick,
                    transitionData.completeTick,
                    transitionData.startAltitudeMeters,
                    transitionData.cutoverAltitudeMeters,
                    transitionData.completeAltitudeMeters,
                    transitionData.cutoverApplied
            ));
        }
    }

    private void close() {
        kspSystem.close();
        physicsEngine.close();
        playerStates.clear();
        vesselStates.clear();
        transitionStates.clear();
        slotAssignments.clear();
        portalManager.clear();
        syncManager.clear();
        transitionManager.clear();
    }

    private KspSnapshot currentSnapshot() {
        return kspSystem.latestSnapshot();
    }

    private String currentUniverseId() {
        KspSnapshot snapshot = currentSnapshot();
        return snapshot == null ? "observable_universe" : snapshot.universe().universeId();
    }

    private String currentGalaxyId() {
        KspSnapshot snapshot = currentSnapshot();
        return snapshot == null ? "milky_way" : snapshot.universe().galaxy().galaxyId();
    }

    private String currentSystemId() {
        KspSnapshot snapshot = currentSnapshot();
        if (snapshot == null || snapshot.vessels().isEmpty()) {
            return "sol";
        }
        return snapshot.vessels().values().iterator().next().systemId();
    }

    private UniverseVector currentUniverseAnchor() {
        KspSnapshot snapshot = currentSnapshot();
        return snapshot == null ? UniverseVector.zero() : snapshot.universe().systemUniversePosition();
    }

    private static SurfaceFrame surfaceFrame(KspBodyState body) {
        SpaceVector axis = normalizeOrFallback(body.definition().rotationAxis(), new SpaceVector(0.0, 1.0, 0.0));
        SpaceVector reference = Math.abs(axis.x()) > 0.92 ? new SpaceVector(0.0, 0.0, 1.0) : new SpaceVector(1.0, 0.0, 0.0);
        SpaceVector east = normalizeOrFallback(axis.cross(reference), new SpaceVector(0.0, 0.0, 1.0));
        SpaceVector north = normalizeOrFallback(east.cross(axis), new SpaceVector(1.0, 0.0, 0.0));
        double phase = body.rotationPhaseRadians();
        SpaceVector surfaceDirection = normalizeOrFallback(
                north.scale(Math.cos(phase)).add(east.scale(Math.sin(phase))),
                new SpaceVector(1.0, 0.0, 0.0)
        );
        SpaceVector prograde = normalizeOrFallback(axis.cross(surfaceDirection), new SpaceVector(0.0, 0.0, 1.0));
        return new SurfaceFrame(surfaceDirection, prograde);
    }

    private static SpaceVector normalizeOrFallback(SpaceVector vector, SpaceVector fallback) {
        return vector.lengthSquared() < 1.0E-12 ? fallback : vector.normalize();
    }

    private static double normalizeTurns(double turns) {
        double normalized = turns % 1.0;
        return normalized < 0.0 ? normalized + 1.0 : normalized;
    }

    private record SurfaceFrame(SpaceVector surfaceDirection, SpaceVector progradeDirection) {
    }

    private record LandingTarget(
            String dimension,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {
    }

    private void createTakeoffPortal(ServerPlayer player, PlanetDomainDefinition definition, String planetDimension, String sdDimension) {
        double halfSize = 16.0;
        double portalY = definition.takeoffPrewarmAltitudeMeters();
        Portal portal = new Portal(
                UUID.randomUUID(),
                planetDimension,
                sdDimension,
                4,
                new net.minecraft.world.phys.AABB(
                        player.getX() - halfSize, portalY - 1.0, player.getZ() - halfSize,
                        player.getX() + halfSize, portalY + 1.0, player.getZ() + halfSize
                ),
                false,
                PortalOrientation.DEFAULT,
                PortalType.SURFACE_TO_SPACE
        );
        portalManager.registerPortal(portal);
        broadcastPortal(portal, true);
    }

    private void createLandingPortal(SpaceVesselState vessel, String sdDimension, String planetDimension) {
        double halfSize = 16.0;
        double shellY = SpaceDimensions.SD_SHELL_Y;
        Portal portal = new Portal(
                UUID.randomUUID(),
                sdDimension,
                planetDimension,
                5,
                new net.minecraft.world.phys.AABB(
                        vessel.shellX() - halfSize, shellY - 1.0, vessel.shellZ() - halfSize,
                        vessel.shellX() + halfSize, shellY + 1.0, vessel.shellZ() + halfSize
                ),
                false,
                PortalOrientation.DEFAULT,
                PortalType.SPACE_TO_SURFACE
        );
        portalManager.registerPortal(portal);
        broadcastPortal(portal, true);
    }

    private void cleanupPortalsForTransition(UUID transitionId) {
        for (Portal portal : portalManager.getAllPortals()) {
            portalManager.unregisterPortal(portal.id());
            broadcastPortal(portal, false);
        }
    }

    private void broadcastPortal(Portal portal, boolean active) {
        SpacePortalSyncPacket packet = new SpacePortalSyncPacket(
                portal.id(),
                portal.sourceDimension(),
                portal.targetDimension(),
                portal.area().minX, portal.area().minY, portal.area().minZ,
                portal.area().maxX, portal.area().maxY, portal.area().maxZ,
                portal.type(),
                portal.faceIndex(),
                active
        );
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    public SpacePortalManager portalManager() {
        return portalManager;
    }

    public CubeCoordinateMapper coordinateMapper() {
        return coordinateMapper;
    }
}
