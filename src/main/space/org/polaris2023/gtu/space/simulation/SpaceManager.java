package org.polaris2023.gtu.space.simulation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.polaris2023.gtu.space.simulation.ksp.KspBackgroundSystem;
import org.polaris2023.gtu.space.simulation.ksp.KspSaveData;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;
import org.polaris2023.gtu.space.simulation.ksp.KspSystemDefinition;
import org.polaris2023.gtu.space.simulation.math.SpaceCoordinate;
import org.polaris2023.gtu.space.simulation.math.SpaceVector;
import org.polaris2023.gtu.space.simulation.math.UniverseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceManager.class);
    private static final LevelResource KSP_RESOURCE = new LevelResource("gtu_ksp");
    private static final String SAVE_FILE = "ksp_state.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SpaceManager INSTANCES = null;

    private final MinecraftServer server;
    private final SpacePhysicsEngine physicsEngine = new SpacePhysicsEngine();
    private final KspBackgroundSystem kspSystem = new KspBackgroundSystem(KspSystemDefinition.solarSystem());
    private final Map<UUID, SpacePlayerState> playerStates = new ConcurrentHashMap<>();

    private SpaceManager(MinecraftServer server) {
        this.server = server;
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
        physicsEngine.stepSimulation(1.0f / 20.0f);
        kspSystem.tick();
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

    public SpacePlayerState getOrCreate(ServerPlayer player) {
        return playerStates.computeIfAbsent(player.getUUID(), ignored ->
                new SpacePlayerState(
                        SpacePlayerState.Mode.PLANET,
                        player.level().dimension().location().toString(),
                        currentUniverseId(),
                        currentGalaxyId(),
                        currentSystemId(),
                        currentUniverseAnchor(),
                        SpaceCoordinate.origin(),
                        null
                )
        );
    }

    public SpacePlayerState enterSpace(ServerPlayer player) {
        ServerLevel targetLevel = player.server.overworld();
        player.teleportTo(targetLevel, 0.5, 64.0, 0.5, player.getYRot(), player.getXRot());

        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.SPACE,
                targetLevel.dimension().location().toString(),
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                UUID.randomUUID()
        );
        playerStates.put(player.getUUID(), next);
        return next;
    }

    public SpacePlayerState leaveSpace(ServerPlayer player, ServerLevel targetLevel, double x, double y, double z) {
        player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());

        SpacePlayerState next = new SpacePlayerState(
                SpacePlayerState.Mode.PLANET,
                targetLevel.dimension().location().toString(),
                currentUniverseId(),
                currentGalaxyId(),
                currentSystemId(),
                currentUniverseAnchor(),
                SpaceCoordinate.origin(),
                null
        );
        playerStates.put(player.getUUID(), next);
        return next;
    }

    public void save() {
        try {
            Path dir = server.getWorldPath(KSP_RESOURCE);
            Files.createDirectories(dir);
            Path file = dir.resolve(SAVE_FILE);
            KspSaveData data = kspSystem.exportState();
            data.playerStates = exportPlayerStates();
            Files.writeString(file, GSON.toJson(data));
            LOGGER.info("KSP state saved to {}", file);
        } catch (IOException e) {
            LOGGER.error("Failed to save KSP state", e);
        }
    }

    public void load() {
        Path file = server.getWorldPath(KSP_RESOURCE).resolve(SAVE_FILE);
        if (!Files.exists(file)) {
            LOGGER.info("No KSP save file found, starting fresh");
            return;
        }
        try {
            KspSaveData data = GSON.fromJson(Files.readString(file), KspSaveData.class);
            kspSystem.importState(data);
            importPlayerStates(data.playerStates);
            LOGGER.info("KSP state loaded from {}", file);
        } catch (IOException e) {
            LOGGER.error("Failed to load KSP state", e);
        }
    }

    private Map<String, KspSaveData.PlayerStateData> exportPlayerStates() {
        Map<String, KspSaveData.PlayerStateData> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, SpacePlayerState> entry : playerStates.entrySet()) {
            SpacePlayerState ps = entry.getValue();
            KspSaveData.PlayerStateData psd = new KspSaveData.PlayerStateData();
            psd.mode = ps.mode().name();
            psd.anchorDimension = ps.anchorDimension();
            psd.universeId = ps.universeId();
            psd.galaxyId = ps.galaxyId();
            psd.systemId = ps.systemId();
            UniverseVector uv = ps.universeAnchorPosition();
            psd.universeAnchorPosition = new String[]{
                    uv.x().toPlainString(), uv.y().toPlainString(), uv.z().toPlainString()
            };
            psd.coordinate = new double[]{
                    ps.coordinate().sectorX(), ps.coordinate().sectorY(), ps.coordinate().sectorZ(),
                    ps.coordinate().local().x(), ps.coordinate().local().y(), ps.coordinate().local().z()
            };
            psd.vesselId = ps.vesselId() != null ? ps.vesselId().toString() : null;
            result.put(entry.getKey().toString(), psd);
        }
        return result;
    }

    private void importPlayerStates(Map<String, KspSaveData.PlayerStateData> data) {
        if (data == null) return;
        for (Map.Entry<String, KspSaveData.PlayerStateData> entry : data.entrySet()) {
            UUID uuid = UUID.fromString(entry.getKey());
            KspSaveData.PlayerStateData psd = entry.getValue();
            UniverseVector uv = psd.universeAnchorPosition != null
                    ? new UniverseVector(
                    new java.math.BigDecimal(psd.universeAnchorPosition[0]),
                    new java.math.BigDecimal(psd.universeAnchorPosition[1]),
                    new java.math.BigDecimal(psd.universeAnchorPosition[2]))
                    : UniverseVector.zero();
            SpaceCoordinate coord = psd.coordinate != null && psd.coordinate.length >= 6
                    ? new SpaceCoordinate(
                    (long) psd.coordinate[0], (long) psd.coordinate[1], (long) psd.coordinate[2],
                    new SpaceVector(psd.coordinate[3], psd.coordinate[4], psd.coordinate[5]))
                    : SpaceCoordinate.origin();
            UUID vesselId = psd.vesselId != null ? UUID.fromString(psd.vesselId) : null;
            SpacePlayerState ps = new SpacePlayerState(
                    SpacePlayerState.Mode.valueOf(psd.mode),
                    psd.anchorDimension,
                    psd.universeId,
                    psd.galaxyId,
                    psd.systemId,
                    uv,
                    coord,
                    vesselId
            );
            playerStates.put(uuid, ps);
        }
    }

    private void close() {
        kspSystem.close();
        physicsEngine.close();
        playerStates.clear();
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
}

