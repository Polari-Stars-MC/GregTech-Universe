package org.polaris2023.gtu.space.client.cosmic.ch;

import net.minecraft.core.Direction;
import org.joml.Vector2f;

/**
 * Direct 1.21.1 port of Cosmic Horizons' LightCubeMapProcedure.
 */
public final class CHLightCubeMapPort {
    private CHLightCubeMapPort() {
    }

    public static Vector2f[] execute(Direction direction) {
        if (direction == null) {
            return new Vector2f[0];
        }
        return switch (direction) {
            case UP -> new Vector2f[]{
                    new Vector2f(0.125F, 0.0F),
                    new Vector2f(0.125F, 0.25F),
                    new Vector2f(0.25F, 0.25F),
                    new Vector2f(0.25F, 0.0F)
            };
            case DOWN -> new Vector2f[]{
                    new Vector2f(0.375F, 0.0F),
                    new Vector2f(0.375F, 0.25F),
                    new Vector2f(0.25F, 0.25F),
                    new Vector2f(0.25F, 0.0F)
            };
            case NORTH -> new Vector2f[]{
                    new Vector2f(0.25F, 0.25F),
                    new Vector2f(0.25F, 0.5F),
                    new Vector2f(0.375F, 0.5F),
                    new Vector2f(0.375F, 0.25F)
            };
            case SOUTH -> new Vector2f[]{
                    new Vector2f(0.0F, 0.25F),
                    new Vector2f(0.0F, 0.5F),
                    new Vector2f(0.125F, 0.5F),
                    new Vector2f(0.125F, 0.25F)
            };
            case WEST -> new Vector2f[]{
                    new Vector2f(0.375F, 0.25F),
                    new Vector2f(0.375F, 0.5F),
                    new Vector2f(0.5F, 0.5F),
                    new Vector2f(0.5F, 0.25F)
            };
            case EAST -> new Vector2f[]{
                    new Vector2f(0.125F, 0.25F),
                    new Vector2f(0.125F, 0.5F),
                    new Vector2f(0.25F, 0.5F),
                    new Vector2f(0.25F, 0.25F)
            };
        };
    }
}
