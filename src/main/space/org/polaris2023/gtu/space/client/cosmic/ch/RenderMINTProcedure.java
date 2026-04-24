package org.polaris2023.gtu.space.client.cosmic.ch;

/**
 * CH-style render entry ordering port.
 * This keeps the layered submission order in one original-name entrypoint
 * instead of spreading the sequence across multiple renderers.
 */
public final class RenderMINTProcedure {
    private RenderMINTProcedure() {
    }

    public static void renderEarth(EarthLayerPass pass) {
        pass.renderSurface();
        pass.renderInverseTexture();
        pass.renderClouds();
        pass.renderAtmosphere();
    }

    public static void renderPlanet(PlanetLayerPass pass) {
        pass.renderSurface();
        if (pass.hasAtmosphere()) {
            pass.renderAtmosphere();
        }
        if (pass.hasRing()) {
            pass.renderRing();
        }
    }

    public static <S> void renderOrderedSides(S[] orderedSides, OrderedSideRenderer<S> renderer) {
        for (S side : orderedSides) {
            renderer.render(side);
        }
    }

    public static <S> void renderSubdividedSides(S[] orderedSides, int subdivisions, SubdividedSideRenderer<S> renderer) {
        float step = 2.0F / subdivisions;
        for (S side : orderedSides) {
            for (int y = 0; y < subdivisions; y++) {
                float v0 = -1.0F + y * step;
                float v1 = v0 + step;
                for (int x = 0; x < subdivisions; x++) {
                    float u0 = -1.0F + x * step;
                    float u1 = u0 + step;
                    renderer.render(side, u0, v0, u1, v1);
                }
            }
        }
    }

    public static void renderIndexed(int count, IndexedRenderer renderer) {
        for (int i = 0; i < count; i++) {
            renderer.render(i);
        }
    }

    public interface EarthLayerPass {
        void renderSurface();
        void renderInverseTexture();
        void renderClouds();
        void renderAtmosphere();
    }

    public interface PlanetLayerPass {
        void renderSurface();
        void renderAtmosphere();
        void renderRing();
        boolean hasAtmosphere();
        boolean hasRing();
    }

    public interface OrderedSideRenderer<S> {
        void render(S side);
    }

    public interface SubdividedSideRenderer<S> {
        void render(S side, float u0, float v0, float u1, float v1);
    }

    public interface IndexedRenderer {
        void render(int index);
    }
}
