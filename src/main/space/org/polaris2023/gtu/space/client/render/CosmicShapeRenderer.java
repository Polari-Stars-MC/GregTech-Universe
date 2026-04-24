package org.polaris2023.gtu.space.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class CosmicShapeRenderer {
    private CosmicShapeRenderer() {
    }

    // Ported from Cosmic Horizons RenderMINTProcedure.renderShape to 1.21.1 APIs.
    public static void renderShape(
            VertexBuffer shape,
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            float x,
            float y,
            float z,
            float yawDegrees,
            float pitchDegrees,
            float rollDegrees,
            float scaleX,
            float scaleY,
            float scaleZ,
            int argb
    ) {
        if (shape == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yawDegrees)));
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(pitchDegrees)));
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(rollDegrees)));
        poseStack.scale(scaleX, scaleY, scaleZ);

        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, alpha);

        Matrix4f modelView = new Matrix4f(poseStack.last().pose());
        shape.bind();
        shape.drawWithShader(
                modelView,
                projectionMatrix,
                shape.getFormat().hasUV(0) ? GameRenderer.getPositionTexColorShader() : GameRenderer.getPositionColorShader()
        );
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static void renderTexturedShape(
            VertexBuffer shape,
            ResourceLocation texture,
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            float x,
            float y,
            float z,
            float yawDegrees,
            float pitchDegrees,
            float rollDegrees,
            float scaleX,
            float scaleY,
            float scaleZ,
            int argb
    ) {
        if (shape == null || texture == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yawDegrees)));
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(pitchDegrees)));
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(rollDegrees)));
        poseStack.scale(scaleX, scaleY, scaleZ);

        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red, green, blue, alpha);

        Matrix4f modelView = new Matrix4f(poseStack.last().pose());
        shape.bind();
        shape.drawWithShader(modelView, projectionMatrix, GameRenderer.getPositionTexColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
