package org.polaris2023.gtu.space.network;

import org.polaris2023.gtu.space.network.payload.KspOverviewPacket;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;

import java.util.concurrent.atomic.AtomicReference;

public final class KspClientSyncState {
    private static final AtomicReference<KspOverviewPacket> OVERVIEW = new AtomicReference<>();
    private static final AtomicReference<KspSnapshot> DEBUG_SNAPSHOT = new AtomicReference<>();

    private KspClientSyncState() {
    }

    public static void updateOverview(KspOverviewPacket packet) {
        OVERVIEW.set(packet);
    }

    public static KspOverviewPacket latestOverview() {
        return OVERVIEW.get();
    }

    public static void updateDebugSnapshot(KspSnapshot snapshot) {
        DEBUG_SNAPSHOT.set(snapshot);
    }

    public static KspSnapshot latestDebugSnapshot() {
        return DEBUG_SNAPSHOT.get();
    }

    public static void clear() {
        OVERVIEW.set(null);
        DEBUG_SNAPSHOT.set(null);
    }
}

