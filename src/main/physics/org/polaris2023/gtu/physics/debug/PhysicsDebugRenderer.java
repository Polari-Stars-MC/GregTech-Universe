package org.polaris2023.gtu.physics.debug;

import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.PhysicsConstants;
import org.polaris2023.gtu.physics.inertia.InertiaManager;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.init.DataComponents;
import org.polaris2023.gtu.physics.load.PlayerLoadManager;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物理调试渲染器
 * <p>
 * 绘制运动轨迹抛物线和加速度曲线
 */
public class PhysicsDebugRenderer {

    private static final Map<Integer, MotionTracker> trackers = new ConcurrentHashMap<>();
    private static boolean enabled = false;
    private static final int PREDICTION_STEPS = 50;

    /**
     * 曲线图参数
     */
    private static final int GRAPH_WIDTH = 150;      // 图表宽度
    private static final int GRAPH_HEIGHT = 60;      // 图表高度
    private static final int GRAPH_RIGHT_MARGIN = 10; // 图表右边距

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static MotionTracker getOrCreateTracker(Entity entity) {
        return trackers.computeIfAbsent(entity.getId(), id -> new MotionTracker());
    }

    public static void recordEntity(Entity entity, long tick) {
        if (!enabled) return;
        MotionTracker tracker = getOrCreateTracker(entity);
        tracker.record(entity, tick);
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Vec3 camera = new Vec3(camX, camY, camZ);

        // 渲染 Bullet 刚体碰撞形状
        renderBulletCollisionShapes(poseStack, bufferSource, camera);

        MotionTracker tracker = trackers.get(player.getId());
        if (tracker == null) return;

        // 渲染3D轨迹
        renderTrajectory(poseStack, bufferSource, tracker.getPositionHistory(), camera, 1.0f, 1.0f, 0.0f);

        List<Vector3f> prediction = tracker.predictParabola(
                PREDICTION_STEPS,
                -player.level().getData(DataAttachments.DIMENSION_PHYSICS.get()).gravity,
                PhysicsConstants.SECONDS_PER_TICK
        );
        renderTrajectory(poseStack, bufferSource, prediction, camera, 1.0f, 0.3f, 0.3f);

        renderVelocityVector(poseStack, bufferSource, player, tracker, camera);
        renderAccelerationVector(poseStack, bufferSource, player, tracker, camera);
    }

    /**
     * 渲染 Bullet 刚体碰撞形状
     */
    private static void renderBulletCollisionShapes(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        var bodies = org.polaris2023.gtu.physics.network.ClientBulletCache.getAll();
        if (bodies.isEmpty()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        for (var entry : bodies.entrySet()) {
            int entityId = entry.getKey();
            var data = entry.getValue();

            // 相对于相机的位置
            float relX = (float) (data.x() - camera.x);
            float relY = (float) (data.y() - camera.y);
            float relZ = (float) (data.z() - camera.z);

            // 根据形状类型渲染
            if (data.shapeType() == 1) {
                // 胶囊形状
                renderCapsuleWireframe(consumer, last, relX, relY, relZ, data.param1(), data.param2(), 1.0f, 0.5f, 0.0f);
            } else {
                // 盒子形状
                renderBoxWireframe(consumer, last, relX, relY, relZ, data.param1(), data.param2(), data.param3(), 0.0f, 1.0f, 1.0f);
            }
        }
    }

    /**
     * 渲染盒子碰撞框（线框）
     */
    private static void renderBoxWireframe(VertexConsumer consumer, PoseStack.Pose last,
                                           float cx, float cy, float cz,
                                           float hx, float hy, float hz,
                                           float r, float g, float b) {
        // 8个顶点
        float x0 = cx - hx, x1 = cx + hx;
        float y0 = cy - hy, y1 = cy + hy;
        float z0 = cz - hz, z1 = cz + hz;

        // 12条边
        // 底面
        addLine(consumer, last, x0, y0, z0, x1, y0, z0, r, g, b);
        addLine(consumer, last, x1, y0, z0, x1, y0, z1, r, g, b);
        addLine(consumer, last, x1, y0, z1, x0, y0, z1, r, g, b);
        addLine(consumer, last, x0, y0, z1, x0, y0, z0, r, g, b);
        // 顶面
        addLine(consumer, last, x0, y1, z0, x1, y1, z0, r, g, b);
        addLine(consumer, last, x1, y1, z0, x1, y1, z1, r, g, b);
        addLine(consumer, last, x1, y1, z1, x0, y1, z1, r, g, b);
        addLine(consumer, last, x0, y1, z1, x0, y1, z0, r, g, b);
        // 垂直边
        addLine(consumer, last, x0, y0, z0, x0, y1, z0, r, g, b);
        addLine(consumer, last, x1, y0, z0, x1, y1, z0, r, g, b);
        addLine(consumer, last, x1, y0, z1, x1, y1, z1, r, g, b);
        addLine(consumer, last, x0, y0, z1, x0, y1, z1, r, g, b);
    }

    /**
     * 渲染胶囊碰撞框（简化为圆柱+半球）
     */
    private static void renderCapsuleWireframe(VertexConsumer consumer, PoseStack.Pose last,
                                               float cx, float cy, float cz,
                                               float radius, float cylinderHeight,
                                               float r, float g, float b) {
        // 胶囊中心在 cy，总高度 = cylinderHeight + 2*radius
        float halfHeight = cylinderHeight / 2;

        // 绘制圆环（Y轴方向胶囊）
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
            float cos1 = (float) Math.cos(angle1) * radius;
            float sin1 = (float) Math.sin(angle1) * radius;
            float cos2 = (float) Math.cos(angle2) * radius;
            float sin2 = (float) Math.sin(angle2) * radius;

            // 顶部圆环
            addLine(consumer, last, cx + cos1, cy + halfHeight + radius, cz + sin1,
                    cx + cos2, cy + halfHeight + radius, cz + sin2, r, g, b);
            // 底部圆环
            addLine(consumer, last, cx + cos1, cy - halfHeight - radius, cz + sin1,
                    cx + cos2, cy - halfHeight - radius, cz + sin2, r, g, b);
            // 中部圆环（圆柱部分上下）
            addLine(consumer, last, cx + cos1, cy + halfHeight, cz + sin1,
                    cx + cos2, cy + halfHeight, cz + sin2, r, g, b);
            addLine(consumer, last, cx + cos1, cy - halfHeight, cz + sin1,
                    cx + cos2, cy - halfHeight, cz + sin2, r, g, b);
            // 垂直线
            addLine(consumer, last, cx + cos1, cy - halfHeight, cz + sin1,
                    cx + cos1, cy + halfHeight, cz + sin1, r, g, b);
        }
    }

    /**
     * 添加一条线段
     */
    private static void addLine(VertexConsumer consumer, PoseStack.Pose last,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b) {
        consumer.addVertex(last, x1, y1, z1).setColor(r, g, b, 1.0f).setNormal(last, 1.0f, 0.0f, 0.0f);
        consumer.addVertex(last, x2, y2, z2).setColor(r, g, b, 1.0f).setNormal(last, 1.0f, 0.0f, 0.0f);
    }

    /**
     * 渲染2D HUD (在渲染结束后单独调用)
     */
    public static void renderHud(GuiGraphics guiGraphics, MotionTracker tracker) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null) return;

        MotionTracker.MotionStats stats = tracker.getStats();

        // 左上角信息
        List<Component> info = new ArrayList<>();
        info.add(Component.literal(String.format("§e速度: §f%.2f m/s", stats.currentSpeed)));
        info.add(Component.literal(String.format("§b速度向量: §f(%.2f, %.2f, %.2f)",
                stats.currentVelocity.x, stats.currentVelocity.y, stats.currentVelocity.z)));
        info.add(Component.literal(String.format("§9加速度: §f%.2f m/s²", stats.accelerationMagnitude)));
        info.add(Component.literal(String.format("§9加速度向量: §f(%.2f, %.2f, %.2f)",
                stats.currentAcceleration.x, stats.currentAcceleration.y, stats.currentAcceleration.z)));
        // 惯性状态
        Player player = mc.player;
        if (player != null) {
            DimensionPhysics config = player.level().getData(DataAttachments.DIMENSION_PHYSICS.get());
            info.add(Component.literal(String.format("§6维度: §f%s", config.name)));
            info.add(Component.literal(String.format("§7重力: §f%.2f m/s²", config.gravity)));
            info.add(Component.literal(String.format("§7空气阻力: §f%.3f", config.airResistance)));
            info.add(Component.literal(String.format("§7安全高度: §f%.1f m", config.safeFallHeight)));

            boolean airborne = InertiaManager.isInAirborne(player);
            String inertiaStatus = airborne ? "§c滞空 (无法控制)" : "§a可控制";
            info.add(Component.literal("§d惯性状态: " + inertiaStatus));

            // 负重信息
            info.add(Component.literal(PlayerLoadManager.getLoadInfo(player)));
            info.add(Component.literal(String.format("§7速度修正: §f%.0f%% | 跳跃修正: §f%.0f%%",
                    PlayerLoadManager.getSpeedModifier(player) * 100,
                    PlayerLoadManager.getJumpModifier(player) * 100)));
            info.add(Component.literal(""));
        }

        info.add(Component.literal("§a─ 速度向量"));
        info.add(Component.literal("§9─ 加速度向量"));
        info.add(Component.literal("§e─ 历史轨迹"));
        info.add(Component.literal("§c─ 预测抛物线"));

        int y = 10;
        for (Component line : info) {
            guiGraphics.drawString(font, line, 10, y, 0xFFFFFF, true);
            y += 10;
        }

        // 绘制曲线图
        renderMotionGraphs(guiGraphics, tracker, font);
    }

    /**
     * 渲染运动曲线图
     */
    private static void renderMotionGraphs(GuiGraphics guiGraphics, MotionTracker tracker, Font font) {
        List<Float> heightHistory = tracker.getHeightHistory();
        List<Float> speedHistory = tracker.getSpeedHistory();
        List<Float> velocityYHistory = tracker.getVelocityYHistory();

        if (heightHistory.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // 计算图表位置（右侧）
        int graphX = screenWidth - GRAPH_WIDTH - GRAPH_RIGHT_MARGIN - 40; // 留出Y轴刻度空间
        int graphY = 10;
        int graphSpacing = GRAPH_HEIGHT + 25;
        String recordTime = tracker.getRecordSeconds() + "秒";

        // 高度曲线
        renderSingleGraph(guiGraphics, font, heightHistory,
                graphX, graphY,
                "高度 (m)", 0xFFFF55, recordTime);

        // 速度曲线
        renderSingleGraph(guiGraphics, font, speedHistory,
                graphX, graphY + graphSpacing,
                "速度 (m/s)", 0x55FF55, recordTime);

        // Y轴速度曲线
        renderSingleGraph(guiGraphics, font, velocityYHistory,
                graphX, graphY + graphSpacing * 2,
                "Y轴速度 (m/s)", 0x5555FF, recordTime);
    }

    /**
     * 渲染单个曲线图
     */
    private static void renderSingleGraph(GuiGraphics guiGraphics, Font font, List<Float> data,
                                           int x, int y, String title, int color, String timeRange) {
        if (data.isEmpty()) return;

        // 计算数据范围
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;
        for (Float v : data) {
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
        }

        // 避免范围为0
        if (maxVal - minVal < 0.01f) {
            maxVal = minVal + 1.0f;
        }

        // 绘制背景
        guiGraphics.fill(x, y, x + GRAPH_WIDTH, y + GRAPH_HEIGHT, 0x80000000);

        // 绘制边框
        guiGraphics.renderOutline(x, y, GRAPH_WIDTH, GRAPH_HEIGHT, 0xFF444444);

        // 绘制标题
        guiGraphics.drawString(font, title + " [" + timeRange + "]", x, y - 10, color, true);

        // 绘制Y轴刻度
        String minLabel = String.format("%.1f", minVal);
        String maxLabel = String.format("%.1f", maxVal);
        guiGraphics.drawString(font, maxLabel, x + GRAPH_WIDTH + 2, y, 0xAAAAAA, false);
        guiGraphics.drawString(font, minLabel, x + GRAPH_WIDTH + 2, y + GRAPH_HEIGHT - 8, 0xAAAAAA, false);

        // 绘制曲线
        int dataPoints = Math.min(data.size(), GRAPH_WIDTH);

        for (int i = 1; i < dataPoints; i++) {
            float prevVal = data.get(data.size() * (i - 1) / dataPoints);
            float currVal = data.get(data.size() * i / dataPoints);

            // 归一化到图表高度
            float prevNorm = (prevVal - minVal) / (maxVal - minVal);
            float currNorm = (currVal - minVal) / (maxVal - minVal);

            int x1 = x + (GRAPH_WIDTH * (i - 1) / dataPoints);
            int x2 = x + (GRAPH_WIDTH * i / dataPoints);
            int y1 = y + GRAPH_HEIGHT - (int) (prevNorm * GRAPH_HEIGHT);
            int y2 = y + GRAPH_HEIGHT - (int) (currNorm * GRAPH_HEIGHT);

            // 绘制线段
            guiGraphics.hLine(x1, x2, y1, color);
            if (y1 != y2) {
                guiGraphics.vLine(x2, Math.min(y1, y2), Math.max(y1, y2), color);
            }
        }

        // 绘制当前值标记
        float currentVal = data.get(data.size() - 1);
        float currentNorm = (currentVal - minVal) / (maxVal - minVal);
        int currentY = y + GRAPH_HEIGHT - (int) (currentNorm * GRAPH_HEIGHT);
        guiGraphics.fill(x + GRAPH_WIDTH - 2, currentY - 2, x + GRAPH_WIDTH + 2, currentY + 2, 0xFFFFFFFF);
    }

    private static void renderTrajectory(PoseStack poseStack, MultiBufferSource bufferSource,
                                          List<Vector3f> points, Vec3 camera,
                                          float r, float g, float b) {
        if (points.size() < 2) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        for (int i = 1; i < points.size(); i++) {
            Vector3f prev = points.get(i - 1);
            Vector3f curr = points.get(i);
            float alpha = (float) i / points.size();

            float x1 = prev.x - (float) camera.x;
            float y1 = prev.y - (float) camera.y;
            float z1 = prev.z - (float) camera.z;
            float x2 = curr.x - (float) camera.x;
            float y2 = curr.y - (float) camera.y;
            float z2 = curr.z - (float) camera.z;

            consumer.addVertex(last, x1, y1, z1).setColor(r, g, b, alpha).setNormal(last, 1.0f, 0.0f, 0.0f);
            consumer.addVertex(last, x2, y2, z2).setColor(r, g, b, alpha).setNormal(last, 1.0f, 0.0f, 0.0f);
        }
    }

    private static void renderVelocityVector(PoseStack poseStack, MultiBufferSource bufferSource,
                                              Entity entity, MotionTracker tracker, Vec3 camera) {
        MotionTracker.MotionStats stats = tracker.getStats();
        if (stats.currentSpeed < 0.01f) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        float startX = (float) (entity.getX() - camera.x);
        float startY = (float) (entity.getY() + entity.getBbHeight() / 2 - camera.y);
        float startZ = (float) (entity.getZ() - camera.z);

        float scale = 0.5f;
        float endX = startX + stats.currentVelocity.x * scale;
        float endY = startY + stats.currentVelocity.y * scale;
        float endZ = startZ + stats.currentVelocity.z * scale;

        consumer.addVertex(last, startX, startY, startZ).setColor(0.0f, 1.0f, 0.0f, 1.0f).setNormal(last, 1.0f, 0.0f, 0.0f);
        consumer.addVertex(last, endX, endY, endZ).setColor(0.0f, 1.0f, 0.0f, 1.0f).setNormal(last, 1.0f, 0.0f, 0.0f);
    }

    private static void renderAccelerationVector(PoseStack poseStack, MultiBufferSource bufferSource,
                                                  Entity entity, MotionTracker tracker, Vec3 camera) {
        MotionTracker.MotionStats stats = tracker.getStats();
        if (stats.accelerationMagnitude < 0.1f) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        float startX = (float) (entity.getX() - camera.x);
        float startY = (float) (entity.getY() + entity.getBbHeight() / 2 - camera.y);
        float startZ = (float) (entity.getZ() - camera.z);

        float scale = 0.1f;
        float endX = startX + stats.currentAcceleration.x * scale;
        float endY = startY + stats.currentAcceleration.y * scale;
        float endZ = startZ + stats.currentAcceleration.z * scale;

        consumer.addVertex(last, startX, startY, startZ).setColor(0.0f, 0.5f, 1.0f, 1.0f).setNormal(last, 1.0f, 0.0f, 0.0f);
        consumer.addVertex(last, endX, endY, endZ).setColor(0.0f, 0.5f, 1.0f, 1.0f).setNormal(last, 1.0f, 0.0f, 0.0f);
    }

    public static void clearAll() {
        trackers.clear();
    }
}
