package org.polaris2023.gtu.space.client.cosmic;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.network.ClientSpaceCache;
import org.polaris2023.gtu.space.network.SpaceStateSyncPacket;
import org.polaris2023.gtu.space.client.cosmic.ch.AnimatedLightCubeMapProcedure;
import org.polaris2023.gtu.space.client.cosmic.ch.RenderMINTProcedure;
import org.polaris2023.gtu.space.client.cosmic.ch.TexturedCubeCuratedProcedure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;

/**
 * Direct space-module port of the Cosmic Horizons Earth cube rendering path.
 * This renderer keeps the CH-style layered order:
 * surface -> inverse/night -> animated cloud atlas -> atmosphere shells.
 */
public final class CosmicEarthRenderer {
    private static final Vector3f KEY_LIGHT_DIRECTION = new Vector3f(-0.35F, 0.74F, 0.58F).normalize();
    private static final EarthTextures TEXTURES = new EarthTextures();
    private static final EarthFace[] CURATED_FACES = mapFaces(TexturedCubeCuratedProcedure.execute());
    private static final EarthFace[] ANIMATED_FACES = mapAnimatedFaces();

    private CosmicEarthRenderer() {
    }

    private static EarthFace[] mapAnimatedFaces() {
        EarthFace[] mapped = new EarthFace[CURATED_FACES.length];
        for (int i = 0; i < CURATED_FACES.length; i++) {
            EarthFace face = CURATED_FACES[i];
            mapped[i] = new EarthFace(face.side(), face.vertices(), AnimatedLightCubeMapProcedure.execute(reverseMapSide(face.side())));
        }
        return mapped;
    }

    private static EarthFace[] mapFaces(TexturedCubeCuratedProcedure.Face[] faces) {
        EarthFace[] mapped = new EarthFace[faces.length];
        for (int i = 0; i < faces.length; i++) {
            mapped[i] = new EarthFace(mapSide(faces[i].side()), faces[i].vertices(), faces[i].uvs());
        }
        return mapped;
    }

    private static CubeSide mapSide(Direction side) {
        return switch (side) {
            case NORTH -> CubeSide.NORTH;
            case SOUTH -> CubeSide.SOUTH;
            case EAST -> CubeSide.EAST;
            case WEST -> CubeSide.WEST;
            case UP -> CubeSide.UP;
            case DOWN -> CubeSide.DOWN;
        };
    }

    private static Direction reverseMapSide(CubeSide side) {
        return switch (side) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    public static void render(
            PoseStack poseStack,
            Vector3f center,
            float size,
            float phaseDegrees,
            float alpha,
            double viewerDistance
    ) {
        if (alpha <= 0.001F || size <= 0.001F) {
            return;
        }

        TEXTURES.ensureLoaded();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f direction = normalizedOrFallback(new Vector3f(center), new Vector3f(0.0F, -1.0F, 0.0F));
        Vector3f axis = new Vector3f(0.0F, 1.0F, 0.0F);
        CubeFrame frame = cubeFrame(direction, axis, center.length(), size * 0.5F, size * 0.5F, Math.toRadians(phaseDegrees));
        Vector3f lightLocal = worldToLocal(frame, new Vector3f(KEY_LIGHT_DIRECTION));
        Vector3f viewLocal = localViewDirection(frame);
        CosmicEarthRenderData renderData = CosmicEarthRenderSequence.build(lightLocal, viewLocal);
        int subdivisions = resolveSubdivisions(viewerDistance);
        int cloudSubdivisions = Math.max(1, subdivisions / 2);
        CubeSide[] orderedSides = sortedSides(frame);

        RenderMINTProcedure.renderEarth(new RenderMINTProcedure.EarthLayerPass() {
            @Override
            public void renderSurface() {
                CosmicEarthRenderer.renderSurface(buffer, poseStack.last(), frame, orderedSides, lightLocal, viewLocal, alpha, subdivisions, renderData);
            }

            @Override
            public void renderInverseTexture() {
                CosmicEarthRenderer.renderInverseTexture(buffer, poseStack.last(), frame, orderedSides, alpha, renderData);
            }

            @Override
            public void renderClouds() {
                CosmicEarthRenderer.renderClouds(buffer, poseStack.last(), frame, orderedSides, lightLocal, alpha, cloudSubdivisions, renderData);
            }

            @Override
            public void renderAtmosphere() {
                CosmicEarthRenderer.renderAtmosphere(buffer, poseStack.last(), frame, orderedSides, lightLocal, viewLocal, alpha, subdivisions, viewerDistance, renderData);
            }
        });

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderSurface(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide[] orderedSides,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alpha,
            int subdivisions,
            CosmicEarthRenderData renderData
    ) {
        RenderMINTProcedure.renderOrderedSides(orderedSides, side -> {
            EarthFace face = findFace(CURATED_FACES, side);
            if (face == null) {
                return;
            }
            RenderMINTProcedure.renderIndexed(4, i -> {
                addSurfaceVertex(buffer, pose, frame, side, face.vertices()[i], face.uvs()[i], lightLocal, viewLocal, alpha, renderData);
            });
        });
    }

    private static void addSurfaceVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            Vector3f local,
            Vector2f uv,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alpha,
            CosmicEarthRenderData renderData
    ) {
        TextureSample day = TEXTURES.sampleSurface(side, uv.x, uv.y);
        CosmicEarthRenderData.VertexColorSet lightData = renderData.light_data().get(side);
        Vector3f normal = side.normal();

        float lambert = clamp(normal.dot(lightLocal), 0.0F, 1.0F);
        float viewDot = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float diffuse = 0.10F + lambert * 0.90F;
        float rim = (float) Math.pow(1.0F - viewDot, 2.35F);
        float oceanMask = clamp((day.blue - Math.max(day.red, day.green)) * 2.6F + 0.28F, 0.0F, 1.0F);
        float landMask = 1.0F - oceanMask;
        Vector3f halfVector = normalizedOrFallback(new Vector3f(lightLocal).add(viewLocal), lightLocal);
        float specular = oceanMask * (float) Math.pow(clamp(normal.dot(halfVector), 0.0F, 1.0F), 18.0F) * 0.42F;

        float baseRed = day.red * (0.86F + landMask * 0.10F);
        float baseGreen = day.green * (0.88F + landMask * 0.16F);
        float baseBlue = day.blue * (0.78F + oceanMask * 0.30F);
        float lightRed = ((((lightData.c0() >>> 16) & 0xFF) / 255.0F) + (((lightData.c1() >>> 16) & 0xFF) / 255.0F)
                + (((lightData.c2() >>> 16) & 0xFF) / 255.0F) + (((lightData.c3() >>> 16) & 0xFF) / 255.0F)) * 0.25F;
        float lightGreen = ((((lightData.c0() >>> 8) & 0xFF) / 255.0F) + (((lightData.c1() >>> 8) & 0xFF) / 255.0F)
                + (((lightData.c2() >>> 8) & 0xFF) / 255.0F) + (((lightData.c3() >>> 8) & 0xFF) / 255.0F)) * 0.25F;
        float lightBlue = ((((lightData.c0()) & 0xFF) / 255.0F) + (((lightData.c1()) & 0xFF) / 255.0F)
                + (((lightData.c2()) & 0xFF) / 255.0F) + (((lightData.c3()) & 0xFF) / 255.0F)) * 0.25F;

        Vector3f position = cubePoint(frame, local);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp((baseRed * diffuse + rim * (0.03F + oceanMask * 0.04F) + specular * 0.28F) * lightRed, 0.0F, 1.0F),
                clamp((baseGreen * diffuse + rim * (0.06F + oceanMask * 0.05F) + specular * 0.36F) * lightGreen, 0.0F, 1.0F),
                clamp((baseBlue * diffuse + rim * (0.12F + oceanMask * 0.10F) + specular * 0.70F) * lightBlue, 0.0F, 1.0F),
                alpha
        );
    }

    private static void renderInverseTexture(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide[] orderedSides,
            float alphaScale,
            CosmicEarthRenderData renderData
    ) {
        RenderMINTProcedure.renderOrderedSides(orderedSides, side -> {
            EarthFace face = findFace(CURATED_FACES, side);
            if (face == null) {
                return;
            }
            RenderMINTProcedure.renderIndexed(4, i -> {
                addInverseVertex(buffer, pose, frame, side, face.vertices()[i], face.uvs()[i], alphaScale, renderData);
            });
        });
    }

    private static void addInverseVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            Vector3f local,
            Vector2f uv,
            float alphaScale,
            CosmicEarthRenderData renderData
    ) {
        TextureSample night = TEXTURES.sampleNight(uv.x, uv.y, side.normal(), 0.0);
        CosmicEarthRenderData.VertexAlphaSet inverseAlpha = renderData.i_alpha_data().get(side);
        int vertexIndex = faceIndex(local, side);
        float alpha = alphaScale * night.luminance() * vertexValue(inverseAlpha, vertexIndex);
        if (alpha <= 0.002F) {
            return;
        }

        Vector3f position = cubeShellPoint(frame, local, 1.0008F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(night.red * 1.35F, 0.0F, 1.0F),
                clamp(night.green * 1.25F, 0.0F, 1.0F),
                clamp(night.blue * 1.15F, 0.0F, 1.0F),
                clamp(alpha, 0.0F, 1.0F)
        );
    }

    private static void renderClouds(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide[] orderedSides,
            Vector3f lightLocal,
            float alpha,
            int subdivisions,
            CosmicEarthRenderData renderData
    ) {
        RenderMINTProcedure.renderOrderedSides(orderedSides, side -> {
            EarthFace face = findFace(ANIMATED_FACES, side);
            if (face == null) {
                return;
            }
            RenderMINTProcedure.renderIndexed(4, i -> {
                addCloudVertex(buffer, pose, frame, side, face.vertices()[i], face.uvs()[i], lightLocal, alpha, renderData);
            });
        });
    }

    private static void addCloudVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            Vector3f local,
            Vector2f uv,
            Vector3f lightLocal,
            float alphaScale,
            CosmicEarthRenderData renderData
    ) {
        TextureSample cloud = TEXTURES.sampleCloud(uv.x, uv.y);
        CosmicEarthRenderData.CloudData cloudData = renderData.cloud_data();
        float cloudMask = Math.max(cloud.luminance(), cloud.alpha);
        float lambert = clamp(side.normal().dot(lightLocal), 0.0F, 1.0F);
        float cloudAlpha = alphaScale * cloudMask * (0.08F + 0.26F * (0.30F + lambert * 0.70F));
        float whiten = cloudData.r() + lambert * (1.0F - cloudData.r());

        Vector3f position = cubeShellPoint(frame, local, 1.022F);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(whiten * cloudData.r(), 0.0F, 1.0F),
                clamp(whiten * cloudData.g(), 0.0F, 1.0F),
                clamp(whiten * cloudData.b(), 0.0F, 1.0F),
                clamp(cloudAlpha * cloudData.alpha(), 0.0F, 1.0F)
        );
    }

    private static void renderAtmosphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide[] orderedSides,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alphaScale,
            int subdivisions,
            double viewerDistance,
            CosmicEarthRenderData renderData
    ) {
        float[] shellScales = {1.018F, 1.032F, 1.048F, 1.066F, 1.086F};
        float[] shellAlpha = {0.26F, 0.22F, 0.18F, 0.13F, 0.09F};
        int layerCount = resolveAtmosphereLayerCount(viewerDistance);
        for (int layer = 0; layer < layerCount; layer++) {
            final int shellLayer = layer;
            RenderMINTProcedure.renderSubdividedSides(orderedSides, subdivisions, (side, u0, v0, u1, v1) -> {
                addAtmosphereVertex(buffer, pose, frame, side, u0, v0, lightLocal, viewLocal, alphaScale * shellAlpha[shellLayer], shellScales[shellLayer], renderData);
                addAtmosphereVertex(buffer, pose, frame, side, u1, v0, lightLocal, viewLocal, alphaScale * shellAlpha[shellLayer], shellScales[shellLayer], renderData);
                addAtmosphereVertex(buffer, pose, frame, side, u1, v1, lightLocal, viewLocal, alphaScale * shellAlpha[shellLayer], shellScales[shellLayer], renderData);
                addAtmosphereVertex(buffer, pose, frame, side, u0, v1, lightLocal, viewLocal, alphaScale * shellAlpha[shellLayer], shellScales[shellLayer], renderData);
            });
        }
    }

    private static void addAtmosphereVertex(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            CubeFrame frame,
            CubeSide side,
            float u,
            float v,
            Vector3f lightLocal,
            Vector3f viewLocal,
            float alphaScale,
            float shellScale,
            CosmicEarthRenderData renderData
    ) {
        Vector3f local = side.local(u, v);
        Vector3f normal = side.normal();
        CosmicEarthRenderData.VertexAlphaSet alphaData = renderData.alpha_data().get(side);
        float viewDot = clamp(normal.dot(viewLocal), 0.0F, 1.0F);
        float edge = clamp((Math.max(Math.abs(u), Math.abs(v)) - 0.35F) / 0.65F, 0.0F, 1.0F);
        float rim = (float) Math.pow(1.0F - viewDot, 2.05F) * (0.48F + edge * 0.52F);
        float sunAmount = 0.30F + clamp(normal.dot(lightLocal), 0.0F, 1.0F) * 0.70F;
        float alpha = alphaScale * rim * (0.08F + sunAmount * 0.24F) * vertexValue(alphaData, faceIndex(local, side));

        Vector3f position = cubeShellPoint(frame, local, shellScale);
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(
                clamp(0.20F + sunAmount * 0.16F, 0.0F, 1.0F),
                clamp(0.50F + sunAmount * 0.22F, 0.0F, 1.0F),
                clamp(0.98F + sunAmount * 0.10F, 0.0F, 1.0F),
                clamp(alpha, 0.0F, 1.0F)
        );
    }

    private static int resolveSubdivisions(double viewerDistance) {
        if (viewerDistance < 2_000.0) return 2;
        if (viewerDistance < 10_000.0) return 1;
        return 1;
    }

    private static int resolveAtmosphereLayerCount(double viewerDistance) {
        if (viewerDistance < 2_000.0) return 2;
        return 1;
    }

    private static float vertexValue(CosmicEarthRenderData.VertexAlphaSet set, int vertexIndex) {
        return switch (vertexIndex) {
            case 0 -> set.a0();
            case 1 -> set.a1();
            case 2 -> set.a2();
            default -> set.a3();
        };
    }

    private static int faceIndex(Vector3f local, CubeSide side) {
        EarthFace face = findFace(CURATED_FACES, side);
        if (face == null) {
            return 0;
        }
        for (int i = 0; i < face.vertices().length; i++) {
            if (face.vertices()[i].distanceSquared(local) < 1.0E-6F) {
                return i;
            }
        }
        return 0;
    }

    private static EarthFace findFace(EarthFace[] faces, CubeSide side) {
        for (EarthFace face : faces) {
            if (face.side() == side) {
                return face;
            }
        }
        return null;
    }

    private static CubeFrame cubeFrame(Vector3f direction, Vector3f axisHint, float radius, float halfSpan, float halfDepth, double rotationPhaseRadians) {
        Vector3f centerDirection = normalizedOrFallback(new Vector3f(direction), new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f up = normalizedOrFallback(new Vector3f(axisHint), new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f reference = Math.abs(up.z()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 0.0F, 1.0F);
        if (reference.cross(up, new Vector3f()).lengthSquared() < 1.0E-6F) {
            reference.set(0.0F, 1.0F, 0.0F);
        }
        Vector3f right = reference.cross(up, new Vector3f()).normalize();
        Vector3f forward = up.cross(right, new Vector3f()).normalize();
        Quaternionf spin = new Quaternionf().fromAxisAngleRad(up.x(), up.y(), up.z(), (float) rotationPhaseRadians);
        spin.transform(right);
        spin.transform(forward);
        return new CubeFrame(new Vector3f(centerDirection).mul(radius), right, up, forward, halfSpan, halfDepth);
    }

    private static Vector3f cubePoint(CubeFrame frame, Vector3f local) {
        return new Vector3f(frame.center)
                .add(new Vector3f(frame.right).mul(local.x() * frame.halfSpan))
                .add(new Vector3f(frame.up).mul(local.y() * frame.halfSpan))
                .add(new Vector3f(frame.forward).mul(local.z() * frame.halfDepth));
    }

    private static Vector3f cubeShellPoint(CubeFrame frame, Vector3f local, float scale) {
        return cubePoint(frame, new Vector3f(local).mul(scale));
    }

    private static Vector3f worldToLocal(CubeFrame frame, Vector3f world) {
        return new Vector3f(world.dot(frame.right), world.dot(frame.up), world.dot(frame.forward));
    }

    private static Vector3f localViewDirection(CubeFrame frame) {
        return normalizedOrFallback(worldToLocal(frame, new Vector3f(frame.center).negate()), new Vector3f(0.0F, 0.0F, -1.0F));
    }

    private static Vector3f normalizedOrFallback(Vector3f vector, Vector3f fallback) {
        return vector.lengthSquared() < 1.0E-6F ? new Vector3f(fallback).normalize() : vector.normalize();
    }

    private static CubeSide[] sortedSides(CubeFrame frame) {
        CubeSideDepth[] sides = new CubeSideDepth[CubeSide.values().length];
        int index = 0;
        for (CubeSide side : CubeSide.values()) {
            sides[index++] = new CubeSideDepth(side, cubePoint(frame, side.center()).lengthSquared());
        }
        java.util.Arrays.sort(sides, (left, right) -> Double.compare(right.depth, left.depth));
        CubeSide[] result = new CubeSide[sides.length];
        for (int i = 0; i < sides.length; i++) {
            result[i] = sides[i].side;
        }
        return result;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class EarthTextures {
        private final EnumMap<CubeSide, ResourceLocation> faceLocations = new EnumMap<>(CubeSide.class);
        private final EnumMap<CubeSide, BufferedImage> faceImages = new EnumMap<>(CubeSide.class);
        private final EnumMap<CubeSide, FaceUvBounds> faceBounds = new EnumMap<>(CubeSide.class);
        private final EarthTerrainPreview terrainPreview = new EarthTerrainPreview();
        private final ResourceLocation nightLocation = GregtechUniverseSpace.id("textures/celestial_body/planet/earth/surface_night.png");
        private final ResourceLocation cloudLocation = GregtechUniverseSpace.id("textures/celestial_body/planet/earth/cloud.png");
        private BufferedImage nightImage;
        private BufferedImage cloudAtlas;
        private int cloudFrameSize;
        private int cloudFrameCount;
        private int cachedCloudFrame = Integer.MIN_VALUE;
        private int[] cachedCloudPixels;
        private boolean loaded;

        private EarthTextures() {
            for (CubeSide side : CubeSide.values()) {
                faceLocations.put(side, GregtechUniverseSpace.id("textures/celestial_body/planet/earth/faces/" + side.fileName + ".png"));
            }
        }

        private void ensureLoaded() {
            if (loaded) return;
            loaded = true;
            for (CubeSide side : CubeSide.values()) {
                BufferedImage image = load(faceLocations.get(side));
                if (image != null) faceImages.put(side, image);
                EarthFace face = findFace(CURATED_FACES, side);
                if (face != null) {
                    faceBounds.put(side, FaceUvBounds.from(face.uvs()));
                }
            }
            nightImage = load(nightLocation);
            cloudAtlas = load(cloudLocation);
            if (cloudAtlas != null) {
                cloudFrameSize = cloudAtlas.getWidth();
                cloudFrameCount = Math.max(1, cloudAtlas.getHeight() / Math.max(1, cloudFrameSize));
            }
        }

        private TextureSample sampleSurface(CubeSide side, float u, float v) {
            BufferedImage image = faceImages.get(side);
            FaceUvBounds bounds = faceBounds.get(side);
            TextureSample fallback = new TextureSample(0.18F, 0.31F, 0.60F, 1.0F);
            if (image == null || bounds == null) {
                return terrainPreview.sample(side, 0.5F, 0.5F, fallback);
            }
            float localU = (u - bounds.minU) / Math.max(bounds.maxU - bounds.minU, 1.0E-6F);
            float localV = (v - bounds.minV) / Math.max(bounds.maxV - bounds.minV, 1.0E-6F);
            TextureSample base = sample(image, localU, localV, fallback);
            return terrainPreview.sample(side, localU, localV, base);
        }

        private TextureSample sampleNight(float u, float v, Vector3f normal, double rotationPhaseRadians) {
            if (nightImage == null) {
                return new TextureSample(0.0F, 0.0F, 0.0F, 1.0F);
            }
            Vector3f north = new Vector3f(0.0F, 1.0F, 0.0F);
            Vector3f reference = Math.abs(north.z()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 0.0F, 1.0F);
            Vector3f east = reference.cross(north, new Vector3f()).normalize();
            Vector3f prime = north.cross(east, new Vector3f()).normalize();
            Quaternionf spin = new Quaternionf().fromAxisAngleRad(north.x(), north.y(), north.z(), (float) rotationPhaseRadians);
            spin.transform(east);
            spin.transform(prime);
            float longitude = (float) Math.atan2(normal.dot(east), normal.dot(prime));
            float mappedU = 0.5F + longitude / Mth.TWO_PI;
            mappedU = mappedU - (float) Math.floor(mappedU);
            float latitude = clamp(normal.dot(north), -1.0F, 1.0F);
            float mappedV = clamp(0.5F - (float) (Math.asin(latitude) / Math.PI), 0.0F, 1.0F);
            return sample(nightImage, mappedU, mappedV, new TextureSample(0.0F, 0.0F, 0.0F, 1.0F));
        }

        private TextureSample sampleCloud(float u, float v) {
            if (cloudAtlas == null) {
                return new TextureSample(0.0F, 0.0F, 0.0F, 0.0F);
            }
            long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
            int frameIndex = Math.floorMod((int) (gameTime / 2L), cloudFrameCount);
            updateCloudFrameCache(frameIndex);
            int x = clamp(Math.round(clamp(u, 0.0F, 1.0F) * (cloudFrameSize - 1)), 0, cloudFrameSize - 1);
            int y = clamp(Math.round(clamp(v, 0.0F, 1.0F) * (cloudFrameSize - 1)), 0, cloudFrameSize - 1);
            int argb = cachedCloudPixels[y * cloudFrameSize + x];
            return new TextureSample(
                    ((argb >>> 16) & 0xFF) / 255.0F,
                    ((argb >>> 8) & 0xFF) / 255.0F,
                    (argb & 0xFF) / 255.0F,
                    ((argb >>> 24) & 0xFF) / 255.0F
            );
        }

        private void updateCloudFrameCache(int frameIndex) {
            if (cachedCloudFrame == frameIndex && cachedCloudPixels != null) {
                return;
            }
            if (cachedCloudPixels == null || cachedCloudPixels.length != cloudFrameSize * cloudFrameSize) {
                cachedCloudPixels = new int[cloudFrameSize * cloudFrameSize];
            }
            cloudAtlas.getRGB(0, frameIndex * cloudFrameSize, cloudFrameSize, cloudFrameSize, cachedCloudPixels, 0, cloudFrameSize);
            cachedCloudFrame = frameIndex;
        }

        private static TextureSample sample(BufferedImage image, float u, float v, TextureSample fallback) {
            if (image == null) return fallback;
            int x = clamp(Math.round(clamp(u, 0.0F, 1.0F) * (image.getWidth() - 1)), 0, image.getWidth() - 1);
            int y = clamp(Math.round(clamp(v, 0.0F, 1.0F) * (image.getHeight() - 1)), 0, image.getHeight() - 1);
            int argb = image.getRGB(x, y);
            return new TextureSample(
                    ((argb >>> 16) & 0xFF) / 255.0F,
                    ((argb >>> 8) & 0xFF) / 255.0F,
                    (argb & 0xFF) / 255.0F,
                    ((argb >>> 24) & 0xFF) / 255.0F
            );
        }

        private static BufferedImage load(ResourceLocation location) {
            if (location == null || Minecraft.getInstance().getResourceManager() == null) return null;
            try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
                return ImageIO.read(stream);
            } catch (IOException ignored) {
                return null;
            }
        }

        private record FaceUvBounds(float minU, float maxU, float minV, float maxV) {
            private static FaceUvBounds from(Vector2f[] uvs) {
                float minU = Math.min(Math.min(uvs[0].x, uvs[1].x), Math.min(uvs[2].x, uvs[3].x));
                float maxU = Math.max(Math.max(uvs[0].x, uvs[1].x), Math.max(uvs[2].x, uvs[3].x));
                float minV = Math.min(Math.min(uvs[0].y, uvs[1].y), Math.min(uvs[2].y, uvs[3].y));
                float maxV = Math.max(Math.max(uvs[0].y, uvs[1].y), Math.max(uvs[2].y, uvs[3].y));
                return new FaceUvBounds(minU, maxU, minV, maxV);
            }
        }
    }

    private static final class EarthTerrainPreview {
        private static final int PREVIEW_SIZE = 64;
        private static final double FACE_WORLD_SPAN_BLOCKS = 2048.0;
        private static final int SAMPLE_CELL_BLOCKS = 16;

        private final EnumMap<CubeSide, int[]> cachedFaces = new EnumMap<>(CubeSide.class);
        private long cachedAnchorX = Long.MIN_VALUE;
        private long cachedAnchorZ = Long.MIN_VALUE;

        private TextureSample sample(CubeSide side, float u, float v, TextureSample base) {
            SpaceStateSyncPacket state = ClientSpaceCache.state();
            if (state == null) {
                return base;
            }
            ServerLevel level = resolveOverworld();
            if (level == null) {
                return base;
            }

            long anchorX = Math.round(state.stableX() / 32.0);
            long anchorZ = Math.round(state.stableZ() / 32.0);
            if (anchorX != cachedAnchorX || anchorZ != cachedAnchorZ || !cachedFaces.containsKey(side)) {
                rebuild(level, state);
            }

            int[] pixels = cachedFaces.get(side);
            if (pixels == null || pixels.length != PREVIEW_SIZE * PREVIEW_SIZE) {
                return base;
            }

            int x = clamp(Math.round(clamp(u, 0.0F, 1.0F) * (PREVIEW_SIZE - 1)), 0, PREVIEW_SIZE - 1);
            int y = clamp(Math.round(clamp(v, 0.0F, 1.0F) * (PREVIEW_SIZE - 1)), 0, PREVIEW_SIZE - 1);
            int argb = pixels[y * PREVIEW_SIZE + x];
            TextureSample overlay = new TextureSample(
                    ((argb >>> 16) & 0xFF) / 255.0F,
                    ((argb >>> 8) & 0xFF) / 255.0F,
                    (argb & 0xFF) / 255.0F,
                    ((argb >>> 24) & 0xFF) / 255.0F
            );
            float mix = overlay.alpha;
            return new TextureSample(
                    lerp(base.red, overlay.red, mix),
                    lerp(base.green, overlay.green, mix),
                    lerp(base.blue, overlay.blue, mix),
                    base.alpha
            );
        }

        private void rebuild(ServerLevel level, SpaceStateSyncPacket state) {
            cachedFaces.clear();
            cachedAnchorX = Math.round(state.stableX() / 32.0);
            cachedAnchorZ = Math.round(state.stableZ() / 32.0);
            for (CubeSide side : CubeSide.values()) {
                int[] pixels = new int[PREVIEW_SIZE * PREVIEW_SIZE];
                for (int py = 0; py < PREVIEW_SIZE; py++) {
                    float v = py / (float) (PREVIEW_SIZE - 1);
                    for (int px = 0; px < PREVIEW_SIZE; px++) {
                        float u = px / (float) (PREVIEW_SIZE - 1);
                        pixels[py * PREVIEW_SIZE + px] = sampleTerrainArgb(level, state, side, u, v);
                    }
                }
                cachedFaces.put(side, pixels);
            }
        }

        private int sampleTerrainArgb(ServerLevel level, SpaceStateSyncPacket state, CubeSide side, float u, float v) {
            double nx = u * 2.0 - 1.0;
            double nz = 1.0 - v * 2.0;
            SamplePoint samplePoint = resolveSamplePoint(state, side, nx, nz);
            int worldX = quantizeToCell(samplePoint.worldX());
            int worldZ = quantizeToCell(samplePoint.worldZ());
            int height = terrainHeight(level, worldX, worldZ);
            BlockPos ground = new BlockPos(worldX, Math.max(level.getMinBuildHeight(), height - 1), worldZ);
            var stateAt = level.getBlockState(ground);
            int eastHeight = terrainHeight(level, worldX + SAMPLE_CELL_BLOCKS, worldZ);
            int westHeight = terrainHeight(level, worldX - SAMPLE_CELL_BLOCKS, worldZ);
            int northHeight = terrainHeight(level, worldX, worldZ - SAMPLE_CELL_BLOCKS);
            int southHeight = terrainHeight(level, worldX, worldZ + SAMPLE_CELL_BLOCKS);
            float slope = clamp((Math.abs(eastHeight - westHeight) + Math.abs(northHeight - southHeight)) / 72.0F, 0.0F, 1.0F);
            boolean shoreline = isWaterNearby(level, worldX, worldZ);

            float red;
            float green;
            float blue;
            if (!level.getFluidState(ground).isEmpty() || stateAt.is(Blocks.WATER)) {
                red = 0.10F; green = 0.28F; blue = 0.62F;
            } else if (stateAt.is(Blocks.SNOW_BLOCK) || stateAt.is(Blocks.SNOW) || stateAt.is(Blocks.ICE) || stateAt.is(Blocks.PACKED_ICE)) {
                red = 0.92F; green = 0.95F; blue = 0.98F;
            } else if (stateAt.is(Blocks.SAND) || stateAt.is(Blocks.RED_SAND) || stateAt.is(Blocks.SANDSTONE)) {
                red = stateAt.is(Blocks.RED_SAND) ? 0.70F : 0.82F;
                green = stateAt.is(Blocks.RED_SAND) ? 0.44F : 0.76F;
                blue = stateAt.is(Blocks.RED_SAND) ? 0.28F : 0.54F;
            } else if (stateAt.is(Blocks.STONE) || stateAt.is(Blocks.DEEPSLATE) || stateAt.is(Blocks.GRAVEL) || stateAt.is(Blocks.COBBLESTONE)) {
                red = 0.46F; green = 0.46F; blue = 0.48F;
            } else if (stateAt.is(Blocks.DIRT) || stateAt.is(Blocks.COARSE_DIRT) || stateAt.is(Blocks.ROOTED_DIRT) || stateAt.is(Blocks.MUD)) {
                red = 0.42F; green = 0.30F; blue = 0.18F;
            } else {
                red = 0.18F; green = 0.46F; blue = 0.20F;
            }

            float elevation = clamp((height - level.getSeaLevel()) / 160.0F, -0.35F, 0.55F);
            float relief = 0.84F + elevation * 0.26F - slope * 0.22F;
            red = clamp((red + elevation * 0.18F) * relief, 0.0F, 1.0F);
            green = clamp((green + elevation * 0.14F) * (relief + 0.02F), 0.0F, 1.0F);
            blue = clamp((blue + elevation * 0.10F) * (0.94F + elevation * 0.06F), 0.0F, 1.0F);
            if (shoreline && !stateAt.is(Blocks.WATER) && level.getFluidState(ground).isEmpty()) {
                red = clamp(red + 0.10F, 0.0F, 1.0F);
                green = clamp(green + 0.08F, 0.0F, 1.0F);
                blue = clamp(blue + 0.02F, 0.0F, 1.0F);
            }
            float alpha = switch (side) {
                case UP -> 0.82F;
                case DOWN -> 0.16F;
                default -> 0.52F;
            };
            return ((int) (alpha * 255.0F) << 24)
                    | ((int) (red * 255.0F) << 16)
                    | ((int) (green * 255.0F) << 8)
                    | (int) (blue * 255.0F);
        }

        private ServerLevel resolveOverworld() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getSingleplayerServer() == null) {
                return null;
            }
            return minecraft.getSingleplayerServer().overworld();
        }

        private SamplePoint resolveSamplePoint(SpaceStateSyncPacket state, CubeSide side, double nx, double nz) {
            double halfSpan = FACE_WORLD_SPAN_BLOCKS * 0.5;
            double centerX = state.stableX();
            double centerZ = state.stableZ();
            return switch (side) {
                case UP -> new SamplePoint(centerX + nx * halfSpan, centerZ + nz * halfSpan);
                case NORTH -> new SamplePoint(centerX + nx * halfSpan, centerZ - FACE_WORLD_SPAN_BLOCKS + nz * halfSpan);
                case SOUTH -> new SamplePoint(centerX - nx * halfSpan, centerZ + FACE_WORLD_SPAN_BLOCKS + nz * halfSpan);
                case EAST -> new SamplePoint(centerX + FACE_WORLD_SPAN_BLOCKS - nz * halfSpan, centerZ + nx * halfSpan);
                case WEST -> new SamplePoint(centerX - FACE_WORLD_SPAN_BLOCKS + nz * halfSpan, centerZ + nx * halfSpan);
                case DOWN -> new SamplePoint(centerX - nx * halfSpan, centerZ + FACE_WORLD_SPAN_BLOCKS * 2.0 - nz * halfSpan);
            };
        }

        private int quantizeToCell(double value) {
            return (int) Math.round(value / SAMPLE_CELL_BLOCKS) * SAMPLE_CELL_BLOCKS;
        }

        private int terrainHeight(ServerLevel level, int worldX, int worldZ) {
            return Math.max(level.getMinBuildHeight() + 1, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ));
        }

        private boolean isWaterNearby(ServerLevel level, int worldX, int worldZ) {
            int[] offsets = {-SAMPLE_CELL_BLOCKS, 0, SAMPLE_CELL_BLOCKS};
            for (int dx : offsets) {
                for (int dz : offsets) {
                    int height = terrainHeight(level, worldX + dx, worldZ + dz);
                    BlockPos pos = new BlockPos(worldX + dx, Math.max(level.getMinBuildHeight(), height - 1), worldZ + dz);
                    if (!level.getFluidState(pos).isEmpty() || level.getBlockState(pos).is(Blocks.WATER)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private float lerp(float start, float end, float delta) {
            return start + (end - start) * delta;
        }

        private record SamplePoint(double worldX, double worldZ) {
        }
    }

    private record TextureSample(float red, float green, float blue, float alpha) {
        private float luminance() {
            return red * 0.2126F + green * 0.7152F + blue * 0.0722F;
        }
    }

    public enum CubeSide {
        NORTH("north", new Vector3f(0.0F, 0.0F, -1.0F)) { Vector3f local(float u, float v) { return new Vector3f(u, v, -1.0F); } },
        SOUTH("south", new Vector3f(0.0F, 0.0F, 1.0F)) { Vector3f local(float u, float v) { return new Vector3f(-u, v, 1.0F); } },
        UP("up", new Vector3f(0.0F, 1.0F, 0.0F)) { Vector3f local(float u, float v) { return new Vector3f(u, 1.0F, v); } },
        DOWN("down", new Vector3f(0.0F, -1.0F, 0.0F)) { Vector3f local(float u, float v) { return new Vector3f(u, -1.0F, -v); } },
        EAST("east", new Vector3f(1.0F, 0.0F, 0.0F)) { Vector3f local(float u, float v) { return new Vector3f(1.0F, v, -u); } },
        WEST("west", new Vector3f(-1.0F, 0.0F, 0.0F)) { Vector3f local(float u, float v) { return new Vector3f(-1.0F, v, u); } };

        private final String fileName;
        private final Vector3f normal;

        CubeSide(String fileName, Vector3f normal) {
            this.fileName = fileName;
            this.normal = normal;
        }

        abstract Vector3f local(float u, float v);

        public Vector3f center() { return local(0.0F, 0.0F); }
        public Vector3f normal() { return new Vector3f(normal); }
        public float textureU(float u, float v) { return clamp(u * 0.5F + 0.5F, 0.0F, 1.0F); }
        public float textureV(float u, float v) { return clamp(0.5F - v * 0.5F, 0.0F, 1.0F); }
    }

    private record CubeFrame(Vector3f center, Vector3f right, Vector3f up, Vector3f forward, float halfSpan, float halfDepth) {
    }

    private record CubeSideDepth(CubeSide side, double depth) {
    }

    private record EarthFace(CubeSide side, Vector3f[] vertices, Vector2f[] uvs) {
    }
}
