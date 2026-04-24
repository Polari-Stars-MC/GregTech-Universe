package org.polaris2023.gtu.space.client.cosmic.ch;

import net.minecraft.core.Direction;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Direct geometry/UV port of Cosmic Horizons' TexturedcubeProcedure.
 */
public final class TexturedcubeProcedure {
    private TexturedcubeProcedure() {
    }

    public static Face[] execute() {
        return new Face[]{
                face(Direction.DOWN),
                face(Direction.UP),
                face(Direction.SOUTH),
                face(Direction.EAST),
                face(Direction.NORTH),
                face(Direction.WEST)
        };
    }

    public static float[][] ringUvs() {
        return new float[][]{
                {1.0F, 0.0F, 0.75F, 1.0F, 0.25F, 1.0F, 0.0F, 0.0F},
                {0.25F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.75F, 1.0F},
                {0.0F, 0.0F, 1.0F, 0.0F, 0.75F, 1.0F, 0.25F, 1.0F},
                {0.75F, 1.0F, 0.25F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F}
        };
    }

    private static Face face(Direction side) {
        return new Face(
                side,
                new Vector3f[]{
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(-0.5F, 0.5F, -0.5F)).mul(2.0F),
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(-0.5F, 0.5F, 0.5F)).mul(2.0F),
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(0.5F, 0.5F, 0.5F)).mul(2.0F),
                        CubeVertexOrientorProcedure.execute(side, 0.0, new Vector3f(0.5F, 0.5F, -0.5F)).mul(2.0F)
                },
                LightCubeMapProcedure.execute(side)
        );
    }

    public record Face(Direction side, Vector3f[] vertices, Vector2f[] uvs) {
    }
}
