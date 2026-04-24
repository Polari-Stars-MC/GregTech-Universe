package org.polaris2023.gtu.space.client.cosmic.ch;

import net.minecraft.core.Direction;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Direct geometry/UV port of Cosmic Horizons' TexturedCubeCuratedProcedure.
 */
public final class TexturedCubeCuratedProcedure {
    private TexturedCubeCuratedProcedure() {
    }

    public static Face[] execute() {
        return new Face[]{
                face(Direction.DOWN, new Vector2f[]{
                        new Vector2f(0.75F, 0.0F),
                        new Vector2f(0.75F, 0.25F),
                        new Vector2f(0.5F, 0.25F),
                        new Vector2f(0.5F, 0.0F)
                }),
                face(Direction.UP, new Vector2f[]{
                        new Vector2f(0.25F, 0.0F),
                        new Vector2f(0.25F, 0.25F),
                        new Vector2f(0.5F, 0.25F),
                        new Vector2f(0.5F, 0.0F)
                }),
                face(Direction.SOUTH, new Vector2f[]{
                        new Vector2f(0.25F, 0.25F),
                        new Vector2f(0.25F, 0.5F),
                        new Vector2f(0.5F, 0.5F),
                        new Vector2f(0.5F, 0.25F)
                }),
                face(Direction.EAST, new Vector2f[]{
                        new Vector2f(0.5F, 0.25F),
                        new Vector2f(0.5F, 0.5F),
                        new Vector2f(0.75F, 0.5F),
                        new Vector2f(0.75F, 0.25F)
                }),
                face(Direction.NORTH, new Vector2f[]{
                        new Vector2f(0.75F, 0.25F),
                        new Vector2f(0.75F, 0.5F),
                        new Vector2f(1.0F, 0.5F),
                        new Vector2f(1.0F, 0.25F)
                }),
                face(Direction.WEST, new Vector2f[]{
                        new Vector2f(0.0F, 0.25F),
                        new Vector2f(0.0F, 0.5F),
                        new Vector2f(0.25F, 0.5F),
                        new Vector2f(0.25F, 0.25F)
                })
        };
    }

    private static Face face(Direction side, Vector2f[] uvs) {
        return new Face(
                side,
                new Vector3f[]{
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(-0.5F, 0.5F, -0.5F)).mul(2.0F),
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(-0.5F, 0.5F, 0.5F)).mul(2.0F),
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(0.5F, 0.5F, 0.5F)).mul(2.0F),
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(0.5F, 0.5F, -0.5F)).mul(2.0F)
                },
                uvs
        );
    }

    public record Face(Direction side, Vector3f[] vertices, Vector2f[] uvs) {
    }
}
