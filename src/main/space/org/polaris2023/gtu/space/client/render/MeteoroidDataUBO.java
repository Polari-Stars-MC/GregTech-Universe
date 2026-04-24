package org.polaris2023.gtu.space.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class MeteoroidDataUBO extends UniformBuffer {
    private final int maxCount;

    public MeteoroidDataUBO(int bindingPoint) {
        super(bindingPoint, ((GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE) == 65536) ? 120 : 28) * 528 + 16);
        this.maxCount = (GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE) == 65536) ? 120 : 28;
    }

    public void updateEmpty() {
        ByteBuffer buffer = MemoryUtil.memCalloc(getSize());
        buffer.putInt(0);
        buffer.putInt(0).putInt(0).putInt(0);
        for (int i = 0; i < maxCount; i++) {
            for (int j = 0; j < 528 / 4; j++) buffer.putFloat(0f);
        }
        buffer.flip();
        update(buffer, false);
    }

    public int getMaxCount() {
        return maxCount;
    }
}
