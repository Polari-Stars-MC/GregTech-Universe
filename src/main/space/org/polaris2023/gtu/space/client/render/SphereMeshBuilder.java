package org.polaris2023.gtu.space.client.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;

public class SphereMeshBuilder {
    private static final int LOD_LEVELS = 20;
    private static final VertexBuffer[] sphereBuffers = new VertexBuffer[LOD_LEVELS];

    public static void buildAll() {
        for (int i = 0; i < LOD_LEVELS; i++) {
            if (sphereBuffers[i] != null) continue;
            int precision = i + 2;
            sphereBuffers[i] = buildSphere(precision);
        }
    }

    private static VertexBuffer buildSphere(int precision) {
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);

        for (int lat = 0; lat < precision; lat++) {
            double theta0 = Math.PI * lat / precision;
            double theta1 = Math.PI * (lat + 1) / precision;
            double sinT0 = Math.sin(theta0), cosT0 = Math.cos(theta0);
            double sinT1 = Math.sin(theta1), cosT1 = Math.cos(theta1);

            for (int lon = 0; lon < precision * 2; lon++) {
                double phi0 = Math.PI * 2 * lon / (precision * 2);
                double phi1 = Math.PI * 2 * (lon + 1) / (precision * 2);
                double sinP0 = Math.sin(phi0), cosP0 = Math.cos(phi0);
                double sinP1 = Math.sin(phi1), cosP1 = Math.cos(phi1);

                float x00 = (float) (sinT0 * cosP0);
                float y00 = (float) cosT0;
                float z00 = (float) (sinT0 * sinP0);
                float u00 = (float) lon / (precision * 2);
                float v00 = (float) lat / precision;

                float x10 = (float) (sinT0 * cosP1);
                float y10 = (float) cosT0;
                float z10 = (float) (sinT0 * sinP1);
                float u10 = (float) (lon + 1) / (precision * 2);
                float v10 = v00;

                float x01 = (float) (sinT1 * cosP0);
                float y01 = (float) cosT1;
                float z01 = (float) (sinT1 * sinP0);
                float u01 = u00;
                float v01 = (float) (lat + 1) / precision;

                float x11 = (float) (sinT1 * cosP1);
                float y11 = (float) cosT1;
                float z11 = (float) (sinT1 * sinP1);
                float u11 = u10;
                float v11 = v01;

                addVertex(builder, x00, y00, z00, u00, v00);
                addVertex(builder, x01, y01, z01, u01, v01);
                addVertex(builder, x11, y11, z11, u11, v11);

                addVertex(builder, x00, y00, z00, u00, v00);
                addVertex(builder, x11, y11, z11, u11, v11);
                addVertex(builder, x10, y10, z10, u10, v10);
            }
        }

        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return vb;
    }

    private static void addVertex(BufferBuilder builder, float x, float y, float z, float u, float v) {
        builder.addVertex(x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(0xF000F0)
                .setNormal(x, y, z);
    }

    public static int selectLOD(double renderDistance, double renderDiameter) {
        double viewingAngle = Math.atan2(renderDiameter, renderDistance);
        double fov = Math.toRadians(70.0);
        double renderSize = viewingAngle / fov;

        if (renderSize < 0.01) return 0;
        if (renderSize < 0.05) return 2;
        if (renderSize < 0.1) return 5;
        if (renderSize < 0.3) return 10;
        if (renderSize < 0.6) return 15;
        return LOD_LEVELS - 1;
    }

    public static VertexBuffer getBuffer(int lod) {
        if (lod < 0) lod = 0;
        if (lod >= LOD_LEVELS) lod = LOD_LEVELS - 1;
        if (sphereBuffers[lod] == null) buildAll();
        return sphereBuffers[lod];
    }

    public static void drawSphere(int lod, org.joml.Matrix4f modelView, org.joml.Matrix4f projection, ShaderInstance shader) {
        VertexBuffer vb = getBuffer(lod);
        if (vb == null) return;
        vb.bind();
        vb.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
    }
}
