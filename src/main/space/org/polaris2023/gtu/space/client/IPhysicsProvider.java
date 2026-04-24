package org.polaris2023.gtu.space.client;

import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

public interface IPhysicsProvider {
    Vector3d getPlayerPosition();

    double getPlayerAltitude();

    boolean isInSpace(Player player);

    Vector3d getGravityDirection();
}
