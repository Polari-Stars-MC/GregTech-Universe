package org.polaris2023.gtu.space.client.cosmic;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.client.cosmic.ch.RenderMINTProcedure;
import org.polaris2023.gtu.space.client.cosmic.ch.Ring1Procedure;
import org.polaris2023.gtu.space.client.cosmic.ch.Ring2Procedure;
import org.polaris2023.gtu.space.client.cosmic.ch.Ring3Procedure;
import org.polaris2023.gtu.space.client.cosmic.ch.Ring4Procedure;
import org.polaris2023.gtu.space.client.cosmic.ch.TexturedcubeProcedure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CH-style textured cube renderer for non-earth planets.
 * The face vertex order, atlas UV layout and ring quads are ported from
 * TexturedcubeProcedure / Ring1~4Procedure instead of using sphere reprojection.
 */
public final class CosmicPlanetRenderer {
    private static final Vector3f KEY_LIGHT_DIRECTION = new Vector3f(-0.35F, 0.74F, 0.58F).normalize();
    private static final Map<String, PlanetVisualConfig> CONFIGS = Map.of(
            "moon", new PlanetVisualConfig(false, false),
            "mercury", new PlanetVisualConfig(false, false),
            "venus", new PlanetVisualConfig(true, false),
            "mars", new PlanetVisualConfig(true, false),
            "jupiter", new PlanetVisualConfig(true, false),
            "saturn", new PlanetVisualConfig(true, true),
            "uranus", new PlanetVisualConfig(true, false),
            "neptune", new PlanetVisualConfig(true, false)
    );
    private static final ChFace[] CH_FACES = mapFaces(TexturedcubeProcedure.execute());
    private static final Map<String, PlanetTextureSet> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final RingFace[] CH_RINGS = {
            mapRing(Ring1Procedure.execute(), 0.52F),
            mapRing(Ring2Procedure.execute(), 0.60F),
            mapRing(Ring3Procedure.execute(), 0.56F),
            mapRing(Ring4Procedure.execute(), 0.50F)
    };

    private CosmicPlanetRenderer() {
    }

    private static ChFace[] mapFaces(TexturedcubeProcedure.Face[] faces) {
        ChFace[] mapped = new ChFace[faces.length];
        for (int i = 0; i < faces.length; i++) {
            TexturedcubeProcedure.Face face = faces[i];
            mapped[i] = new ChFace(mapSide(face.side()), face.vertices(), face.uvs());
        }
        return mapped;
    }

    private static CubeSide mapSide(Direction direction) {
        return switch (direction) {
            case NORTH -> CubeSide.NORTH;
            case SOUTH -> CubeSide.SOUTH;
            case EAST -> CubeSide.EAST;
            case WEST -> CubeSide.WEST;
            case UP -> CubeSide.UP;
            case DOWN -> CubeSide.DOWN;
        };
    }

    private static RingFace mapRing(Ring1Procedure.RingFace face, float alpha) {
        return new RingFace(face.vertices(), face.uvs(), alpha);
    }

    private static RingFace mapRing(Ring2Procedure.RingFace face, float alpha) {
        return new RingFace(face.vertices(), face.uvs(), alpha);
    }

    private static RingFace mapRing(Ring3Procedure.RingFace face, float alpha) {
        return new RingFace(face.vertices(), face.uvs(), alpha);
    }

    private static RingFace mapRing(Ring4Procedure.RingFace face, float alpha) {
        return new RingFace(face.vertices(), face.uvs(), alpha);
    }

    public static void render(
            PoseStack poseStack,
            String planetId,
            Vector3f center,
            Vector3f axis,
            float size,
            float phaseDegrees,
            float alpha,
            double viewerDistance
    ) {
        if (alpha <= 0.001F || size <= 0.001F) {
            return;
        }

        PlanetTextureSet textures = PlanetTextureSet.forPlanet(planetId);
        textures.ensureLoaded();
        PlanetVisualConfig config = CONFIGS.getOrDefault(planetId, new PlanetVisualConfig(false, false));

        Vector3f direction = normalizedOrFallback(new Vector3f(center), new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f upAxis = normalizedOrFallback(new Vector3f(axis), new Vector3f(0.0F, 1.0F, 0.0F));
        CubeFrame frame = cubeFrame(direction, upAxis, Math.max(center.length(), 1.0F), size * 0.5F, size * 0.5F, Math.toRadians(phaseDegrees));
        Vector3f lightLocal = worldToLocal(frame, new Vector3f(KEY_LIGHT_DIRECTION));
        Vector3f viewLocal = localViewDirection(frame);
        CosmicPlanetRenderData renderData = CosmicPlanetRenderData.build(lightLocal, viewLocal, config.atmosphere());
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        RenderMINTProcedure.renderPlanet(new RenderMINTProcedure.PlanetLayerPass() {
            @Override
            public void renderSurface() {
                CosmicPlanetRenderer.renderSurface(buffer, poseStack.last(), frame, textures, renderData, alpha);
            }

            @Override
            public void renderAtmosphere() {
                CosmicPlanetRenderer.renderAtmosphere(buffer, poseStack.last(), frame, renderData, alpha, planetId);
            }

            @Override
            public void renderRing() {
                CosmicPlanetRenderer.renderSaturnRings(buffer, poseStack.last(), frame, alpha);
            }

            @Override
            public boolean hasAtmosphere() {
                return config.atmosphere();
            }

            @Override
            public boolean hasRing() {
                return config.ring();
            }
        });

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderSurface(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            PlanetTextureSet textures,
            CosmicPlanetRenderData renderData,
            float alpha
    ) {
        List<ChFace> orderedFaces = sortedFaces(frame);
        RenderMINTProcedure.renderIndexed(orderedFaces.size(), faceIndex -> {
            ChFace face = orderedFaces.get(faceIndex);
            CosmicPlanetRenderData.VertexColorSet lightData = renderData.light_data().get(face.side());
            RenderMINTProcedure.renderIndexed(4, i -> {
                Vector3f position = cubePoint(frame, face.vertices()[i]);
                Vector2f uv = face.uvs()[i];
                TextureSample sample = textures.sample(uv.x, uv.y);
                int lightColor = lightData.color(i);
                float lightRed = ((lightColor >>> 16) & 0xFF) / 255.0F;
                float lightGreen = ((lightColor >>> 8) & 0xFF) / 255.0F;
                float lightBlue = (lightColor & 0xFF) / 255.0F;
                buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                        clamp(sample.red() * lightRed, 0.0F, 1.0F),
                        clamp(sample.green() * lightGreen, 0.0F, 1.0F),
                        clamp(sample.blue() * lightBlue, 0.0F, 1.0F),
                        alpha
                );
            });
        });
    }

    private static void renderAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CosmicPlanetRenderData renderData,
            float alphaScale,
            String planetId
    ) {
        Vector3f tint = atmosphereTint(planetId);
        float[] shellScales = {1.025F, 1.05F, 1.08F};
        List<ChFace> orderedFaces = sortedFaces(frame);
        for (int layer = 0; layer < shellScales.length; layer++) {
            final int shellLayer = layer;
            float layerStrength = layer == 0 ? 0.22F : (layer == 1 ? 0.14F : 0.08F);
            RenderMINTProcedure.renderIndexed(orderedFaces.size(), faceIndex -> {
                ChFace face = orderedFaces.get(faceIndex);
                CosmicPlanetRenderData.VertexAlphaSet alphaData = renderData.alpha_data().get(face.side());
                RenderMINTProcedure.renderIndexed(4, i -> {
                    Vector3f position = cubeShellPoint(frame, face.vertices()[i], shellScales[shellLayer]);
                    float vertexAlpha = alphaData.alpha(i) * layerStrength * alphaScale;
                    buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                            tint.x(),
                            tint.y(),
                            tint.z(),
                            clamp(vertexAlpha, 0.0F, 1.0F)
                    );
                });
            });
        }
    }

    private static void renderSaturnRings(BufferBuilder buffer, PoseStack.Pose pose, CubeFrame frame, float alpha) {
        RenderMINTProcedure.renderIndexed(CH_RINGS.length, bandIndex -> {
            RingFace band = CH_RINGS[bandIndex];
            RenderMINTProcedure.renderIndexed(4, i -> {
                Vector3f local = new Vector3f(band.vertices()[i]).mul(5.5F);
                Vector3f position = ringPoint(frame, local);
                float u = band.uvs()[i].x;
                float v = band.uvs()[i].y;
                float tone = 0.76F + (1.0F - Math.abs(v - 0.5F) * 1.5F) * 0.10F;
                buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                        clamp(tone, 0.0F, 1.0F),
                        clamp(tone * 0.94F, 0.0F, 1.0F),
                        clamp(tone * 0.78F, 0.0F, 1.0F),
                        alpha * band.alphaScale()
                );
            });
        });
    }

    private static Vector3f ringPoint(CubeFrame frame, Vector3f local) {
        return new Vector3f(frame.center)
                .add(new Vector3f(frame.right).mul(local.x() * frame.halfSpan))
                .add(new Vector3f(frame.up).mul(local.y() * frame.halfSpan))
                .add(new Vector3f(frame.forward).mul(local.z() * frame.halfDepth));
    }

    private static Vector3f atmosphereTint(String planetId) {
        return switch (planetId) {
            case "venus" -> new Vector3f(0.92F, 0.80F, 0.56F);
            case "mars" -> new Vector3f(0.78F, 0.50F, 0.42F);
            case "jupiter", "saturn" -> new Vector3f(0.85F, 0.78F, 0.68F);
            case "uranus", "neptune" -> new Vector3f(0.54F, 0.76F, 0.92F);
            default -> new Vector3f(0.72F, 0.80F, 0.90F);
        };
    }

    private static CubeFrame cubeFrame(Vector3f direction, Vector3f axisHint, float radius, float halfSpan, float halfDepth, double rotationPhaseRadians) {
        Vector3f centerDirection = normalizedOrFallback(new Vector3f(direction), new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f up = normalizedOrFallback(new Vector3f(axisHint), new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f reference = Math.abs(up.z()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 0.0F, 1.0F);
        if (reference.cross(up, new Vector3f()).lengthSquared() < 1.0E-6F) {
            reference.set(1.0F, 0.0F, 0.0F);
        }
        Vector3f right = reference.cross(up, new Vector3f()).normalize();
        Vector3f forward = up.cross(right, new Vector3f()).normalize();
        Quaternionf spin = new Quaternionf().fromAxisAngleRad(up.x(), up.y(), up.z(), (float) rotationPhaseRadians);
        spin.transform(right);
        spin.transform(forward);
        return new CubeFrame(new Vector3f(centerDirection).mul(radius), right, up, forward, halfSpan, halfDepth);
    }

    private static Vector3f cubePoint(CubeFrame frame, Vector3f local) {
        return new Vector3f(frame.center())
                .add(new Vector3f(frame.right()).mul(local.x() * frame.halfSpan()))
                .add(new Vector3f(frame.up()).mul(local.y() * frame.halfSpan()))
                .add(new Vector3f(frame.forward()).mul(local.z() * frame.halfDepth()));
    }

    private static Vector3f cubeShellPoint(CubeFrame frame, Vector3f local, float scale) {
        return cubePoint(frame, new Vector3f(local).mul(scale));
    }

    private static Vector3f worldToLocal(CubeFrame frame, Vector3f world) {
        return new Vector3f(world.dot(frame.right()), world.dot(frame.up()), world.dot(frame.forward()));
    }

    private static Vector3f localViewDirection(CubeFrame frame) {
        return normalizedOrFallback(worldToLocal(frame, new Vector3f(frame.center()).negate()), new Vector3f(0.0F, 0.0F, -1.0F));
    }

    private static Vector3f normalizedOrFallback(Vector3f vector, Vector3f fallback) {
        return vector.lengthSquared() < 1.0E-6F ? new Vector3f(fallback).normalize() : vector.normalize();
    }

    private static List<ChFace> sortedFaces(CubeFrame frame) {
        List<FaceDepth> ordered = new ArrayList<>(CH_FACES.length);
        for (ChFace face : CH_FACES) {
            Vector3f center = new Vector3f();
            for (Vector3f vertex : face.vertices()) {
                center.add(vertex);
            }
            center.div(4.0F);
            ordered.add(new FaceDepth(face, cubePoint(frame, center).lengthSquared()));
        }
        ordered.sort(Comparator.comparingDouble(FaceDepth::depth).reversed());
        List<ChFace> result = new ArrayList<>(ordered.size());
        for (FaceDepth depth : ordered) {
            result.add(depth.face());
        }
        return result;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum CubeSide {
        NORTH,
        SOUTH,
        UP,
        DOWN,
        EAST,
        WEST
    }

    private record CubeFrame(Vector3f center, Vector3f right, Vector3f up, Vector3f forward, float halfSpan, float halfDepth) {
    }

    private record FaceDepth(ChFace face, double depth) {
    }

    private record PlanetVisualConfig(boolean atmosphere, boolean ring) {
    }

    private record TextureSample(float red, float green, float blue) {
    }

    private record ChFace(CubeSide side, Vector3f[] vertices, Vector2f[] uvs) {
    }

    private record RingFace(Vector3f[] vertices, Vector2f[] uvs, float alphaScale) {
    }

    private record CosmicPlanetRenderData(
            EnumMap<CubeSide, VertexColorSet> light_data,
            EnumMap<CubeSide, VertexAlphaSet> alpha_data,
            EnumMap<CubeSide, VertexAlphaSet> i_alpha_data
    ) {
        private static CosmicPlanetRenderData build(Vector3f lightLocal, Vector3f viewLocal, boolean atmosphere) {
            EnumMap<CubeSide, VertexColorSet> lightData = new EnumMap<>(CubeSide.class);
            EnumMap<CubeSide, VertexAlphaSet> alphaData = new EnumMap<>(CubeSide.class);
            EnumMap<CubeSide, VertexAlphaSet> inverseAlphaData = new EnumMap<>(CubeSide.class);
            for (ChFace face : CH_FACES) {
                Vector3f normal = faceNormal(face);
                float faceLight = clamp(normal.dot(lightLocal), 0.0F, 1.0F);
                float faceView = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
                int c0 = multiplyRgb(chBaseLightColor(face.side(), 0), 0.32F + faceLight * 0.68F);
                int c1 = multiplyRgb(chBaseLightColor(face.side(), 1), 0.32F + faceLight * 0.68F);
                int c2 = multiplyRgb(chBaseLightColor(face.side(), 2), 0.32F + faceLight * 0.68F);
                int c3 = multiplyRgb(chBaseLightColor(face.side(), 3), 0.32F + faceLight * 0.68F);
                lightData.put(face.side(), new VertexColorSet(c0, c1, c2, c3));

                float edgeAlpha = atmosphere ? (0.10F + (1.0F - faceView) * 0.28F + faceLight * 0.08F) : 0.0F;
                alphaData.put(face.side(), new VertexAlphaSet(edgeAlpha, edgeAlpha, edgeAlpha, edgeAlpha));

                float inverse = 0.20F + (1.0F - faceLight) * 0.48F;
                inverseAlphaData.put(face.side(), new VertexAlphaSet(inverse, inverse, inverse, inverse));
            }
            return new CosmicPlanetRenderData(lightData, alphaData, inverseAlphaData);
        }

        private static int chBaseLightColor(CubeSide side, int vertexIndex) {
            return switch (side) {
                case DOWN -> 0xFFA0A0A0;
                case UP -> 0xFFFFFFFF;
                case SOUTH -> 0xFFE0E0E0;
                case EAST, WEST -> vertexIndex < 2 ? 0xFFC0C0C0 : 0xFFE0E0E0;
                case NORTH -> vertexIndex < 2 ? 0xFFE0E0E0 : 0xFFC0C0C0;
            };
        }

        private static int multiplyRgb(int argb, float amount) {
            int r = Math.max(0, Math.min(255, Math.round(((argb >>> 16) & 0xFF) * amount)));
            int g = Math.max(0, Math.min(255, Math.round(((argb >>> 8) & 0xFF) * amount)));
            int b = Math.max(0, Math.min(255, Math.round((argb & 0xFF) * amount)));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        private static Vector3f faceNormal(ChFace face) {
            Vector3f a = new Vector3f(face.vertices()[1]).sub(face.vertices()[0]);
            Vector3f b = new Vector3f(face.vertices()[2]).sub(face.vertices()[0]);
            return a.cross(b, new Vector3f()).normalize();
        }

        private record VertexColorSet(int c0, int c1, int c2, int c3) {
            private int color(int index) {
                return switch (index) {
                    case 0 -> c0;
                    case 1 -> c1;
                    case 2 -> c2;
                    default -> c3;
                };
            }
        }

        private record VertexAlphaSet(float a0, float a1, float a2, float a3) {
            private float alpha(int index) {
                return switch (index) {
                    case 0 -> a0;
                    case 1 -> a1;
                    case 2 -> a2;
                    default -> a3;
                };
            }
        }
    }

    private static final class PlanetTextureSet {
        private final ResourceLocation surfaceLocation;
        private BufferedImage surface;
        private boolean loaded;

        private PlanetTextureSet(String planetId) {
            this.surfaceLocation = GregtechUniverseSpace.id("textures/celestial_body/planet/" + planetId + "/surface.png");
        }

        private static PlanetTextureSet forPlanet(String planetId) {
            return TEXTURE_CACHE.computeIfAbsent(planetId, PlanetTextureSet::new);
        }

        private void ensureLoaded() {
            if (loaded) {
                return;
            }
            loaded = true;
            surface = load(surfaceLocation);
        }

        private TextureSample sample(float u, float v) {
            if (surface == null) {
                return new TextureSample(0.82F, 0.84F, 0.88F);
            }
            int x = Math.max(0, Math.min(surface.getWidth() - 1, Math.round(clamp(u, 0.0F, 1.0F) * (surface.getWidth() - 1))));
            int y = Math.max(0, Math.min(surface.getHeight() - 1, Math.round(clamp(v, 0.0F, 1.0F) * (surface.getHeight() - 1))));
            int argb = surface.getRGB(x, y);
            return new TextureSample(
                    ((argb >>> 16) & 0xFF) / 255.0F,
                    ((argb >>> 8) & 0xFF) / 255.0F,
                    (argb & 0xFF) / 255.0F
            );
        }

        private static BufferedImage load(ResourceLocation location) {
            if (location == null || Minecraft.getInstance().getResourceManager() == null) {
                return null;
            }
            try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
                return ImageIO.read(stream);
            } catch (IOException ignored) {
                return null;
            }
        }
    }
}
