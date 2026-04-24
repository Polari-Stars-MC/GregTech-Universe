package org.polaris2023.gtu.space.client.portal;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

public final class PortalFrameGeometry {
    private static final float DEFAULT_HALF_SIZE = 16.0F;

    private PortalFrameGeometry() {
    }

    public static void renderHorizontalPortalFrame(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            float centerX, float centerY, float centerZ,
            float halfSize,
            float red, float green, float blue, float alpha
    ) {
        float x0 = centerX - halfSize;
        float x1 = centerX + halfSize;
        float z0 = centerZ - halfSize;
        float z1 = centerZ + halfSize;
        float y = centerY;

        buffer.addVertex(pose, x0, y, z0).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x1, y, z0).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x1, y, z1).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x0, y, z1).setColor(red, green, blue, alpha);
    }

    public static void renderPortalBorder(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            float centerX, float centerY, float centerZ,
            float halfSize,
            float borderWidth,
            float red, float green, float blue, float alpha
    ) {
        float inner = halfSize;
        float outer = halfSize + borderWidth;
        float y = centerY;

        addBorderStrip(buffer, pose, centerX - outer, centerX - inner, centerZ - outer, centerZ + outer, y, red, green, blue, alpha);
        addBorderStrip(buffer, pose, centerX + inner, centerX + outer, centerZ - outer, centerZ + outer, y, red, green, blue, alpha);
        addBorderStrip(buffer, pose, centerX - inner, centerX + inner, centerZ - outer, centerZ - inner, y, red, green, blue, alpha);
        addBorderStrip(buffer, pose, centerX - inner, centerX + inner, centerZ + inner, centerZ + outer, y, red, green, blue, alpha);
    }

    private static void addBorderStrip(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            float x0, float x1, float z0, float z1, float y,
            float red, float green, float blue, float alpha
    ) {
        buffer.addVertex(pose, x0, y, z0).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x1, y, z0).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x1, y, z1).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x0, y, z1).setColor(red, green, blue, alpha);
    }
}
