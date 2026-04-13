package org.polaris2023.gtu.physics.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.polaris2023.gtu.physics.world.PhysicsPauseManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 Minecraft 客户端
 * <p>
 * 检测游戏暂停状态，暂停物理系统
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftPause {

    @Shadow
    public Screen screen;

    /**
     * 游戏暂停时调用（打开暂停菜单）
     */
    @Inject(method = "pauseGame", at = @At("HEAD"))
    private void gtu_physics$onPauseGame(CallbackInfo ci) {
        PhysicsPauseManager.getInstance().pause("game_paused");
    }

    /**
     * 每帧开始时检查屏幕状态
     * 如果之前暂停但现在没有屏幕了，说明恢复了
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void gtu_physics$onTick(CallbackInfo ci) {
        PhysicsPauseManager pauseManager = PhysicsPauseManager.getInstance();

        // 如果之前暂停，现在没有屏幕了，恢复
        if (pauseManager.isPaused() && this.screen == null) {
            pauseManager.resume();
        }
    }

    /**
     * 设置屏幕时检查
     * 如果设置了 null 屏幕，说明关闭了菜单
     */
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void gtu_physics$onSetScreen(Screen screen, CallbackInfo ci) {
        PhysicsPauseManager pauseManager = PhysicsPauseManager.getInstance();

        // 如果关闭了屏幕（设为 null），恢复物理
        if (screen == null && pauseManager.isPaused()) {
            pauseManager.resume();
        }
    }
}
