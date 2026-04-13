package org.polaris2023.gtu.physics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;
import org.polaris2023.gtu.physics.debug.BulletShapeRenderer;
import org.polaris2023.gtu.physics.debug.PhysicsDebugRenderer;

/**
 * 客户端渲染事件处理器
 */
@EventBusSubscriber(modid = GregtechUniversePhysics.MOD_ID, value = Dist.CLIENT)
public class ClientRenderHandler {

    /**
     * 调试模式切换按键 (F6)
     * <p>
     * 循环切换模式：
     * <ul>
     *   <li>OFF → 轨迹调试</li>
     *   <li>轨迹调试 → Bullet 碰撞形状</li>
     *   <li>Bullet 碰撞形状 → 全部开启</li>
     *   <li>全部开启 → OFF</li>
     * </ul>
     */
    public static final Lazy<KeyMapping> DEBUG_KEY = Lazy.of(() ->
            new KeyMapping(
                    "key.gtu_physics.debug",
                    GLFW.GLFW_KEY_F6,
                    "key.categories.gtu_physics"
            )
    );

    /**
     * 调试模式：0=关闭, 1=轨迹, 2=碰撞形状, 3=全部
     */
    private static int debugMode = 0;

    private static final String[] MODE_NAMES = {
            "§c关闭",
            "§e轨迹调试",
            "§bBullet 碰撞形状",
            "§a全部开启"
    };

    /**
     * 注册按键映射
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DEBUG_KEY.get());
    }

    /**
     * 客户端 tick 处理按键
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 检测按键
        if (DEBUG_KEY.get().consumeClick()) {
            debugMode = (debugMode + 1) % MODE_NAMES.length;
            updateRendererStates();
            mc.player.displayClientMessage(
                    Component.literal("§7[GTU Physics] §f调试模式: " + MODE_NAMES[debugMode]),
                    true
            );
        }
    }

    private static void updateRendererStates() {
        PhysicsDebugRenderer.setEnabled(debugMode == 1 || debugMode == 3);
        BulletShapeRenderer.setEnabled(debugMode == 2 || debugMode == 3);
    }

    /**
     * 渲染世界后绘制3D调试信息
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (debugMode == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        var camera = event.getCamera();

        // 轨迹调试渲染
        if (PhysicsDebugRenderer.isEnabled()) {
            PhysicsDebugRenderer.recordEntity(mc.player, mc.level.getGameTime());
            PhysicsDebugRenderer.render(
                    poseStack,
                    bufferSource,
                    camera.getPosition().x,
                    camera.getPosition().y,
                    camera.getPosition().z
            );
        }

        // Bullet 碰撞形状渲染
        if (BulletShapeRenderer.isEnabled()) {
            BulletShapeRenderer.render(
                    poseStack,
                    bufferSource,
                    camera.getPosition().x,
                    camera.getPosition().y,
                    camera.getPosition().z
            );
        }

        bufferSource.endBatch();
    }

    /**
     * 渲染GUI（2D曲线图）
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!PhysicsDebugRenderer.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var tracker = PhysicsDebugRenderer.getOrCreateTracker(mc.player);
        PhysicsDebugRenderer.renderHud(event.getGuiGraphics(), tracker);

        // Bullet 碰撞形状模式下显示图例
        if (BulletShapeRenderer.isEnabled()) {
            renderBulletLegend(event);
        }
    }

    /**
     * 渲染 Bullet 碰撞形状图例
     */
    private static void renderBulletLegend(RenderGuiEvent.Post event) {
        var graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;

        int x = 10;
        int y = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 60;

        graphics.drawString(font, "§b[Bullet 碰撞形状]", x, y, 0xFFFFFF, true);
        graphics.drawString(font, "§9■ 蓝色 = BoxCollisionShape", x, y + 12, 0xFFFFFF, true);
        graphics.drawString(font, "§a■ 绿色 = CapsuleCollisionShape (玩家)", x, y + 24, 0xFFFFFF, true);
        graphics.drawString(font, "§c→ 红色 = 速度向量", x, y + 36, 0xFFFFFF, true);
        graphics.drawString(font, "§e↻ 黄色 = 角速度", x, y + 48, 0xFFFFFF, true);
    }
}
