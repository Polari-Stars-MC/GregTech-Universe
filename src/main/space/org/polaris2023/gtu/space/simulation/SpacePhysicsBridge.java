package org.polaris2023.gtu.space.simulation;

import net.minecraft.server.level.ServerPlayer;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;

public interface SpacePhysicsBridge {
    SpacePhysicsInput readPlayerInput(ServerPlayer player, SpacePlayerState state, SpaceVesselState vessel, KspSnapshot snapshot);

    void publishPlayerState(ServerPlayer player, SpacePhysicsOutput output);
}
