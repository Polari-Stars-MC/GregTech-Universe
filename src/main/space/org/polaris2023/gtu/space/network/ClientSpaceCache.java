package org.polaris2023.gtu.space.network;

public final class ClientSpaceCache {
    private static final int SEAMLESS_COMPOSITOR_HOLD_FRAMES = 2;

    private static volatile SpaceStateSyncPacket state;
    private static volatile SpaceTransitionPacket transition = SpaceTransitionPacket.inactive(0L);
    private static volatile SpaceSnapshotPacket snapshot;
    private static volatile long lastTransitionReceiveNanos;
    private static PendingSeamlessTeleport pendingSeamlessTeleport;

    private ClientSpaceCache() {
    }

    public static void updateState(SpaceStateSyncPacket packet) {
        state = packet;
    }

    public static void updateTransition(SpaceTransitionPacket packet) {
        transition = packet;
        lastTransitionReceiveNanos = System.nanoTime();
    }

    public static void updateSnapshot(SpaceSnapshotPacket packet) {
        snapshot = packet;
    }

    public static synchronized void updateSeamlessTeleport(SpaceSeamlessTeleportPacket packet) {
        pendingSeamlessTeleport = new PendingSeamlessTeleport(packet);
    }

    public static SpaceStateSyncPacket state() {
        return state;
    }

    public static SpaceTransitionPacket transition() {
        return transition;
    }

    public static SpaceSnapshotPacket snapshot() {
        return snapshot;
    }

    public static boolean hasSpaceState() {
        return state != null;
    }

    public static double estimatedServerTickExact() {
        SpaceTransitionPacket packet = transition;
        if (packet == null) {
            return 0.0;
        }
        if (lastTransitionReceiveNanos == 0L) {
            return packet.serverTick();
        }
        double elapsedTicks = Math.max(0.0, (System.nanoTime() - lastTransitionReceiveNanos) / 50_000_000.0);
        return packet.serverTick() + elapsedTicks;
    }

    public static long estimatedServerTick() {
        return Math.max(0L, Math.round(estimatedServerTickExact()));
    }

    public static synchronized boolean prepareSeamlessRespawn(String targetDimension) {
        PendingSeamlessTeleport pending = activePending();
        if (pending == null) {
            return false;
        }
        if (!pending.packet.targetDimension().equals(targetDimension)) {
            pendingSeamlessTeleport = null;
            return false;
        }
        pending.armed = true;
        return true;
    }

    public static synchronized boolean shouldSuppressLoadingScreen() {
        PendingSeamlessTeleport pending = activePending();
        return pending != null && pending.armed;
    }

    public static synchronized boolean shouldSmoothRespawn() {
        PendingSeamlessTeleport pending = activePending();
        return pending != null && pending.armed;
    }

    public static synchronized SpaceSeamlessTeleportPacket activeSeamlessTeleport() {
        PendingSeamlessTeleport pending = activePending();
        if (pending == null || !pending.armed) {
            return null;
        }
        return pending.packet;
    }

    public static synchronized void markSeamlessRespawnApplied() {
        PendingSeamlessTeleport pending = activePending();
        if (pending == null) {
            return;
        }
        pending.poseApplied = true;
    }

    public static synchronized void finishSeamlessRespawn(String targetDimension) {
        PendingSeamlessTeleport pending = activePending();
        if (pending == null || !pending.packet.targetDimension().equals(targetDimension)) {
            return;
        }
        pending.armed = false;
        pending.poseApplied = true;
        pending.compositorHoldFrames = SEAMLESS_COMPOSITOR_HOLD_FRAMES;
    }

    public static synchronized boolean consumeSeamlessCompositorHold(String currentDimension) {
        PendingSeamlessTeleport pending = activePending();
        if (pending == null || !pending.packet.targetDimension().equals(currentDimension)) {
            return false;
        }
        if (pending.compositorHoldFrames <= 0) {
            if (!pending.armed) {
                pendingSeamlessTeleport = null;
            }
            return false;
        }
        pending.compositorHoldFrames--;
        if (pending.compositorHoldFrames <= 0 && !pending.armed) {
            pendingSeamlessTeleport = null;
        }
        return true;
    }

    private static PendingSeamlessTeleport activePending() {
        PendingSeamlessTeleport pending = pendingSeamlessTeleport;
        if (pending == null) {
            return null;
        }
        if (pending.expireServerTick < estimatedServerTick()) {
            pendingSeamlessTeleport = null;
            return null;
        }
        return pending;
    }

    private static final class PendingSeamlessTeleport {
        private final SpaceSeamlessTeleportPacket packet;
        private final long expireServerTick;
        private boolean armed;
        private boolean poseApplied;
        private int compositorHoldFrames;

        private PendingSeamlessTeleport(SpaceSeamlessTeleportPacket packet) {
            this.packet = packet;
            this.expireServerTick = packet.expireServerTick();
        }
    }
}
