package org.polaris2023.gtu.space.client.cosmic.ch;

import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.client.cosmic.CosmicEarthRenderData;
import org.polaris2023.gtu.space.client.cosmic.CosmicEarthRenderer;

import java.util.EnumMap;

/**
 * 1.21.1-oriented port of the data flow in InitialiseRenderSequenceProcedure.
 * It preserves the CH loop structure over Direction.values() and produces
 * light_data / alpha_data / i_alpha_data / cloud_data for the Earth cube.
 */
public final class CHInitialiseRenderSequencePort {
    private CHInitialiseRenderSequencePort() {
    }

    public static CosmicEarthRenderData build(Vector3f lightLocal, Vector3f viewLocal) {
        EnumMap<CosmicEarthRenderer.CubeSide, CosmicEarthRenderData.VertexColorSet> lightData = new EnumMap<>(CosmicEarthRenderer.CubeSide.class);
        EnumMap<CosmicEarthRenderer.CubeSide, CosmicEarthRenderData.VertexAlphaSet> alphaData = new EnumMap<>(CosmicEarthRenderer.CubeSide.class);
        EnumMap<CosmicEarthRenderer.CubeSide, CosmicEarthRenderData.VertexAlphaSet> inverseAlphaData = new EnumMap<>(CosmicEarthRenderer.CubeSide.class);

        double counter = 0.0;
        for (Direction direction : Direction.values()) {
            Vector3f directionVector = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            Vector3f vertex1 = CHCubeVertexOrientorPort.execute(direction, 0.0, new Vector3f(-0.5F, 0.5F, -0.5F));
            Vector3f vertex2 = CHCubeVertexOrientorPort.execute(direction, 0.0, new Vector3f(-0.5F, 0.5F, 0.5F));
            Vector3f vertex3 = CHCubeVertexOrientorPort.execute(direction, 0.0, new Vector3f(0.5F, 0.5F, 0.5F));
            Vector3f vertex4 = CHCubeVertexOrientorPort.execute(direction, 0.0, new Vector3f(0.5F, 0.5F, -0.5F));

            int c0 = brightnessColor(directionVector, lightLocal, viewLocal, vertex1);
            int c1 = brightnessColor(directionVector, lightLocal, viewLocal, vertex2);
            int c2 = brightnessColor(directionVector, lightLocal, viewLocal, vertex3);
            int c3 = brightnessColor(directionVector, lightLocal, viewLocal, vertex4);

            float a0 = alphaValue(directionVector, lightLocal, viewLocal, vertex1, false);
            float a1 = alphaValue(directionVector, lightLocal, viewLocal, vertex2, false);
            float a2 = alphaValue(directionVector, lightLocal, viewLocal, vertex3, false);
            float a3 = alphaValue(directionVector, lightLocal, viewLocal, vertex4, false);

            float ia0 = alphaValue(directionVector, lightLocal, viewLocal, vertex1, true);
            float ia1 = alphaValue(directionVector, lightLocal, viewLocal, vertex2, true);
            float ia2 = alphaValue(directionVector, lightLocal, viewLocal, vertex3, true);
            float ia3 = alphaValue(directionVector, lightLocal, viewLocal, vertex4, true);

            CosmicEarthRenderer.CubeSide side = mapSide(direction);
            lightData.put(side, new CosmicEarthRenderData.VertexColorSet(c0, c1, c2, c3));
            alphaData.put(side, new CosmicEarthRenderData.VertexAlphaSet(a0, a1, a2, a3));
            inverseAlphaData.put(side, new CosmicEarthRenderData.VertexAlphaSet(ia0, ia1, ia2, ia3));
            counter = clamp((float) (counter + 1.0), 0.0F, 5.0F);
        }

        return new CosmicEarthRenderData(
                lightData,
                alphaData,
                inverseAlphaData,
                new CosmicEarthRenderData.CloudData(2, 0.82F, 0.84F, 0.86F, 0.95F)
        );
    }

    private static int brightnessColor(Vector3f directionVector, Vector3f lightLocal, Vector3f viewLocal, Vector3f vertex) {
        Vector3f normal = new Vector3f(vertex).normalize();
        float faceLight = clamp(normal.dot(lightLocal), 0.0F, 1.0F);
        float faceView = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float directional = clamp(directionVector.normalize(new Vector3f()).dot(lightLocal) * 0.5F + 0.5F, 0.0F, 1.0F);
        float red = 0.68F + faceLight * 0.24F + directional * 0.08F;
        float green = 0.72F + faceLight * 0.20F + directional * 0.08F;
        float blue = 0.80F + faceLight * 0.14F + (1.0F - faceView) * 0.06F;
        return argb(1.0F, red, green, blue);
    }

    private static float alphaValue(Vector3f directionVector, Vector3f lightLocal, Vector3f viewLocal, Vector3f vertex, boolean inverse) {
        Vector3f normal = new Vector3f(vertex).normalize();
        float faceLight = clamp(normal.dot(lightLocal), 0.0F, 1.0F);
        float faceView = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float directional = clamp(directionVector.normalize(new Vector3f()).dot(viewLocal) * 0.5F + 0.5F, 0.0F, 1.0F);
        if (inverse) {
            return clamp(0.22F + (1.0F - faceLight) * 0.52F + (1.0F - directional) * 0.12F, 0.0F, 1.0F);
        }
        return clamp(0.08F + (1.0F - faceView) * 0.22F + faceLight * 0.08F + directional * 0.04F, 0.0F, 1.0F);
    }

    private static CosmicEarthRenderer.CubeSide mapSide(Direction direction) {
        return switch (direction) {
            case NORTH -> CosmicEarthRenderer.CubeSide.NORTH;
            case SOUTH -> CosmicEarthRenderer.CubeSide.SOUTH;
            case EAST -> CosmicEarthRenderer.CubeSide.EAST;
            case WEST -> CosmicEarthRenderer.CubeSide.WEST;
            case UP -> CosmicEarthRenderer.CubeSide.UP;
            case DOWN -> CosmicEarthRenderer.CubeSide.DOWN;
        };
    }

    private static int argb(float alpha, float red, float green, float blue) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        int r = Math.max(0, Math.min(255, Math.round(clamp(red, 0.0F, 1.0F) * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(clamp(green, 0.0F, 1.0F) * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(clamp(blue, 0.0F, 1.0F) * 255.0F)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
