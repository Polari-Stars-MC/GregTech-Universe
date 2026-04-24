package org.polaris2023.gtu.modpacks.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamControllerBlockEntity;
import org.polaris2023.gtu.modpacks.dam.DamSegmentState;
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.menu.WaterDamMenu;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DamControlScreen extends AbstractContainerScreen<WaterDamMenu> {
    private static final int PANEL_WIDTH = 228;
    private static final int PANEL_HEIGHT = 196;
    private static final int BORDER = 0xFF5E5E5E;
    private static final int HEADER = 0xFF2E3138;
    private static final int BODY = 0xE01C1E23;
    private static final int TAB_ACTIVE = 0xFF2B5D8A;
    private static final int TAB_IDLE = 0xFF3A3D46;
    private static final int ACCENT = 0xFF00BCD4;
    private static final String[] TABS = {"总览", "坝段", "公式", "结构"};

    private int selectedTab;

    public DamControlScreen(WaterDamMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = Integer.MAX_VALUE;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderPanel(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = leftPos();
        int top = topPos();
        for (int i = 0; i < TABS.length; i++) {
            int tabX = left + 8 + i * 52;
            int tabY = top + 24;
            if (mouseX >= tabX && mouseX <= tabX + 48 && mouseY >= tabY && mouseY <= tabY + 16) {
                selectedTab = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = leftPos();
        int top = topPos();
        guiGraphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, BORDER);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BODY);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + 20, HEADER);
        guiGraphics.drawCenteredString(font, "GTCEu 水坝控制器", left + PANEL_WIDTH / 2, top + 6, 0xFFFFFF);

        for (int i = 0; i < TABS.length; i++) {
            int tabX = left + 8 + i * 52;
            int color = selectedTab == i ? TAB_ACTIVE : TAB_IDLE;
            guiGraphics.fill(tabX, top + 24, tabX + 48, top + 40, color);
            guiGraphics.drawCenteredString(font, TABS[i], tabX + 24, top + 29, 0xFFFFFF);
        }

        WaterDamControllerBlockEntity controller = getController();
        int textX = left + 10;
        int textY = top + 50;
        if (controller == null) {
            guiGraphics.drawString(font, "控制器客户端数据未同步", textX, textY, 0xFF8080, false);
            return;
        }

        switch (selectedTab) {
            case 0 -> renderOverview(guiGraphics, controller, textX, textY);
            case 1 -> renderSegments(guiGraphics, controller, textX, textY);
            case 2 -> renderFormula(guiGraphics, controller, textX, textY);
            case 3 -> renderStructure(guiGraphics, controller, textX, textY);
            default -> renderOverview(guiGraphics, controller, textX, textY);
        }
    }

    private void renderOverview(GuiGraphics guiGraphics, WaterDamControllerBlockEntity controller, int x, int y) {
        DamTier tier = controller.getTier();
        drawLine(guiGraphics, x, y, "状态", controller.isFormed() ? "已成型" : "未成型", controller.isFormed() ? 0x80FF80 : 0xFF8080);
        drawLine(guiGraphics, x, y + 12, "等级", tier.getDisplayName(), tier.getColor().getColor() != null ? tier.getColor().getColor() : 0xFFFFFF);
        drawLine(guiGraphics, x, y + 24, "坝段数", Integer.toString(controller.getSegmentCount()), 0xFFFF80);
        drawLine(guiGraphics, x, y + 36, "总流速", String.format("%.2f", controller.getRiverFlowSpeed()), 0x80C8FF);
        drawLine(guiGraphics, x, y + 48, "总应力", formatStress(controller.getStressOutput()), ACCENT);
        drawLine(guiGraphics, x, y + 60, "轴速", String.format("%.1f RPM", controller.getRotationSpeed()), 0x80FFB0);
        drawLine(guiGraphics, x, y + 72, "活跃叶轮", String.format("%d / %d", controller.getSegments().stream().filter(DamSegmentState::assembled).count(), Math.max(1, controller.getSegmentCount())), 0x80FFFF);

        if (!controller.getStructureIssues().isEmpty()) {
            guiGraphics.drawString(font, "结构告警:", x, y + 92, 0xFF9090, false);
            int lineY = y + 104;
            for (int i = 0; i < Math.min(4, controller.getStructureIssues().size()); i++) {
                guiGraphics.drawString(font, "- " + controller.getStructureIssues().get(i), x, lineY, 0xFFB0B0, false);
                lineY += 10;
            }
        }
    }

    private void renderSegments(GuiGraphics guiGraphics, WaterDamControllerBlockEntity controller, int x, int y) {
        List<DamSegmentState> segments = controller.getSegments();
        if (segments.isEmpty()) {
            guiGraphics.drawString(font, "没有已识别的坝段", x, y, 0xFF8080, false);
            return;
        }
        int lineY = y;
        for (int i = 0; i < Math.min(10, segments.size()); i++) {
            DamSegmentState segment = segments.get(i);
            BlockPos axis = segment.axisPos();
            guiGraphics.drawString(font,
                    String.format("#%d 轴心[%d,%d,%d]", segment.index() + 1, axis.getX(), axis.getY(), axis.getZ()),
                    x, lineY, 0xFFFFFF, false);
            lineY += 10;
            guiGraphics.drawString(font,
                    String.format("  Flow %.2f | Share %s | Hatch %s | Wheel %s",
                            segment.flowSpeed(),
                            formatStress(segment.stressShare()),
                            segment.hatchValid() ? "OK" : "ERR",
                            segment.assembled() ? "RUN" : "IDLE"),
                    x, lineY, segment.hatchValid() ? 0xA0FFA0 : 0xFF9090, false);
            lineY += 14;
        }
    }

    private void renderFormula(GuiGraphics guiGraphics, WaterDamControllerBlockEntity controller, int x, int y) {
        DamTier tier = controller.getTier();
        double base = tier.getBaseStress();
        double flowFactor = tier.getFlowFactor(controller.getRiverFlowSpeed());
        double stackFactor = tier.getStackFactor(controller.getSegmentCount());
        double rpm = tier.calculateRpm(controller.getRiverFlowSpeed());

        guiGraphics.drawString(font, "baseStress = 32 * 4^tier", x, y, 0xFFFFFF, false);
        guiGraphics.drawString(font, String.format("base = %.0f SU", base), x, y + 14, ACCENT, false);
        guiGraphics.drawString(font, String.format("flowFactor = %.3f", flowFactor), x, y + 28, 0x80C8FF, false);
        guiGraphics.drawString(font, String.format("stackFactor = %.3f", stackFactor), x, y + 42, 0xFFFF80, false);
        guiGraphics.drawString(font, String.format("total = %.0f SU", controller.getStressOutput()), x, y + 56, 0x80FFB0, false);
        guiGraphics.drawString(font, String.format("rpm = %.1f", rpm), x, y + 70, 0xFFFFFF, false);
        guiGraphics.drawString(font, String.format("segmentShare = %s", formatStress(tier.calculateSegmentStress(controller.getSegmentCount(), controller.getRiverFlowSpeed()))), x, y + 84, 0xFFFFFF, false);
    }

    private void renderStructure(GuiGraphics guiGraphics, WaterDamControllerBlockEntity controller, int x, int y) {
        guiGraphics.drawString(font, "蓝图尺寸: 7 x 7 x 3", x, y, 0xFFFFFF, false);
        guiGraphics.drawString(font, "轴心局部坐标: [3,3,1]", x, y + 14, 0xFFFFFF, false);
        guiGraphics.drawString(font, "控制器局部坐标: [6,6,1]", x, y + 28, 0xFFFFFF, false);
        guiGraphics.drawString(font, "输出链: 轴心 -> 传动杆 -> 应力仓", x, y + 42, 0xFFFFFF, false);
        guiGraphics.drawString(font, "堆叠方向: 控制器顺时针横向", x, y + 56, 0xFFFFFF, false);
        guiGraphics.drawString(font, "最大堆叠: 16 段", x, y + 70, 0xFFFFFF, false);

        int lineY = y + 92;
        if (controller.getStructureIssues().isEmpty()) {
            guiGraphics.drawString(font, "无结构告警", x, lineY, 0x80FF80, false);
            return;
        }

        guiGraphics.drawString(font, "结构告警列表:", x, lineY, 0xFF9090, false);
        lineY += 12;
        for (int i = 0; i < Math.min(8, controller.getStructureIssues().size()); i++) {
            guiGraphics.drawString(font, "- " + controller.getStructureIssues().get(i), x, lineY, 0xFFB0B0, false);
            lineY += 10;
        }
    }

    private void drawLine(GuiGraphics guiGraphics, int x, int y, String label, String value, int valueColor) {
        guiGraphics.drawString(font, label + ":", x, y, 0xFFFFFF, false);
        guiGraphics.drawString(font, value, x + 92, y, valueColor, false);
    }

    private WaterDamControllerBlockEntity getController() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        BlockPos pos = menu.getControllerPos();
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return null;
        }
        return minecraft.level.getBlockEntity(pos) instanceof WaterDamControllerBlockEntity controller ? controller : null;
    }

    private int leftPos() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int topPos() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private String formatStress(double stress) {
        if (stress >= 1_000_000_000) return String.format("%.2f GSU", stress / 1_000_000_000.0);
        if (stress >= 1_000_000) return String.format("%.2f MSU", stress / 1_000_000.0);
        if (stress >= 1_000) return String.format("%.1f kSU", stress / 1_000.0);
        return String.format("%.0f SU", stress);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
