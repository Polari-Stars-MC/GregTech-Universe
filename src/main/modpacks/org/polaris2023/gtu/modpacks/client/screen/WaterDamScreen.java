package org.polaris2023.gtu.modpacks.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.menu.WaterDamMenu;

/**
 * 水坝控制器 GUI 屏幕。
 * <p>
 * 采用 GTCEu 风格的信息面板设计。
 * 显示水坝状态、等级、应力输出、流速等关键信息。
 * </p>
 */
@OnlyIn(Dist.CLIENT)
public class WaterDamScreen extends AbstractContainerScreen<WaterDamMenu> {

    // GTCEu风格的颜色定义
    private static final int BG_COLOR = 0xCC2D2D2D;          // 深灰半透明背景
    private static final int BORDER_COLOR = 0xFF5B5B5B;      // 边框颜色
    private static final int HEADER_COLOR = 0xFF3C3C3C;      // 标题栏
    private static final int ACCENT_COLOR = 0xFF4CAF50;      // 强调色（绿色）
    private static final int STRESS_BAR_BG = 0xFF1A1A1A;     // 应力条背景
    private static final int STRESS_BAR_FG = 0xFF00BCD4;     // 应力条前景（青色）
    private static final int WARNING_COLOR = 0xFFFF5722;     // 警告色

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 180;

    public WaterDamScreen(WaterDamMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // GTCEu风格不需要背包区域
        this.inventoryLabelY = Integer.MAX_VALUE; // 隐藏背包标签
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderDamPanel(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 由 renderDamPanel 处理
    }

    private void renderDamPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (this.width - PANEL_WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        // 主面板背景
        guiGraphics.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, BORDER_COLOR);
        guiGraphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, BG_COLOR);

        // 标题栏
        guiGraphics.fill(x, y, x + PANEL_WIDTH, y + 20, HEADER_COLOR);
        guiGraphics.fill(x, y + 20, x + PANEL_WIDTH, y + 21, ACCENT_COLOR);

        // 标题
        guiGraphics.drawCenteredString(this.font, "§6水坝控制器", x + PANEL_WIDTH / 2, y + 6, 0xFFFFFF);

        int textY = y + 28;
        int leftMargin = x + 10;

        DamTier tier = DamTier.byIndex(menu.getTierIndex());
        boolean formed = menu.isFormed();

        // 状态指示灯
        int statusColor = formed ? 0xFF00FF00 : 0xFFFF0000;
        guiGraphics.fill(leftMargin, textY + 1, leftMargin + 8, textY + 9, statusColor);
        guiGraphics.drawString(this.font,
                formed ? "§a多方块已成型" : "§c多方块未成型",
                leftMargin + 12, textY, 0xFFFFFF, false);
        textY += 16;

        // 分隔线
        guiGraphics.fill(leftMargin, textY, x + PANEL_WIDTH - 10, textY + 1, 0xFF555555);
        textY += 6;

        // 等级
        guiGraphics.drawString(this.font, "§7等级:", leftMargin, textY, 0xFFFFFF, false);
        String tierText = tier.getColor().toString() + tier.getDisplayName();
        guiGraphics.drawString(this.font, tierText,
                leftMargin + 100, textY, 0xFFFFFF, false);
        textY += 14;

        // 应力输出
        guiGraphics.drawString(this.font, "§7应力输出:", leftMargin, textY, 0xFFFFFF, false);
        double stress = menu.getStressOutput();
        String stressText = formatStress(stress);
        guiGraphics.drawString(this.font, "§b" + stressText,
                leftMargin + 100, textY, 0xFFFFFF, false);
        textY += 14;

        // 应力条
        int barX = leftMargin;
        int barWidth = PANEL_WIDTH - 20;
        int barHeight = 8;
        guiGraphics.fill(barX, textY, barX + barWidth, textY + barHeight, STRESS_BAR_BG);
        if (stress > 0) {
            double maxStress = DamTier.UXV.getBaseStress() * 3.0;
            int fillWidth = (int) Math.min(barWidth, (stress / maxStress) * barWidth);
            guiGraphics.fill(barX, textY, barX + fillWidth, textY + barHeight, STRESS_BAR_FG);
        }
        textY += 14;

        // 旋转速度
        guiGraphics.drawString(this.font, "§7转速:", leftMargin, textY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font,
                String.format("§a%.1f RPM", menu.getRotationSpeed()),
                leftMargin + 100, textY, 0xFFFFFF, false);
        textY += 14;

        // 河流流速
        guiGraphics.drawString(this.font, "§7河流流速:", leftMargin, textY, 0xFFFFFF, false);
        double flowSpeed = menu.getFlowSpeed();
        ChatFormatting flowColor = flowSpeed > 0.5 ? ChatFormatting.GREEN :
                (flowSpeed > 0.1 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        guiGraphics.drawString(this.font,
                flowColor.toString() + String.format("%.2f", flowSpeed),
                leftMargin + 100, textY, 0xFFFFFF, false);
        textY += 14;

        // 分隔线
        guiGraphics.fill(leftMargin, textY, x + PANEL_WIDTH - 10, textY + 1, 0xFF555555);
        textY += 6;

        // 连接水坝数
        int damCount = menu.getConnectedDamCount() + 1; // +1 包含主水坝
        guiGraphics.drawString(this.font, "§7水坝堆叠:", leftMargin, textY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font,
                "§e" + damCount + " 个",
                leftMargin + 100, textY, 0xFFFFFF, false);
        textY += 14;

        // 基础应力
        guiGraphics.drawString(this.font, "§7基础应力:", leftMargin, textY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font,
                "§8" + formatStress(tier.getBaseStress()),
                leftMargin + 100, textY, 0xFFFFFF, false);
        textY += 14;

        // 应力公式说明
        guiGraphics.drawString(this.font,
                "§8公式: base × (1+n×0.8) × flow",
                leftMargin, textY, 0x888888, false);
    }

    private String formatStress(double stress) {
        if (stress >= 1_000_000_000) {
            return String.format("%.2f GSU", stress / 1_000_000_000);
        } else if (stress >= 1_000_000) {
            return String.format("%.2f MSU", stress / 1_000_000);
        } else if (stress >= 1_000) {
            return String.format("%.1f kSU", stress / 1_000);
        } else {
            return String.format("%.0f SU", stress);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
