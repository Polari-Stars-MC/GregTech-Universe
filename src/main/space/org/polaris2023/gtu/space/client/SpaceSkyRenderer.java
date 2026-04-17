package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.Direction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.runtime.solar.CelestialRenderState;
import org.polaris2023.gtu.space.runtime.solar.CelestialRenderShape;
import org.polaris2023.gtu.space.runtime.solar.SolarSystemBackgroundRuntime;
import org.polaris2023.gtu.space.runtime.solar.SolarSystemDefinition;
import org.polaris2023.gtu.space.runtime.solar.SolarSystemFrame;
import org.polaris2023.gtu.space.runtime.solar.SolarSystemRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class SpaceSkyRenderer {
    private static final float SKY_RADIUS = 2048.0F;
    private static final float STAR_RADIUS = 1800.0F;
    private static final SolarSystemBackgroundRuntime SOLAR_SYSTEM =
            new SolarSystemBackgroundRuntime(new SolarSystemRuntime(SolarSystemDefinition.earthLike()));
    private static final List<Star> STARS = generateStars(900, 0x475455370BL);

    private SpaceSkyRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldRenderSpaceSky(minecraft)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(camera.rotation()).conjugate());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        SOLAR_SYSTEM.ensureStarted();
        SOLAR_SYSTEM.syncTime(minecraft.level.getDayTime());
        SolarSystemFrame frame = SOLAR_SYSTEM.latestFrame();

        renderSkyShell(poseStack);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        renderStars(poseStack, partialTick, frame);
        renderBodies(poseStack, frame);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }

    private static boolean shouldRenderSpaceSky(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null;
    }

    private static void renderSkyShell(PoseStack poseStack) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        PoseStack.Pose pose = poseStack.last();

        addCubeFace(buffer, pose, SKY_RADIUS, AxisFace.UP, 4, 6, 12, 255);
        addCubeFace(buffer, pose, SKY_RADIUS, AxisFace.DOWN, 2, 3, 8, 255);
        addCubeFace(buffer, pose, SKY_RADIUS, AxisFace.NORTH, 3, 5, 10, 255);
        addCubeFace(buffer, pose, SKY_RADIUS, AxisFace.SOUTH, 3, 4, 9, 255);
        addCubeFace(buffer, pose, SKY_RADIUS, AxisFace.EAST, 2, 4, 8, 255);
        addCubeFace(buffer, pose, SKY_RADIUS, AxisFace.WEST, 2, 4, 8, 255);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderStars(PoseStack poseStack, float partialTick, SolarSystemFrame frame) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        PoseStack.Pose pose = poseStack.last();
        float twinkleTime = (minecraft.level.getGameTime() + partialTick) * 0.015F;
        float skyAngle = frame.dayFraction() * Mth.TWO_PI;

        for (Star star : STARS) {
            float alpha = Mth.clamp(star.alphaBase + Mth.sin(twinkleTime * star.twinkleSpeed + star.phase) * 0.18F, 0.15F, 1.0F);
            Vector3f direction = applySkyRotation(star.direction, skyAngle);
            addBillboard(buffer, pose, direction, STAR_RADIUS, star.size, star.red, star.green, star.blue, alpha);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderBodies(PoseStack poseStack, SolarSystemFrame frame) {
        PoseStack.Pose pose = poseStack.last();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (CelestialRenderState body : frame.bodies()) {
            if (body.glowEnabled()) {
                addBillboard(buffer, pose, body.direction(), body.renderRadius() - 1.0F, body.glowSize(), body.red(), body.green(), body.blue(), Math.min(body.alpha(), 0.24F));
            }
            if (body.renderShape() == CelestialRenderShape.SPHERE) {
                drawSphere(
                        buffer,
                        pose,
                        body.direction(),
                        body.renderRadius(),
                        body.renderSize() * 0.5F,
                        body.red(),
                        body.green(),
                        body.blue(),
                        body.alpha()
                );
            } else {
                addBillboard(buffer, pose, body.direction(), body.renderRadius(), body.renderSize(), body.red(), body.green(), body.blue(), body.alpha());
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addBillboard(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            float radius,
            float size,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vector3f forward = new Vector3f(direction).normalize();
        Vector3f referenceUp = Math.abs(forward.y()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = forward.cross(referenceUp, new Vector3f()).normalize(size);
        Vector3f up = right.cross(forward, new Vector3f()).normalize(size);
        Vector3f center = new Vector3f(forward).mul(radius);

        Vector3f v0 = new Vector3f(center).sub(right).sub(up);
        Vector3f v1 = new Vector3f(center).add(right).sub(up);
        Vector3f v2 = new Vector3f(center).add(right).add(up);
        Vector3f v3 = new Vector3f(center).sub(right).add(up);

        putVertex(buffer, pose, v0, red, green, blue, alpha);
        putVertex(buffer, pose, v1, red, green, blue, alpha);
        putVertex(buffer, pose, v2, red, green, blue, alpha);
        putVertex(buffer, pose, v3, red, green, blue, alpha);
    }

    private static void drawSphere(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            Vector3f direction,
            float radiusFromViewer,
            float sphereRadius,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vector3f center = new Vector3f(direction).normalize().mul(radiusFromViewer);
        Vector3f viewNormal = new Vector3f(direction).normalize().negate();
        Vector3f referenceUp = Math.abs(viewNormal.y()) > 0.92F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f tangent = viewNormal.cross(referenceUp, new Vector3f()).normalize();
        Vector3f bitangent = tangent.cross(viewNormal, new Vector3f()).normalize();
        Vector3f light = new Vector3f(-0.35F, 0.45F, -1.0F).normalize();

        int latSegments = 8;
        int lonSegments = 16;

        for (int lat = 0; lat < latSegments; lat++) {
            float v0 = lat / (float) latSegments;
            float v1 = (lat + 1) / (float) latSegments;
            float phi0 = (v0 - 0.5F) * Mth.PI;
            float phi1 = (v1 - 0.5F) * Mth.PI;

            for (int lon = 0; lon < lonSegments; lon++) {
                float u0 = lon / (float) lonSegments;
                float u1 = (lon + 1) / (float) lonSegments;
                float theta0 = u0 * Mth.TWO_PI;
                float theta1 = u1 * Mth.TWO_PI;

                SphereVertex a = sphereVertex(center, tangent, bitangent, viewNormal, sphereRadius, phi0, theta0, light, red, green, blue, alpha);
                SphereVertex b = sphereVertex(center, tangent, bitangent, viewNormal, sphereRadius, phi1, theta0, light, red, green, blue, alpha);
                SphereVertex c = sphereVertex(center, tangent, bitangent, viewNormal, sphereRadius, phi1, theta1, light, red, green, blue, alpha);
                SphereVertex d = sphereVertex(center, tangent, bitangent, viewNormal, sphereRadius, phi0, theta1, light, red, green, blue, alpha);

                putVertex(buffer, pose, a.position, a.red, a.green, a.blue, a.alpha);
                putVertex(buffer, pose, b.position, b.red, b.green, b.blue, b.alpha);
                putVertex(buffer, pose, c.position, c.red, c.green, c.blue, c.alpha);

                putVertex(buffer, pose, a.position, a.red, a.green, a.blue, a.alpha);
                putVertex(buffer, pose, c.position, c.red, c.green, c.blue, c.alpha);
                putVertex(buffer, pose, d.position, d.red, d.green, d.blue, d.alpha);
            }
        }
    }

    private static SphereVertex sphereVertex(
            Vector3f center,
            Vector3f tangent,
            Vector3f bitangent,
            Vector3f forward,
            float sphereRadius,
            float phi,
            float theta,
            Vector3f light,
            float baseRed,
            float baseGreen,
            float baseBlue,
            float alpha
    ) {
        float cosPhi = Mth.cos(phi);
        float sinPhi = Mth.sin(phi);
        float cosTheta = Mth.cos(theta);
        float sinTheta = Mth.sin(theta);

        Vector3f normal = new Vector3f(tangent).mul(cosPhi * cosTheta)
                .add(new Vector3f(bitangent).mul(cosPhi * sinTheta))
                .add(new Vector3f(forward).mul(sinPhi))
                .normalize();

        Vector3f position = new Vector3f(center).add(new Vector3f(normal).mul(sphereRadius));
        float diffuse = Math.max(0.35F, normal.dot(light) * 0.65F + 0.35F);

        return new SphereVertex(
                position,
                baseRed * diffuse,
                baseGreen * diffuse,
                baseBlue * diffuse,
                alpha
        );
    }

    private static void addCubeFace(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            float radius,
            AxisFace face,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float r = radius;
        switch (face) {
            case UP -> {
                putVertex(buffer, pose, -r, r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, r, r, red, green, blue, alpha);
                putVertex(buffer, pose, r, r, r, red, green, blue, alpha);
                putVertex(buffer, pose, r, r, -r, red, green, blue, alpha);
            }
            case DOWN -> {
                putVertex(buffer, pose, -r, -r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, r, -r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, r, -r, r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, -r, r, red, green, blue, alpha);
            }
            case NORTH -> {
                putVertex(buffer, pose, -r, -r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, r, r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, r, -r, -r, red, green, blue, alpha);
            }
            case SOUTH -> {
                putVertex(buffer, pose, -r, -r, r, red, green, blue, alpha);
                putVertex(buffer, pose, r, -r, r, red, green, blue, alpha);
                putVertex(buffer, pose, r, r, r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, r, r, red, green, blue, alpha);
            }
            case EAST -> {
                putVertex(buffer, pose, r, -r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, r, r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, r, r, r, red, green, blue, alpha);
                putVertex(buffer, pose, r, -r, r, red, green, blue, alpha);
            }
            case WEST -> {
                putVertex(buffer, pose, -r, -r, -r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, -r, r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, r, r, red, green, blue, alpha);
                putVertex(buffer, pose, -r, r, -r, red, green, blue, alpha);
            }
        }
    }

    private static void putVertex(BufferBuilder buffer, PoseStack.Pose pose, Vector3f vector, float red, float green, float blue, float alpha) {
        buffer.addVertex(pose, vector.x(), vector.y(), vector.z()).setColor(red, green, blue, alpha);
    }

    private static void putVertex(BufferBuilder buffer, PoseStack.Pose pose, float x, float y, float z, int red, int green, int blue, int alpha) {
        buffer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
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
            float twinkleSpeed = 0.7F + random.nextFloat() * 2.5F;
            float phase = random.nextFloat() * Mth.TWO_PI;
            result.add(new Star(direction, size, red, green, blue, alphaBase, twinkleSpeed, phase));
        }

        return result;
    }

    private static Vector3f randomUnitVector(Random random) {
        float z = random.nextFloat() * 2.0F - 1.0F;
        float theta = random.nextFloat() * Mth.TWO_PI;
        float radial = Mth.sqrt(Math.max(0.0F, 1.0F - z * z));
        return new Vector3f(radial * Mth.cos(theta), z, radial * Mth.sin(theta));
    }

    private static Vector3f applySkyRotation(Vector3f vector, float skyAngle) {
        Vector3f rotated = new Vector3f(vector);
        rotated.rotateZ(skyAngle);
        rotated.add(new Vector3f(Direction.NORTH.getStepX(), Direction.NORTH.getStepY(), Direction.NORTH.getStepZ()).mul(rotated.y * SOLAR_SYSTEM.definition().axialTilt()));
        return rotated.normalize();
    }

    private static Vector3f directionVector(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private enum AxisFace {
        UP,
        DOWN,
        NORTH,
        SOUTH,
        EAST,
        WEST
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

    private record SphereVertex(
            Vector3f position,
            float red,
            float green,
            float blue,
            float alpha
    ) {
    }

}
