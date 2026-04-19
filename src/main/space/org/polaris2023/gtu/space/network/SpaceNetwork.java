package org.polaris2023.gtu.space.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.polaris2023.gtu.space.network.payload.KspDebugSnapshotPacket;
import org.polaris2023.gtu.space.network.payload.KspDebugSnapshotRequestPacket;
import org.polaris2023.gtu.space.network.payload.KspOverviewPacket;

public final class SpaceNetwork {
    private SpaceNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

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
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        KspPacketSender.syncDebugSnapshot(serverPlayer);
                    }
                })
        );
    }
}
