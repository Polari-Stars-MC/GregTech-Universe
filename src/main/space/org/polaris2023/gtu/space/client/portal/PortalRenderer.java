package org.polaris2023.gtu.space.client.portal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.polaris2023.gtu.space.client.CubePlanetRenderer;
import org.polaris2023.gtu.space.client.PlanetProxyRenderer;
import org.polaris2023.gtu.space.client.SpaceRenderRuntime;
import org.polaris2023.gtu.space.client.StarfieldRenderer;
import org.polaris2023.gtu.space.client.SunRenderInfo;
import org.polaris2023.gtu.space.client.SunRenderer;
import org.polaris2023.gtu.space.portal.PortalType;

import java.util.Collection;

public final class PortalRenderer {
    private static final float PORTAL_RENDER_DISTANCE = 256.0F;
    private static final float PORTAL_HALF_SIZE = 16.0F;
    private static final StarfieldRenderer STARFIELD = new StarfieldRenderer(600, 0x50525441L);
    private static final SunRenderer SUN = new SunRenderer();

    private PortalRenderer() {
    }

    public static void renderPortals(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        Collection<PortalSyncData> portals = ClientPortalCache.allPortals();
        if (portals.isEmpty()) {
            return;
        }

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        for (PortalSyncData portal : portals) {
            if (!portal.sourceDimension().equals(dimension)) {
                continue;
            }
            double cx = (portal.minX() + portal.maxX()) * 0.5 - camX;
            double cy = (portal.minY() + portal.maxY()) * 0.5 - camY;
            double cz = (portal.minZ() + portal.maxZ()) * 0.5 - camZ;
            double distSq = cx * cx + cy * cy + cz * cz;
            if (distSq > PORTAL_RENDER_DISTANCE * PORTAL_RENDER_DISTANCE) {
                continue;
            }
            renderSinglePortal(poseStack, portal, (float) cx, (float) cy, (float) cz, partialTick);
        }
    }

    private static void renderSinglePortal(
            PoseStack poseStack,
            PortalSyncData portal,
            float relX, float relY, float relZ,
            float partialTick
    ) {
        PoseStack.Pose pose = poseStack.last();

        GL11.glEnable(GL11.GL_STENCIL_TEST);

        // Step 1: Write portal frame to stencil buffer
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder stencilBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        PortalFrameGeometry.renderHorizontalPortalFrame(
                stencilBuffer, pose, relX, relY, relZ, PORTAL_HALF_SIZE,
                1.0F, 1.0F, 1.0F, 1.0F
        );
        BufferUploader.drawWithShader(stencilBuffer.buildOrThrow());

        // Step 2: Render proxy scene only where stencil == 1
        RenderSystem.colorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00);

        if (portal.type() == PortalType.SURFACE_TO_SPACE) {
            renderSpaceProxyScene(poseStack, pose, partialTick);
        } else {
            renderSurfaceProxyScene(pose, relX, relY, relZ);
        }

        // Step 3: Disable stencil, render portal border glow
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.depthMask(false);

        float time = (System.nanoTime() / 1_000_000_000.0F) % 6.283F;
        float pulseAlpha = 0.3F + 0.15F * (float) Math.sin(time * 2.0);

        BufferBuilder borderBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        PortalFrameGeometry.renderPortalBorder(
                borderBuffer, pose, relX, relY, relZ, PORTAL_HALF_SIZE, 1.5F,
                0.4F, 0.6F, 1.0F, pulseAlpha
        );
        BufferUploader.drawWithShader(borderBuffer.buildOrThrow());
        RenderSystem.depthMask(true);
    }

    private static void renderSpaceProxyScene(PoseStack poseStack, PoseStack.Pose pose, float partialTick) {
        // Render stars through the portal
        BufferBuilder starBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        STARFIELD.render(starBuffer, pose, 1800.0F, 0.9F, 1.2F);
        BufferUploader.drawWithShader(starBuffer.buildOrThrow());

        // Render sun if available
        SpaceRenderRuntime runtime = SpaceRenderRuntime.get();
        SunRenderInfo sun = runtime.getSun();
        if (sun != null && sun.intensity() > 0.001F) {
            BufferBuilder sunBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            SUN.render(sunBuffer, pose, sun, 1700.0F);
            BufferUploader.drawWithShader(sunBuffer.buildOrThrow());
        }

        // Render a small cube planet below
        BufferBuilder planetBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        PlanetProxyRenderer.addSolidDisc(
                planetBuffer, pose,
                new Vector3f(0.0F, -0.7F, -0.7F).normalize(),
                new Vector3f(-0.35F, 0.74F, 0.58F).normalize(),
                1200.0F, 280.0F,
                0.28F, 0.42F, 0.86F, 0.85F,
                4, 12
        );
        BufferUploader.drawWithShader(planetBuffer.buildOrThrow());
    }

    private static void renderSurfaceProxyScene(PoseStack.Pose pose, float relX, float relY, float relZ) {
        // Render atmosphere gradient looking down
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float radius = 800.0F;
        float y = relY - 400.0F;

        // Atmosphere layer
        buffer.addVertex(pose, relX - radius, y + 200.0F, relZ - radius).setColor(0.22F, 0.45F, 0.88F, 0.35F);
        buffer.addVertex(pose, relX + radius, y + 200.0F, relZ - radius).setColor(0.22F, 0.45F, 0.88F, 0.35F);
        buffer.addVertex(pose, relX + radius, y + 200.0F, relZ + radius).setColor(0.18F, 0.38F, 0.82F, 0.25F);
        buffer.addVertex(pose, relX - radius, y + 200.0F, relZ + radius).setColor(0.18F, 0.38F, 0.82F, 0.25F);

        // Ground proxy
        buffer.addVertex(pose, relX - radius, y, relZ - radius).setColor(0.12F, 0.28F, 0.08F, 0.55F);
        buffer.addVertex(pose, relX + radius, y, relZ - radius).setColor(0.12F, 0.28F, 0.08F, 0.55F);
        buffer.addVertex(pose, relX + radius, y, relZ + radius).setColor(0.10F, 0.22F, 0.06F, 0.45F);
        buffer.addVertex(pose, relX - radius, y, relZ + radius).setColor(0.10F, 0.22F, 0.06F, 0.45F);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
