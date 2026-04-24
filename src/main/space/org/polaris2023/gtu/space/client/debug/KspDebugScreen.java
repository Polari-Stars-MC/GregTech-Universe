package org.polaris2023.gtu.space.client.debug;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.polaris2023.gtu.space.network.KspClientSyncState;
import org.polaris2023.gtu.space.network.payload.KspDebugSnapshotRequestPacket;

public final class KspDebugScreen extends Screen {
    public KspDebugScreen() {
        super(Component.literal("KSP Runtime Debug"));
    }

    @Override
    protected void init() {
        super.init();
        KspClientSyncState.clear();
        PacketDistributor.sendToServer(KspDebugSnapshotRequestPacket.INSTANCE);
        KspDebugWindow.onScreenOpened();
    }

    @Override
    public void removed() {
        KspDebugWindow.onScreenClosed();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        KspDebugWindow.updateDrag();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F6 || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            KspDebugWindow.cycleSelection((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            KspDebugWindow.adjustBodyScale(1.25);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            KspDebugWindow.adjustBodyScale(1.0 / 1.25);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        KspDebugWindow.onScroll(mouseX, mouseY, scrollY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return KspDebugWindow.onMouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        KspDebugWindow.render(guiGraphics);
    }
}
