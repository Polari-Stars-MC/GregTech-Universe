package org.polaris2023.gtu.space.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class CosmicShapeBuffers {
    private static VertexBuffer solidCube;
    private static VertexBuffer skyboxCube;
    private static VertexBuffer lightMappedCube;

    private CosmicShapeBuffers() {
    }

    public static VertexBuffer solidCube() {
        if (solidCube == null) {
            solidCube = buildSolidCube();
        }
        return solidCube;
    }

    public static VertexBuffer skyboxCube() {
        if (skyboxCube == null) {
            skyboxCube = buildSkyboxCube();
        }
        return skyboxCube;
    }

    public static VertexBuffer lightMappedCube() {
        if (lightMappedCube == null) {
            lightMappedCube = buildLightMappedCube();
        }
        return lightMappedCube;
    }

    private static VertexBuffer buildSolidCube() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addSolidCube(builder, -1);
        return upload(builder);
    }

    private static VertexBuffer buildSkyboxCube() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addSkyboxCube(builder, -1);
        return upload(builder);
    }

    private static VertexBuffer buildLightMappedCube() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addLightMappedCube(builder);
        return upload(builder);
    }

    private static VertexBuffer upload(BufferBuilder builder) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }

    // Ported from Cosmic Horizons Texturedcube/Skyboxshape procedures to 1.21.1 builder APIs.
    private static void addSolidCube(BufferBuilder builder, int argb) {
        float size = 0.5F;
        addColorVertex(builder, size, -size, -size, argb);
        addColorVertex(builder, size, -size, size, argb);
        addColorVertex(builder, -size, -size, size, argb);
        addColorVertex(builder, -size, -size, -size, argb);

        addColorVertex(builder, -size, size, -size, argb);
        addColorVertex(builder, -size, size, size, argb);
        addColorVertex(builder, size, size, size, argb);
        addColorVertex(builder, size, size, -size, argb);

        addColorVertex(builder, size, size, -size, argb);
        addColorVertex(builder, size, -size, -size, argb);
        addColorVertex(builder, -size, -size, -size, argb);
        addColorVertex(builder, -size, size, -size, argb);

        addColorVertex(builder, -size, size, size, argb);
        addColorVertex(builder, -size, -size, size, argb);
        addColorVertex(builder, size, -size, size, argb);
        addColorVertex(builder, size, size, size, argb);

        addColorVertex(builder, -size, size, -size, argb);
        addColorVertex(builder, -size, -size, -size, argb);
        addColorVertex(builder, -size, -size, size, argb);
        addColorVertex(builder, -size, size, size, argb);

        addColorVertex(builder, size, size, size, argb);
        addColorVertex(builder, size, -size, size, argb);
        addColorVertex(builder, size, -size, -size, argb);
        addColorVertex(builder, size, size, -size, argb);
    }

    private static void addSkyboxCube(BufferBuilder builder, int argb) {
        addTexColorVertex(builder, -0.5F, -0.5F, -0.5F, 0.0F, 0.0F, argb);
        addTexColorVertex(builder, -0.5F, -0.5F, 0.5F, 0.0F, 0.5F, argb);
        addTexColorVertex(builder, 0.5F, -0.5F, 0.5F, 0.33333334F, 0.5F, argb);
        addTexColorVertex(builder, 0.5F, -0.5F, -0.5F, 0.33333334F, 0.0F, argb);

        addTexColorVertex(builder, -0.5F, 0.5F, 0.5F, 0.33333334F, 0.0F, argb);
        addTexColorVertex(builder, -0.5F, 0.5F, -0.5F, 0.33333334F, 0.5F, argb);
        addTexColorVertex(builder, 0.5F, 0.5F, -0.5F, 0.6666667F, 0.5F, argb);
        addTexColorVertex(builder, 0.5F, 0.5F, 0.5F, 0.6666667F, 0.0F, argb);

        addTexColorVertex(builder, 0.5F, 0.5F, 0.5F, 0.6666667F, 0.0F, argb);
        addTexColorVertex(builder, 0.5F, -0.5F, 0.5F, 0.6666667F, 0.5F, argb);
        addTexColorVertex(builder, -0.5F, -0.5F, 0.5F, 1.0F, 0.5F, argb);
        addTexColorVertex(builder, -0.5F, 0.5F, 0.5F, 1.0F, 0.0F, argb);

        addTexColorVertex(builder, -0.5F, 0.5F, 0.5F, 0.0F, 0.5F, argb);
        addTexColorVertex(builder, -0.5F, -0.5F, 0.5F, 0.0F, 1.0F, argb);
        addTexColorVertex(builder, -0.5F, -0.5F, -0.5F, 0.33333334F, 1.0F, argb);
        addTexColorVertex(builder, -0.5F, 0.5F, -0.5F, 0.33333334F, 0.5F, argb);

        addTexColorVertex(builder, -0.5F, 0.5F, -0.5F, 0.33333334F, 0.5F, argb);
        addTexColorVertex(builder, -0.5F, -0.5F, -0.5F, 0.33333334F, 1.0F, argb);
        addTexColorVertex(builder, 0.5F, -0.5F, -0.5F, 0.6666667F, 1.0F, argb);
        addTexColorVertex(builder, 0.5F, 0.5F, -0.5F, 0.6666667F, 0.5F, argb);

        addTexColorVertex(builder, 0.5F, 0.5F, -0.5F, 0.6666667F, 0.5F, argb);
        addTexColorVertex(builder, 0.5F, -0.5F, -0.5F, 0.6666667F, 1.0F, argb);
        addTexColorVertex(builder, 0.5F, -0.5F, 0.5F, 1.0F, 1.0F, argb);
        addTexColorVertex(builder, 0.5F, 0.5F, 0.5F, 1.0F, 0.5F, argb);
    }

    // Ported from CH LightCubeMapProcedure + CubeVertexOrientorProcedure with plane_angle=0.
    private static void addLightMappedCube(BufferBuilder builder) {
        addFace(builder, Face.UP, 0.125F, 0.00F, 0.25F, 0.25F, -1);
        addFace(builder, Face.DOWN, 0.375F, 0.00F, 0.25F, 0.25F, -6250336);
        addFace(builder, Face.NORTH, 0.25F, 0.25F, 0.125F, 0.25F, -2039584);
        addFace(builder, Face.SOUTH, 0.00F, 0.25F, 0.125F, 0.25F, -4144960);
        addFace(builder, Face.WEST, 0.375F, 0.25F, 0.125F, 0.25F, -4144960);
        addFace(builder, Face.EAST, 0.125F, 0.25F, 0.125F, 0.25F, -2039584);
    }

    private static void addFace(BufferBuilder builder, Face face, float u0, float v0, float du, float dv, int argb) {
        addTexColorVertex(builder, face.x1, face.y1, face.z1, u0, v0, argb);
        addTexColorVertex(builder, face.x2, face.y2, face.z2, u0, v0 + dv, argb);
        addTexColorVertex(builder, face.x3, face.y3, face.z3, u0 + du, v0 + dv, argb);
        addTexColorVertex(builder, face.x4, face.y4, face.z4, u0 + du, v0, argb);
    }

    private static void addColorVertex(BufferBuilder builder, float x, float y, float z, int argb) {
        builder.addVertex(x, y, z).setColor(argb);
    }

    private static void addTexColorVertex(BufferBuilder builder, float x, float y, float z, float u, float v, int argb) {
        builder.addVertex(x, y, z).setUv(u, v).setColor(argb);
    }

    private enum Face {
        UP(-0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F),
        DOWN(0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, -0.5F),
        NORTH(0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F),
        SOUTH(-0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F),
        WEST(-0.5F, 0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F),
        EAST(0.5F, 0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F);

        final float x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;

        Face(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.x3 = x3; this.y3 = y3; this.z3 = z3;
            this.x4 = x4; this.y4 = y4; this.z4 = z4;
        }
    }
}
