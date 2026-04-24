package org.polaris2023.gtu.space.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.polaris2023.gtu.space.client.portal.ClientPortalCache;
import org.polaris2023.gtu.space.runtime.SpaceManager;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class SpaceNetwork {
    private SpaceNetwork() {
    }

    @SubscribeEvent
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
                    SpaceManager manager = SpaceManager.get(player.server);
                    manager.setLandingDescent(player, packet.descending());
                })
        );
    }
}
