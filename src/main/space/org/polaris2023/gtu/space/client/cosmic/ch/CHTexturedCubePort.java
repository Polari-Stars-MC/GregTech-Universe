package org.polaris2023.gtu.space.client.cosmic.ch;

import net.minecraft.core.Direction;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Direct port of the static vertex/UV layout baked by Cosmic Horizons'
 * TexturedcubeProcedure and TexturedCubeCuratedProcedure.
 */
public final class CHTexturedCubePort {
    private CHTexturedCubePort() {
    }

    public static Face[] texturedCubeFaces() {
        return buildFaces(false);
    }

    public static Face[] curatedCubeFaces() {
        return buildFaces(true);
    }

    public static Face[] animatedCubeFaces() {
        Face[] base = buildFaces(true);
        Face[] animated = new Face[base.length];
        for (int i = 0; i < base.length; i++) {
            Face face = base[i];
            animated[i] = new Face(face.side(), face.vertices(), CHAnimatedLightCubeMapPort.execute(face.side()));
        }
        return animated;
    }

    public static float[][] ringUvs() {
        return new float[][]{
                {1.0F, 0.0F, 0.75F, 1.0F, 0.25F, 1.0F, 0.0F, 0.0F},
                {0.25F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.75F, 1.0F},
                {0.0F, 0.0F, 1.0F, 0.0F, 0.75F, 1.0F, 0.25F, 1.0F},
                {0.75F, 1.0F, 0.25F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F}
        };
    }

    private static Face[] buildFaces(boolean curated) {
        return new Face[]{
                face(Direction.DOWN, curated ? Direction.DOWN : Direction.DOWN, new Vector3f(-0.5F, 0.5F, -0.5F), 0.0),
                face(Direction.UP, curated ? Direction.UP : Direction.UP, new Vector3f(-0.5F, 0.5F, -0.5F), 0.0),
                face(Direction.SOUTH, curated ? Direction.SOUTH : Direction.SOUTH, new Vector3f(-0.5F, 0.5F, -0.5F), 0.0),
                face(Direction.EAST, curated ? Direction.EAST : Direction.EAST, new Vector3f(-0.5F, 0.5F, -0.5F), 0.0),
                face(Direction.NORTH, curated ? Direction.NORTH : Direction.NORTH, new Vector3f(-0.5F, 0.5F, -0.5F), 0.0),
                face(Direction.WEST, curated ? Direction.WEST : Direction.WEST, new Vector3f(-0.5F, 0.5F, -0.5F), 0.0)
        };
    }

    private static Face face(Direction side, Direction uvSide, Vector3f seed, double planeAngle) {
        Vector3f[] vertices = new Vector3f[]{
                CHCubeVertexOrientorPort.execute(side, planeAngle, new Vector3f(-0.5F, 0.5F, -0.5F)).mul(2.0F),
                CHCubeVertexOrientorPort.execute(side, planeAngle, new Vector3f(-0.5F, 0.5F, 0.5F)).mul(2.0F),
                CHCubeVertexOrientorPort.execute(side, planeAngle, new Vector3f(0.5F, 0.5F, 0.5F)).mul(2.0F),
                CHCubeVertexOrientorPort.execute(side, planeAngle, new Vector3f(0.5F, 0.5F, -0.5F)).mul(2.0F)
        };
        Vector2f[] uvs = CHLightCubeMapPort.execute(uvSide);
        return new Face(side, vertices, uvs);
    }

    public record Face(Direction side, Vector3f[] vertices, Vector2f[] uvs) {
    }
}
