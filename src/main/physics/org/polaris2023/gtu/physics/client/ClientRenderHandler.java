package org.polaris2023.gtu.physics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.polaris2023.gtu.physics.debug.PhysicsDebugRenderer;

/**
 * 客户端渲染事件处理器
 */
@EventBusSubscriber(modid = GregtechUniversePhysics.MOD_ID, value = Dist.CLIENT)
public class ClientRenderHandler {

    /**
     * 调试模式切换按键 (F6)
     */
    public static final Lazy<KeyMapping> DEBUG_KEY = Lazy.of(() ->
            new KeyMapping(
                    "key.gtu_physics.debug",
                    GLFW.GLFW_KEY_F6,
                    "key.categories.gtu_physics"
            )
    );

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
            PhysicsDebugRenderer.toggle();
            boolean enabled = PhysicsDebugRenderer.isEnabled();
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            enabled ? "§a物理调试渲染已启用" : "§c物理调试渲染已禁用"
                    ), true
            );
        }
    }

    /**
     * 渲染世界后绘制3D调试信息
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (!PhysicsDebugRenderer.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        // 从 Minecraft 获取 MultiBufferSource
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        var camera = event.getCamera();

        // 记录当前玩家数据
        PhysicsDebugRenderer.recordEntity(mc.player, mc.level.getGameTime());

        // 渲染3D调试信息
        PhysicsDebugRenderer.render(
                poseStack,
                bufferSource,
                camera.getPosition().x,
                camera.getPosition().y,
                camera.getPosition().z
        );

        // 确保渲染完成
        bufferSource.endBatch();
    }

    /**
     * 渲染GUI（2D曲线图）
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!PhysicsDebugRenderer.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 获取追踪器并渲染HUD
        var tracker = PhysicsDebugRenderer.getOrCreateTracker(mc.player);
        PhysicsDebugRenderer.renderHud(event.getGuiGraphics(), tracker);
    }
}
