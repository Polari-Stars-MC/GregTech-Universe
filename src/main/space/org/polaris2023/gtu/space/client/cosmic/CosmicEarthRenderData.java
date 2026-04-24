package org.polaris2023.gtu.space.client.cosmic;

import java.util.EnumMap;
import java.util.Map;

/**
 * CH-style render payload names kept intentionally close to the original mod:
 * light_data / alpha_data / i_alpha_data / cloud_data.
 */
public record CosmicEarthRenderData(
        Map<CosmicEarthRenderer.CubeSide, VertexColorSet> light_data,
        Map<CosmicEarthRenderer.CubeSide, VertexAlphaSet> alpha_data,
        Map<CosmicEarthRenderer.CubeSide, VertexAlphaSet> i_alpha_data,
        CloudData cloud_data
) {
    public static CosmicEarthRenderData createDefault() {
        EnumMap<CosmicEarthRenderer.CubeSide, VertexColorSet> light = new EnumMap<>(CosmicEarthRenderer.CubeSide.class);
        EnumMap<CosmicEarthRenderer.CubeSide, VertexAlphaSet> alpha = new EnumMap<>(CosmicEarthRenderer.CubeSide.class);
        EnumMap<CosmicEarthRenderer.CubeSide, VertexAlphaSet> inverse = new EnumMap<>(CosmicEarthRenderer.CubeSide.class);
        for (CosmicEarthRenderer.CubeSide side : CosmicEarthRenderer.CubeSide.values()) {
            light.put(side, VertexColorSet.defaultFor(side));
            alpha.put(side, VertexAlphaSet.defaultAtmosphere(side));
            inverse.put(side, VertexAlphaSet.defaultInverse(side));
        }
        return new CosmicEarthRenderData(light, alpha, inverse, new CloudData(2, 0.76F, 0.76F, 0.76F, 1.0F));
    }

    public record VertexColorSet(int c0, int c1, int c2, int c3) {
        private static VertexColorSet defaultFor(CosmicEarthRenderer.CubeSide side) {
            return switch (side) {
                case UP -> new VertexColorSet(0xFFF6FAFF, 0xFFF6FAFF, 0xFFF6FAFF, 0xFFF6FAFF);
                case DOWN -> new VertexColorSet(0xFF5A6880, 0xFF5A6880, 0xFF5A6880, 0xFF5A6880);
                case NORTH, EAST -> new VertexColorSet(0xFFE0E8FF, 0xFFE0E8FF, 0xFFE0E8FF, 0xFFE0E8FF);
                case SOUTH, WEST -> new VertexColorSet(0xFFD0DAF2, 0xFFD0DAF2, 0xFFD0DAF2, 0xFFD0DAF2);
            };
        }
    }

    public record VertexAlphaSet(float a0, float a1, float a2, float a3) {
        private static VertexAlphaSet defaultAtmosphere(CosmicEarthRenderer.CubeSide side) {
            return switch (side) {
                case UP -> new VertexAlphaSet(0.30F, 0.30F, 0.30F, 0.30F);
                case DOWN -> new VertexAlphaSet(0.08F, 0.08F, 0.08F, 0.08F);
                default -> new VertexAlphaSet(0.18F, 0.18F, 0.18F, 0.18F);
            };
        }

        private static VertexAlphaSet defaultInverse(CosmicEarthRenderer.CubeSide side) {
            return switch (side) {
                case NORTH, EAST -> new VertexAlphaSet(0.82F, 0.82F, 0.82F, 0.82F);
                case SOUTH, WEST -> new VertexAlphaSet(0.66F, 0.66F, 0.66F, 0.66F);
                case UP -> new VertexAlphaSet(0.55F, 0.55F, 0.55F, 0.55F);
                case DOWN -> new VertexAlphaSet(0.38F, 0.38F, 0.38F, 0.38F);
            };
        }
    }

    public record CloudData(int tick_delay, float r, float g, float b, float alpha) {
    }
}
