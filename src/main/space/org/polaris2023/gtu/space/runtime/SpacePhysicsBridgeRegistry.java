package org.polaris2023.gtu.space.runtime;

import net.minecraft.server.level.ServerPlayer;
import org.polaris2023.gtu.space.runtime.ksp.KspSnapshot;

public final class SpacePhysicsBridgeRegistry {
    private static final SpacePhysicsBridge NO_OP = new SpacePhysicsBridge() {
        @Override
        public SpacePhysicsInput readPlayerInput(ServerPlayer player, SpacePlayerState state, SpaceVesselState vessel, KspSnapshot snapshot) {
            return SpacePhysicsInput.idle();
        }

        @Override
        public void publishPlayerState(ServerPlayer player, SpacePhysicsOutput output) {
        }
    };

    private static volatile SpacePhysicsBridge bridge = NO_OP;

    private SpacePhysicsBridgeRegistry() {
    }

    public static SpacePhysicsBridge bridge() {
        return bridge;
    }

    public static void setBridge(SpacePhysicsBridge nextBridge) {
        bridge = nextBridge == null ? NO_OP : nextBridge;
    }
}
