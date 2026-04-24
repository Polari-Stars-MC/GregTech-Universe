package org.polaris2023.gtu.space.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.polaris2023.gtu.space.client.portal.ClientPortalCache;
import org.polaris2023.gtu.space.network.payload.KspDebugSnapshotPacket;
import org.polaris2023.gtu.space.network.payload.KspDebugSnapshotRequestPacket;
import org.polaris2023.gtu.space.network.payload.KspOverviewPacket;
import org.polaris2023.gtu.space.simulation.SpaceManager;

public final class SpaceNetwork {
    private SpaceNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                SpaceStateSyncPacket.TYPE,
                SpaceStateSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSpaceCache.updateState(packet))
        );
        registrar.playToClient(
                SpaceTransitionPacket.TYPE,
                SpaceTransitionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSpaceCache.updateTransition(packet))
        );
        registrar.playToClient(
                SpaceSnapshotPacket.TYPE,
                SpaceSnapshotPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSpaceCache.updateSnapshot(packet))
        );
        registrar.playToClient(
                SpaceSeamlessTeleportPacket.TYPE,
                SpaceSeamlessTeleportPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSpaceCache.updateSeamlessTeleport(packet))
        );
        registrar.playToClient(
                SpacePortalSyncPacket.TYPE,
                SpacePortalSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientPortalCache.updatePortal(packet.toSyncData()))
        );
        registrar.playToServer(
                SpaceLandingRequestPacket.TYPE,
                SpaceLandingRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    SpaceManager.get(player.server).setLandingDescent(player, packet.descending());
                })
        );

        registrar.playToClient(
                KspOverviewPacket.TYPE,
                KspOverviewPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> KspClientSyncState.updateOverview(packet))
        );
        registrar.playToClient(
                KspDebugSnapshotPacket.TYPE,
                KspDebugSnapshotPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> KspClientSyncState.updateDebugSnapshot(packet.snapshot()))
        );
        registrar.playToServer(
                KspDebugSnapshotRequestPacket.TYPE,
                KspDebugSnapshotRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        KspPacketSender.syncDebugSnapshot(player);
                    }
                })
        );
    }
}
