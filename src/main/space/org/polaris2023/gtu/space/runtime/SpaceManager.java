package org.polaris2023.gtu.space.runtime;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.polaris2023.gtu.space.runtime.ksp.KspBackgroundSystem;
import org.polaris2023.gtu.space.runtime.ksp.KspSnapshot;
import org.polaris2023.gtu.space.runtime.ksp.KspSystemDefinition;
import org.polaris2023.gtu.space.runtime.math.SpaceCoordinate;
import org.polaris2023.gtu.space.runtime.math.UniverseVector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceManager {
    private static SpaceManager INSTANCES = null;

    private final SpacePhysicsEngine physicsEngine = new SpacePhysicsEngine();
    private final KspBackgroundSystem kspSystem = new KspBackgroundSystem(KspSystemDefinition.solarSystem());
    private final Map<UUID, SpacePlayerState> playerStates = new ConcurrentHashMap<>();

    private SpaceManager() {
    }

    public static SpaceManager get(MinecraftServer server) {
        if (INSTANCES == null) {
            INSTANCES = new SpaceManager();
        }
        return INSTANCES;
    }

    public static void shutdown(MinecraftServer server) {
        if (INSTANCES != null) {
            INSTANCES.close();
        }
        INSTANCES = null;
    }

    public void startSystems() {
        kspSystem.ensureStarted();
    }

    public void tick() {
        physicsEngine.stepSimulation(1.0f / 20.0f);
    }

    public KspSnapshot latestKspSnapshot() {
        return kspSystem.latestSnapshot();
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
