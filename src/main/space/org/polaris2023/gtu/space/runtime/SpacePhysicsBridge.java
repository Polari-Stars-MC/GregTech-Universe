package org.polaris2023.gtu.space.runtime;

import net.minecraft.server.level.ServerPlayer;
import org.polaris2023.gtu.space.runtime.ksp.KspSnapshot;

public interface SpacePhysicsBridge {
    SpacePhysicsInput readPlayerInput(ServerPlayer player, SpacePlayerState state, SpaceVesselState vessel, KspSnapshot snapshot);

    void publishPlayerState(ServerPlayer player, SpacePhysicsOutput output);
}
