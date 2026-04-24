package org.polaris2023.gtu.space.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.polaris2023.gtu.space.network.payload.KspDebugSnapshotPacket;
import org.polaris2023.gtu.space.network.payload.KspOverviewPacket;
import org.polaris2023.gtu.space.simulation.SpaceManager;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;

public final class KspPacketSender {
    private KspPacketSender() {
    }

    public static void syncOverview(ServerPlayer player) {
        KspSnapshot snapshot = SpaceManager.get(player.server).latestKspSnapshot();
        if (snapshot == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, KspOverviewPacket.fromSnapshot(snapshot));
    }

    public static void syncDebugSnapshot(ServerPlayer player) {
        KspSnapshot snapshot = SpaceManager.get(player.server).latestKspSnapshot();
        if (snapshot == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, KspDebugSnapshotPacket.fromSnapshot(snapshot));
    }
}

