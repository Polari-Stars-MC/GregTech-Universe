package org.polaris2023.gtu.space.portal.impl;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.polaris2023.gtu.space.network.SpaceTransitionPacket;
import org.polaris2023.gtu.space.portal.ISyncManager;
import org.polaris2023.gtu.space.portal.TransitionState;
import org.polaris2023.gtu.space.runtime.SpaceDomain;
import org.polaris2023.gtu.space.runtime.SpaceTransitionDirection;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceSyncManager implements ISyncManager {
    private final ConcurrentHashMap<UUID, SyncEntry> syncEntries = new ConcurrentHashMap<>();
    private final MinecraftServer server;

    public SpaceSyncManager(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void broadcastTransitionStart(TransitionState transition) {
        ServerPlayer player = server.getPlayerList().getPlayer(transition.playerId());
        if (player == null) {
            return;
        }
        syncEntries.put(transition.playerId(), new SyncEntry(
                player.position(),
                player.position(),
                transition
        ));
    }

    @Override
    public void broadcastTransitionProgress(TransitionState transition) {
        SyncEntry entry = syncEntries.get(transition.playerId());
        if (entry == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(transition.playerId());
        if (player != null) {
            syncEntries.put(transition.playerId(), new SyncEntry(
                    entry.sourcePosition(),
                    player.position(),
                    transition
            ));
        }
    }

    @Override
    public void broadcastTransitionComplete(TransitionState transition) {
        syncEntries.remove(transition.playerId());
    }

    @Override
    public void broadcastTransitionCancellation(UUID playerId, String reason) {
        syncEntries.remove(playerId);
    }

    @Override
    public Vec3 interpolatePlayerPosition(UUID playerId, float partialTick) {
        SyncEntry entry = syncEntries.get(playerId);
        if (entry == null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            return player != null ? player.position() : Vec3.ZERO;
        }
        double progress = entry.transition().progress();
        double t = Math.clamp(progress + partialTick * 0.05, 0.0, 1.0);
        return entry.sourcePosition().lerp(entry.targetPosition(), t);
    }

    public void clear() {
        syncEntries.clear();
    }

    private record SyncEntry(
            Vec3 sourcePosition,
            Vec3 targetPosition,
            TransitionState transition
    ) {
    }
}
