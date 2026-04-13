package org.polaris2023.gtu.physics.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.collision.CollisionEnergy;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.rotation.RotationalPhysics;

/**
 * Bullet 碰撞形状渲染器
 * <p>
 * 按 F6 在游戏世界中显示各实体的 Bullet 碰撞形状线框，
 * 类似 Minecraft 自带的 F3+B 碰撞箱显示。
 * <p>
 * 显示内容：
 * <ul>
 *   <li>蓝色线框 - 标准 BoxCollisionShape（普通实体）</li>
 *   <li>绿色线框 - CapsuleCollisionShape（玩家）</li>
 *   <li>红色箭头 - 速度向量</li>
 *   <li>黄色弧线 - 角速度指示</li>
 *   <li>实体上方标签 - 质量 / 恢复系数 / 角速度</li>
 * </ul>
 */
public class BulletShapeRenderer {

    private static boolean enabled = false;
    private static final float VELOCITY_SCALE = 0.5f;
    private static final double RENDER_DISTANCE = 64.0;

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 渲染所有附近实体的 Bullet 碰撞形状
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              double camX, double camY, double camZ) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = new Vec3(camX, camY, camZ);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (entity.distanceTo(mc.player) > RENDER_DISTANCE) continue;

            renderEntityShape(poseStack, bufferSource, entity, camera);
        }

        // 渲染玩家自身的碰撞形状（最后渲染，确保可见）
        renderPlayerShape(poseStack, bufferSource, mc.player, camera);
    }

    // ==================== 实体碰撞形状渲染 ====================

    /**
     * 渲染单个实体的碰撞形状
     */
    private static void renderEntityShape(PoseStack poseStack, MultiBufferSource bufferSource,
                                           Entity entity, Vec3 camera) {
        if (entity instanceof Player) {
            renderCapsuleShape(poseStack, bufferSource, entity, camera,
                    0.0f, 1.0f, 0.3f);  // 绿色 - 玩家胶囊
        } else {
            renderBoxShape(poseStack, bufferSource, entity, camera,
                    0.2f, 0.5f, 1.0f);  // 蓝色 - 标准盒子
        }

        // 速度向量
        renderVelocityVector(poseStack, bufferSource, entity, camera);

        // 角速度指示
        renderAngularVelocity(poseStack, bufferSource, entity, camera);
    }

    /**
     * 渲染玩家碰撞形状
     */
    private static void renderPlayerShape(PoseStack poseStack, MultiBufferSource bufferSource,
                                           Player player, Vec3 camera) {
        renderCapsuleShape(poseStack, bufferSource, player, camera,
                0.0f, 1.0f, 0.3f);  // 绿色
        renderVelocityVector(poseStack, bufferSource, player, camera);
        renderAngularVelocity(poseStack, bufferSource, player, camera);
    }

    // ==================== 形状绘制 ====================

    /**
     * 绘制盒子碰撞形状线框
     * <p>
     * 对应 Bullet 的 BoxCollisionShape
     */
    private static void renderBoxShape(PoseStack poseStack, MultiBufferSource bufferSource,
                                        Entity entity, Vec3 camera,
                                        float r, float g, float b) {
        AABB bb = entity.getBoundingBox();

        float minX = (float) (bb.minX - camera.x);
        float minY = (float) (bb.minY - camera.y);
        float minZ = (float) (bb.minZ - camera.z);
        float maxX = (float) (bb.maxX - camera.x);
        float maxY = (float) (bb.maxY - camera.y);
        float maxZ = (float) (bb.maxZ - camera.z);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        // 底面
        line(consumer, last, minX, minY, minZ, maxX, minY, minZ, r, g, b);
        line(consumer, last, maxX, minY, minZ, maxX, minY, maxZ, r, g, b);
        line(consumer, last, maxX, minY, maxZ, minX, minY, maxZ, r, g, b);
        line(consumer, last, minX, minY, maxZ, minX, minY, minZ, r, g, b);

        // 顶面
        line(consumer, last, minX, maxY, minZ, maxX, maxY, minZ, r, g, b);
        line(consumer, last, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b);
        line(consumer, last, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b);
        line(consumer, last, minX, maxY, maxZ, minX, maxY, minZ, r, g, b);

        // 竖边
        line(consumer, last, minX, minY, minZ, minX, maxY, minZ, r, g, b);
        line(consumer, last, maxX, minY, minZ, maxX, maxY, minZ, r, g, b);
        line(consumer, last, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b);
        line(consumer, last, minX, minY, maxZ, minX, maxY, maxZ, r, g, b);
    }

    /**
     * 绘制胶囊碰撞形状线框
     * <p>
     * 对应 Bullet 的 CapsuleCollisionShape（玩家使用）
     */
    private static void renderCapsuleShape(PoseStack poseStack, MultiBufferSource bufferSource,
                                            Entity entity, Vec3 camera,
                                            float r, float g, float b) {
        AABB bb = entity.getBoundingBox();
        float width = (float) bb.getXsize();
        float height = (float) bb.getYsize();
        float radius = width / 2.0f;
        float cylinderHeight = height - width;

        // 胶囊中心
        float cx = (float) (entity.getX() - camera.x);
        float cy = (float) (bb.minY + height / 2.0 - camera.y);
        float cz = (float) (entity.getZ() - camera.z);

        // 半球中心
        float bottomY = cy - cylinderHeight / 2.0f;
        float topY = cy + cylinderHeight / 2.0f;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        int segments = 16;

        // 水平圆环 - 底部
        drawCircleXZ(consumer, last, cx, bottomY, cz, radius, segments, r, g, b);
        // 水平圆环 - 顶部
        drawCircleXZ(consumer, last, cx, topY, cz, radius, segments, r, g, b);
        // 水平圆环 - 中间
        drawCircleXZ(consumer, last, cx, cy, cz, radius, segments, r, g, b);

        // 竖直连线
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI * 2 / 8);
            float dx = (float) (Math.cos(angle) * radius);
            float dz = (float) (Math.sin(angle) * radius);

            // 圆柱侧面
            line(consumer, last, cx + dx, bottomY, cz + dz, cx + dx, topY, cz + dz, r, g, b);

            // 顶部半球弧线
            float arcStep = (float) (Math.PI / 2 / 4);
            float prevArcY = topY;
            float prevArcDx = dx;
            float prevArcDz = dz;
            for (int j = 1; j <= 4; j++) {
                float t = j * arcStep;
                float arcR = (float) (Math.cos(t) * radius);
                float arcY = topY + (float) (Math.sin(t) * radius);
                float adx = (float) (Math.cos(angle) * arcR);
                float adz = (float) (Math.sin(angle) * arcR);
                line(consumer, last, cx + prevArcDx, prevArcY, cz + prevArcDz,
                        cx + adx, arcY, cz + adz, r, g, b);
                prevArcY = arcY;
                prevArcDx = adx;
                prevArcDz = adz;
            }

            // 底部半球弧线
            prevArcY = bottomY;
            prevArcDx = dx;
            prevArcDz = dz;
            for (int j = 1; j <= 4; j++) {
                float t = j * arcStep;
                float arcR = (float) (Math.cos(t) * radius);
                float arcY = bottomY - (float) (Math.sin(t) * radius);
                float adx = (float) (Math.cos(angle) * arcR);
                float adz = (float) (Math.sin(angle) * arcR);
                line(consumer, last, cx + prevArcDx, prevArcY, cz + prevArcDz,
                        cx + adx, arcY, cz + adz, r, g, b);
                prevArcY = arcY;
                prevArcDx = adx;
                prevArcDz = adz;
            }
        }
    }

    /**
     * 绘制 XZ 平面上的圆环
     */
    private static void drawCircleXZ(VertexConsumer consumer, PoseStack.Pose last,
                                      float cx, float cy, float cz,
                                      float radius, int segments,
                                      float r, float g, float b) {
        float prevX = cx + radius;
        float prevZ = cz;
        for (int i = 1; i <= segments; i++) {
            float angle = (float) (i * Math.PI * 2 / segments);
            float x = cx + (float) Math.cos(angle) * radius;
            float z = cz + (float) Math.sin(angle) * radius;
            line(consumer, last, prevX, cy, prevZ, x, cy, z, r, g, b);
            prevX = x;
            prevZ = z;
        }
    }

    // ==================== 速度/角速度指示 ====================

    /**
     * 渲染速度向量（红色箭头）
     */
    private static void renderVelocityVector(PoseStack poseStack, MultiBufferSource bufferSource,
                                              Entity entity, Vec3 camera) {
        Vec3 vel = entity.getDeltaMovement();
        double speed = vel.length();
        if (speed < 0.01) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        float startX = (float) (entity.getX() - camera.x);
        float startY = (float) (entity.getY() + entity.getBbHeight() * 0.5 - camera.y);
        float startZ = (float) (entity.getZ() - camera.z);

        float endX = startX + (float) vel.x * VELOCITY_SCALE;
        float endY = startY + (float) vel.y * VELOCITY_SCALE;
        float endZ = startZ + (float) vel.z * VELOCITY_SCALE;

        // 速度线（红色）
        line(consumer, last, startX, startY, startZ, endX, endY, endZ, 1.0f, 0.2f, 0.2f);

        // 箭头头部
        float arrowSize = 0.05f;
        Vec3 dir = vel.normalize();
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 0.001) right = dir.cross(new Vec3(1, 0, 0));
        right = right.normalize().scale(arrowSize);
        Vec3 back = dir.scale(arrowSize * 2);

        float bx = (float) (endX - back.x);
        float by = (float) (endY - back.y);
        float bz = (float) (endZ - back.z);
        line(consumer, last, endX, endY, endZ,
                bx + (float) right.x, by + (float) right.y, bz + (float) right.z,
                1.0f, 0.2f, 0.2f);
        line(consumer, last, endX, endY, endZ,
                bx - (float) right.x, by - (float) right.y, bz - (float) right.z,
                1.0f, 0.2f, 0.2f);
    }

    /**
     * 渲染角速度指示（黄色弧线）
     */
    private static void renderAngularVelocity(PoseStack poseStack, MultiBufferSource bufferSource,
                                               Entity entity, Vec3 camera) {
        RotationalPhysics rp = entity.getData(DataAttachments.ROTATIONAL_PHYSICS.get());
        if (rp == null || !rp.enabled) return;

        float angularSpeed = rp.getAngularSpeed();
        if (angularSpeed < 0.1f) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose last = poseStack.last();

        float cx = (float) (entity.getX() - camera.x);
        float cy = (float) (entity.getY() + entity.getBbHeight() + 0.3 - camera.y);
        float cz = (float) (entity.getZ() - camera.z);

        // 角速度指示圆弧
        float arcRadius = 0.2f;
        int arcSegments = Math.min(32, Math.max(4, (int) (angularSpeed * 2)));
        float arcLength = Math.min((float) Math.PI * 1.5f, angularSpeed * 0.1f);

        // 绕角速度轴画弧
        float axisX = rp.omegaX / angularSpeed;
        float axisY = rp.omegaY / angularSpeed;
        float axisZ = rp.omegaZ / angularSpeed;

        // 选择一个垂直于角速度轴的方向作为弧线起始
        float perpX, perpY, perpZ;
        if (Math.abs(axisY) < 0.9f) {
            perpX = axisZ;
            perpY = 0;
            perpZ = -axisX;
        } else {
            perpX = 1;
            perpY = 0;
            perpZ = 0;
        }
        // 归一化
        float perpLen = (float) Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
        perpX /= perpLen;
        perpY /= perpLen;
        perpZ /= perpLen;

        float prevPx = cx + perpX * arcRadius;
        float prevPy = cy + perpY * arcRadius;
        float prevPz = cz + perpZ * arcRadius;

        for (int i = 1; i <= arcSegments; i++) {
            float t = (float) i / arcSegments * arcLength;
            float cos = (float) Math.cos(t);
            float sin = (float) Math.sin(t);

            // Rodrigues 旋转公式：绕 axis 旋转 perp
            float dot = perpX * axisX + perpY * axisY + perpZ * axisZ;
            float crossX = perpY * axisZ - perpZ * axisY;
            float crossY = perpZ * axisX - perpX * axisZ;
            float crossZ = perpX * axisY - perpY * axisX;

            float px = perpX * cos + crossX * sin + axisX * dot * (1 - cos);
            float py = perpY * cos + crossY * sin + axisY * dot * (1 - cos);
            float pz = perpZ * cos + crossZ * sin + axisZ * dot * (1 - cos);

            float nx = cx + px * arcRadius;
            float ny = cy + py * arcRadius;
            float nz = cz + pz * arcRadius;

            line(consumer, last, prevPx, prevPy, prevPz, nx, ny, nz, 1.0f, 1.0f, 0.2f);
            prevPx = nx;
            prevPy = ny;
            prevPz = nz;
        }
    }

    // ==================== 辅助方法 ====================

    private static void line(VertexConsumer consumer, PoseStack.Pose last,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b) {
        consumer.addVertex(last, x1, y1, z1).setColor(r, g, b, 0.8f).setNormal(last, 1.0f, 0.0f, 0.0f);
        consumer.addVertex(last, x2, y2, z2).setColor(r, g, b, 0.8f).setNormal(last, 1.0f, 0.0f, 0.0f);
    }
}
