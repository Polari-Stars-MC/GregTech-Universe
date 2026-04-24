package org.polaris2023.gtu.space.client.cosmic.ch;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Direct 1.21.1 port of Cosmic Horizons' CubeVertexOrientorProcedure.
 */
public final class CHCubeVertexOrientorPort {
    private CHCubeVertexOrientorPort() {
    }

    public static Vector3f execute(Direction direction, double planeAngleDegrees, Vector3f vertex) {
        if (direction == null || vertex == null) {
            return new Vector3f();
        }
        Vector3f result = new Vector3f(vertex);
        float plane = (float) Math.toRadians(planeAngleDegrees);
        return switch (direction) {
            case UP -> result.rotateY(plane);
            case DOWN -> result.rotateY(plane + (float) Math.PI).rotateX((float) -Math.PI);
            case NORTH -> result.rotateY(plane).rotateX(-1.5707964F).rotateY(1.5707964F);
            case SOUTH -> result.rotateY(plane).rotateX(-1.5707964F).rotateY(4.712389F);
            case EAST -> result.rotateY(plane).rotateX(-1.5707964F).rotateY(0.0F);
            case WEST -> result.rotateY(plane).rotateX(-1.5707964F).rotateY((float) Math.PI);
        };
    }
}
