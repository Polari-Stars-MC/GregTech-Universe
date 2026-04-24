package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StarfieldRenderer {
    private final List<Star> stars;

    public StarfieldRenderer(int count, long seed) {
        this.stars = generateStars(count, seed);
    }

    public void render(BufferBuilder buffer, PoseStack.Pose pose, float renderRadius, float alphaScale, float twinkleSpeed) {
        if (alphaScale <= 0.001F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float partialTick = minecraft.getFrameTimeNs() / 50_000_000.0F;
        float time = (minecraft.level.getGameTime() + partialTick) * Math.max(0.01F, twinkleSpeed) * 0.015F;
        for (Star star : stars) {
            float alpha = Mth.clamp(star.alphaBase + Mth.sin(time * star.twinkleSpeed + star.phase) * 0.18F, 0.10F, 1.0F);
            PlanetProxyRenderer.addBillboard(buffer, pose, star.direction, renderRadius, star.size, star.red, star.green, star.blue, alpha * alphaScale);
        }
    }

    private static List<Star> generateStars(int count, long seed) {
        Random random = new Random(seed);
        List<Star> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Vector3f direction = randomUnitVector(random);
            float size = 0.12F + random.nextFloat() * 0.45F;
            float colorBias = random.nextFloat();
            float red = 0.72F + colorBias * 0.28F;
            float green = 0.76F + random.nextFloat() * 0.24F;
            float blue = 0.82F + random.nextFloat() * 0.18F;
            float alphaBase = 0.45F + random.nextFloat() * 0.5F;
            float speed = 0.7F + random.nextFloat() * 2.5F;
            float phase = random.nextFloat() * Mth.TWO_PI;
            result.add(new Star(direction, size, red, green, blue, alphaBase, speed, phase));
        }
        return List.copyOf(result);
    }

    private static Vector3f randomUnitVector(Random random) {
        float z = random.nextFloat() * 2.0F - 1.0F;
        float theta = random.nextFloat() * Mth.TWO_PI;
        float radial = Mth.sqrt(Math.max(0.0F, 1.0F - z * z));
        return new Vector3f(radial * Mth.cos(theta), z, radial * Mth.sin(theta));
    }

    private record Star(
            Vector3f direction,
            float size,
            float red,
            float green,
            float blue,
            float alphaBase,
            float twinkleSpeed,
            float phase
    ) {
    }
}
