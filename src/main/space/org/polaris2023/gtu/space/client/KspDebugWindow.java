package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.polaris2023.gtu.space.runtime.SpaceManager;
import org.polaris2023.gtu.space.runtime.ksp.KspBackgroundSystem;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyDefinition;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyState;
import org.polaris2023.gtu.space.runtime.ksp.KspReferenceFrameKind;
import org.polaris2023.gtu.space.runtime.ksp.KspSnapshot;
import org.polaris2023.gtu.space.runtime.ksp.KspSystemDefinition;
import org.polaris2023.gtu.space.runtime.ksp.KspVesselState;
import org.polaris2023.gtu.space.runtime.math.SpaceVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class KspDebugWindow {
    private static final float TAB_TEXT_SCALE = 0.72F;
    private static final float HEADER_TITLE_SCALE = 0.82F;
    private static final float HEADER_HINT_SCALE = 0.72F;
    private enum SidePage {
        SIMULATION("Simulation"),
        VESSEL("Vessel"),
        SELECTED_BODY("Selected Body"),
        LEGEND("Legend");

        private final String label;

        SidePage(String label) {
            this.label = label;
        }
    }

    private static final float SIDE_TEXT_SCALE = 0.85F;
    private static final int SIDE_LINE = 11;
    private static final int SIDE_SECTION = 22;
    private static final int SIDE_TITLE = 13;
    private static final int TAB_HEIGHT = 40;
    private static final int HEADER_HEIGHT = 32;
    private static final int COLOR_PANEL = 0xE30A0E16;
    private static final int COLOR_PANEL_TOP = 0xF2182130;
    private static final int COLOR_MODEL = 0xD1080A10;
    private static final int COLOR_BORDER = 0xFF404A60;
    private static final int COLOR_TAB_ACTIVE = 0xFF1A2232;
    private static final int COLOR_TAB_IDLE = 0xFF101722;
    private static final int COLOR_TEXT = 0xFFEAF0FA;
    private static final int COLOR_MUTED = 0xFF94A2BA;
    private static final int COLOR_TRACK = 0x935C687E;
    private static final int COLOR_REFERENCE_TRACK = 0x2E9FB3C8;
    private static final int COLOR_AXIS_X = 0x78D65C5C;
    private static final int COLOR_AXIS_Y = 0x785AD67D;
    private static final int COLOR_AXIS_Z = 0x785D8DDF;
    private static final int COLOR_VESSEL = 0xFFFFA84B;
    private static final int COLOR_VESSEL_ORBIT = 0xFFF7A53D;
    private static final int COLOR_PERIAPSIS = 0xFFFF5E5E;
    private static final int COLOR_APOAPSIS = 0xFF71A6FF;
    private static final int COLOR_ROTATION_AXIS = 0xAAE8D44D;
    private static final int COLOR_ROOT = 0xFFFFF0A6;
    private static final int COLOR_EARTH = 0xFF62AEFF;
    private static final int COLOR_MOON = 0xFFD3D8E3;
    private static final int COLOR_SELECTED_TRACK = 0xFFFFD166;
    private static final double DEFAULT_CAMERA_YAW = Math.toRadians(-42.0);
    private static final double DEFAULT_CAMERA_PITCH = Math.toRadians(26.0);
    private static final double DISTANCE_UNIT = 1.0E9;
    private static final KspBackgroundSystem FALLBACK = new KspBackgroundSystem(KspSystemDefinition.solarSystem());
    private static final List<float[]> pendingLines = new ArrayList<>(8192);

    private static boolean panning;
    private static boolean rotating;
    private static double zoom = 1.0;
    private static double bodyScale = 1.0;
    private static double panX;
    private static double panY;
    private static double cameraYaw = DEFAULT_CAMERA_YAW;
    private static double cameraPitch = DEFAULT_CAMERA_PITCH;
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;
    private static String selectedBodyId;
    private static int currentModelMinX;
    private static int currentModelMinY;
    private static int currentModelMaxX;
    private static int currentModelMaxY;
    private static int sidePanelX;
    private static int sidePanelY;
    private static int sidePanelWidth;
    private static int sidePanelHeight;
    private static SidePage activePage = SidePage.SIMULATION;
    private static int legendScroll;

    private KspDebugWindow() {
    }

    public static void onScreenOpened() {
        Minecraft minecraft = Minecraft.getInstance();
        selectedBodyId = selectedBodyId == null ? "earth" : selectedBodyId;
        minecraft.mouseHandler.releaseMouse();
        lastMouseX = Double.NaN;
        lastMouseY = Double.NaN;
    }

    public static void onScreenClosed() {
        Minecraft minecraft = Minecraft.getInstance();
        panning = false;
        rotating = false;
        if (minecraft.screen == null) {
            minecraft.mouseHandler.grabMouse();
        }
    }

    public static void cycleSelection(boolean reverse) {
        KspSnapshot snapshot = resolveSnapshot();
        if (snapshot == null || snapshot.bodies().isEmpty()) {
            return;
        }

        List<String> bodyIds = new ArrayList<>(snapshot.bodies().keySet());
        Collections.sort(bodyIds);
        int currentIndex = selectedBodyId == null ? -1 : bodyIds.indexOf(selectedBodyId);
        if (currentIndex < 0) {
            selectedBodyId = bodyIds.getFirst();
            return;
        }

        int nextIndex = reverse
                ? (currentIndex - 1 + bodyIds.size()) % bodyIds.size()
                : (currentIndex + 1) % bodyIds.size();
        selectedBodyId = bodyIds.get(nextIndex);
    }

    public static void onScroll(double mouseX, double mouseY, double deltaY) {
        if (activePage == SidePage.LEGEND && containsLegendViewport(mouseX, mouseY)) {
            legendScroll = Math.max(0, legendScroll - (int) Math.round(deltaY * 18.0));
            return;
        }
        double factor = deltaY > 0.0 ? 1.12 : 1.0 / 1.12;
        zoom = clamp(zoom * factor, 0.2, 128.0);
    }

    public static void adjustBodyScale(double factor) {
        bodyScale = clamp(bodyScale * factor, 0.1, 10.0);
    }

    public static void updateDrag() {
        if (!(Minecraft.getInstance().screen instanceof KspDebugScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean middleDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        if (leftDown) {
            if (panning && !Double.isNaN(lastMouseX)) {
                panX += mouseX - lastMouseX;
                panY -= mouseY - lastMouseY;
            }
            panning = true;
        } else {
            panning = false;
        }

        if (middleDown) {
            if (rotating && !Double.isNaN(lastMouseX)) {
                cameraYaw += (mouseX - lastMouseX) * 0.0125;
                cameraPitch = clamp(cameraPitch - (mouseY - lastMouseY) * 0.0095, Math.toRadians(-88.0), Math.toRadians(88.0));
            }
            rotating = true;
        } else {
            rotating = false;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public static void render(GuiGraphics graphics) {
        KspSnapshot snapshot = resolveSnapshot();
        if (snapshot == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int panelX = 12;
        int panelY = 10;
        int panelWidth = width - 24;
        int panelHeight = height - 20;

        drawPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        drawScaledText(graphics, font, "KSP Runtime Debug [F6]", panelX + 16, panelY + 10, COLOR_TEXT, HEADER_TITLE_SCALE);
        drawScaledText(graphics, font, "Left drag pan, middle drag rotate, wheel zoom, Tab cycle, +/- body scale", panelX + 16, panelY + 21, COLOR_MUTED, HEADER_HINT_SCALE);

        int contentX = panelX + 14;
        int contentY = panelY + HEADER_HEIGHT + 8;
        int contentWidth = panelWidth - 28;
        int contentHeight = panelHeight - HEADER_HEIGHT - 18;
        int gap = 14;
        int sideWidth = clampInt(contentWidth / 3, 240, 330);
        int modelWidth = Math.max(260, contentWidth - sideWidth - gap);
        sideWidth = Math.max(210, contentWidth - modelWidth - gap);

        int modelX = contentX;
        int modelY = contentY;
        int modelHeight = contentHeight;
        int sideX = modelX + modelWidth + gap;
        int sideY = contentY;
        int sideHeight = contentHeight;

        sideWidth = Math.min(sideWidth, panelX + panelWidth - 14 - sideX);
        modelWidth = sideX - gap - modelX;
        sidePanelX = sideX;
        sidePanelY = sideY;
        sidePanelWidth = sideWidth;
        sidePanelHeight = sideHeight;

        drawBoxPanel(graphics, modelX, modelY, modelWidth, modelHeight);
        renderModel(graphics, snapshot, modelX, modelY, modelWidth, modelHeight);

        drawBoxPanel(graphics, sideX, sideY, sideWidth, sideHeight);
        renderTabs(graphics, font, sideX, sideY, sideWidth);
        int textX = sideX + 14;
        int textTop = sideY + TAB_HEIGHT + 10;
        int textWidth = Math.max(80, Math.round((sideWidth - 32) / SIDE_TEXT_SCALE));
        if (activePage == SidePage.SIMULATION) {
            renderSimulationPage(graphics, font, snapshot, textX, textTop, textWidth);
        } else if (activePage == SidePage.VESSEL) {
            renderVesselPage(graphics, font, snapshot, textX, textTop, textWidth);
        } else if (activePage == SidePage.SELECTED_BODY) {
            renderSelectedBodyPage(graphics, font, snapshot, textX, textTop, textWidth);
        } else {
            renderLegend(graphics, font, textX, textTop, textWidth);
        }
    }

    public static boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (sidePanelWidth <= 0) {
            return false;
        }

        int tabWidth = sidePanelWidth / 2;
        int tabHeight = TAB_HEIGHT / 2;
        for (int i = 0; i < SidePage.values().length; i++) {
            int column = i % 2;
            int row = i / 2;
            int tabX = sidePanelX + column * tabWidth;
            int tabY = sidePanelY + row * tabHeight;
            int tabMaxX = column == 1 ? sidePanelX + sidePanelWidth : tabX + tabWidth;
            int tabMaxY = row == 1 ? sidePanelY + TAB_HEIGHT : tabY + tabHeight;
            if (mouseX >= tabX && mouseX < tabMaxX && mouseY >= tabY && mouseY < tabMaxY) {
                activePage = SidePage.values()[i];
                return true;
            }
        }
        return false;
    }

    private static KspSnapshot resolveSnapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            return SpaceManager.get(minecraft.getSingleplayerServer()).latestKspSnapshot();
        }

        FALLBACK.tick();
        return FALLBACK.latestSnapshot();
    }

    private static boolean containsLegendViewport(double mouseX, double mouseY) {
        int contentMinX = sidePanelX;
        int contentMaxX = sidePanelX + sidePanelWidth;
        int contentMinY = sidePanelY + TAB_HEIGHT;
        int contentMaxY = sidePanelY + sidePanelHeight;
        return mouseX >= contentMinX && mouseX < contentMaxX && mouseY >= contentMinY && mouseY < contentMaxY;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_PANEL);
        graphics.fill(x, y, x + width, y + 40, COLOR_PANEL_TOP);
        graphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
    }

    private static void drawBoxPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_MODEL);
        graphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
    }

    private static void renderTabs(GuiGraphics graphics, Font font, int x, int y, int width) {
        int tabWidth = width / 2;
        int tabHeight = TAB_HEIGHT / 2;
        for (int i = 0; i < SidePage.values().length; i++) {
            SidePage page = SidePage.values()[i];
            int column = i % 2;
            int row = i / 2;
            int tabX = x + column * tabWidth;
            int tabY = y + row * tabHeight;
            int tabMaxX = column == 1 ? x + width : tabX + tabWidth;
            int tabMaxY = row == 1 ? y + TAB_HEIGHT : tabY + tabHeight;
            graphics.fill(tabX, tabY, tabMaxX, tabMaxY, page == activePage ? COLOR_TAB_ACTIVE : COLOR_TAB_IDLE);
            graphics.fill(tabX, tabMaxY - 1, tabMaxX, tabMaxY, COLOR_BORDER);
            graphics.fill(tabMaxX - 1, tabY, tabMaxX, tabMaxY, COLOR_BORDER);
            int labelWidth = Math.round(font.width(page.label) * TAB_TEXT_SCALE);
            int labelX = tabX + (tabMaxX - tabX - labelWidth) / 2;
            int labelY = tabY + (tabHeight - Math.round(font.lineHeight * TAB_TEXT_SCALE)) / 2;
            drawScaledText(graphics, font, page.label, labelX, labelY, page == activePage ? COLOR_TEXT : COLOR_MUTED, TAB_TEXT_SCALE);
        }
        graphics.fill(x, y + tabHeight - 1, x + width, y + tabHeight, COLOR_BORDER);
    }

    private static void renderModel(GuiGraphics graphics, KspSnapshot snapshot, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_MODEL);
        graphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
        currentModelMinX = x + 1;
        currentModelMinY = y + 1;
        currentModelMaxX = x + width - 2;
        currentModelMaxY = y + height - 2;
        pendingLines.clear();

        int centerX = x + width / 2;
        int centerY = y + height / 2;
        double modelRadius = Math.min(width, height) * 0.4;
        double maxDistance = computeMaxDistance(snapshot);
        double maxCompressed = compressDistance(maxDistance);
        double scale = (maxCompressed <= 0.0 ? 1.0 : modelRadius / maxCompressed) * zoom;

        drawAxisLine(graphics, centerX, centerY, new SpaceVector(1.0, 0.0, 0.0), scale, COLOR_AXIS_X);
        drawAxisLine(graphics, centerX, centerY, new SpaceVector(0.0, 1.0, 0.0), scale, COLOR_AXIS_Y);
        drawAxisLine(graphics, centerX, centerY, new SpaceVector(0.0, 0.0, 1.0), scale, COLOR_AXIS_Z);

        Map<String, KspBodyState> bodies = snapshot.bodies();
        String highlightedBodyId = resolveSelectedBodyId(snapshot);

        bodies.values().stream()
                .filter(body -> body.definition().referenceFrameKind() != KspReferenceFrameKind.SYSTEM_CENTER)
                .filter(body -> !body.definition().id().startsWith("asteroid_belt_"))
                .forEach(body -> drawReferenceOrbitPath(graphics, snapshot, body, centerX, centerY, scale,
                        body.definition().id().equals(highlightedBodyId)));

        bodies.values().stream()
                .filter(body -> body.definition().referenceFrameKind() != KspReferenceFrameKind.SYSTEM_CENTER)
                .filter(body -> !body.definition().id().startsWith("asteroid_belt_"))
                .forEach(body -> drawOrbitPath(graphics, snapshot, body,
                        snapshot.bodyHistory().get(body.definition().id()), centerX, centerY, scale,
                        body.definition().id().equals(highlightedBodyId),
                        highlightedBodyId == null || body.definition().id().equals(highlightedBodyId)
                                ? bodyColor(body.definition().id()) : COLOR_TRACK));

        snapshot.vessels().values().stream().findFirst()
                .flatMap(vessel -> computeOsculatingOrbit(snapshot, vessel))
                .ifPresent(orbit -> drawVesselOrbit(graphics, orbit, centerX, centerY, scale));

        for (KspBodyState body : bodies.values()) {
            if (body.definition().id().startsWith("asteroid_belt_")) {
                continue;
            }
            SpaceVector axis = body.definition().rotationAxis();
            if (axis.lengthSquared() < 1.0E-9) {
                continue;
            }
            float[] c = compressedPosition(body.absolutePosition());
            float half = cubeHalfSize(body.definition());
            float axisLen = half * 3.0f;
            SpaceVector dir = axis.normalize();
            float[] top = {c[0] + (float)(dir.x() * axisLen), c[1] + (float)(dir.y() * axisLen), c[2] + (float)(dir.z() * axisLen)};
            float[] bot = {c[0] - (float)(dir.x() * axisLen), c[1] - (float)(dir.y() * axisLen), c[2] - (float)(dir.z() * axisLen)};
            ProjectedPoint pCenter = projectCompressed(c[0], c[1], c[2], centerX, centerY, scale);
            ProjectedPoint pTop = projectCompressed(top[0], top[1], top[2], centerX, centerY, scale);
            ProjectedPoint pBot = projectCompressed(bot[0], bot[1], bot[2], centerX, centerY, scale);
            drawLine(graphics, pCenter.x, pCenter.y, pTop.x, pTop.y, COLOR_ROTATION_AXIS);
            drawLine(graphics, pCenter.x, pCenter.y, pBot.x, pBot.y, COLOR_ROTATION_AXIS);
        }

        flushLines(graphics);

        List<CubeRenderable> cubes = new ArrayList<>();
        for (KspBodyState body : bodies.values()) {
            float[] c = compressedPosition(body.absolutePosition());
            float half = cubeHalfSize(body.definition());
            cubes.add(new CubeRenderable(c[0], c[1], c[2], half, bodyColor(body.definition().id())));
        }
        for (KspVesselState vessel : snapshot.vessels().values()) {
            float[] c = compressedPosition(vessel.absolutePosition());
            cubes.add(new CubeRenderable(c[0], c[1], c[2], 0.025f, COLOR_VESSEL));
        }
        renderCubes(graphics, cubes, centerX, centerY, scale);
    }

    private static int renderSimulationPage(GuiGraphics graphics, Font font, KspSnapshot snapshot, int x, int y, int textWidth) {
        String highlightedBodyId = resolveSelectedBodyId(snapshot);
        KspBodyState highlightedBody = highlightedBodyId == null ? null : snapshot.bodies().get(highlightedBodyId);
        int lineY = y;
        drawScaledText(graphics, font, "tick  " + snapshot.simulationTick(), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, String.format("time  %.2fs", snapshot.simulationSeconds()), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, fitLine(font, "univ  " + snapshot.universe().universeId(), textWidth), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, fitLine(font, "gal   " + snapshot.universe().galaxy().displayName(), textWidth), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, fitLine(font, "scale " + snapshot.universe().scaleFactor().toPlainString(), textWidth), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "bodies " + snapshot.bodies().size(), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "vessels " + snapshot.vessels().size(), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, String.format("zoom  %.2fx", zoom), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, String.format("body  %.2fx  [+/-]", bodyScale), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, String.format("pan   %.0f / %.0f", panX, panY), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, String.format("rot   %.0f / %.0f", Math.toDegrees(cameraYaw), Math.toDegrees(cameraPitch)), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, fitLine(font, "track  " + (highlightedBody == null ? "all" : highlightedBody.definition().displayName()), textWidth), x, lineY, COLOR_MUTED);
        return lineY;
    }

    private static int renderVesselPage(GuiGraphics graphics, Font font, KspSnapshot snapshot, int x, int y, int textWidth) {
        KspVesselState vessel = snapshot.vessels().values().stream().findFirst().orElse(null);
        int lineY = y;

        if (vessel == null) {
            lineY += SIDE_TITLE;
            drawScaledText(graphics, font, "No active vessel", x, lineY, COLOR_MUTED);
        } else {
            lineY += SIDE_TITLE;
            drawScaledText(graphics, font, fitLine(font, vessel.name(), textWidth), x, lineY, COLOR_MUTED);
            lineY += SIDE_LINE;
            drawScaledText(graphics, font, fitLine(font, vessel.systemId(), textWidth), x, lineY, COLOR_MUTED);
            lineY += SIDE_LINE;
            drawScaledText(graphics, font, fitLine(font, "primary  " + vessel.primaryBodyId(), textWidth), x, lineY, COLOR_MUTED);
            lineY += SIDE_LINE;
            drawScaledText(graphics, font, String.format("speed  %.0f m/s", vessel.absoluteVelocity().length()), x, lineY, COLOR_MUTED);

            KspBodyState primary = snapshot.bodies().get(vessel.primaryBodyId());
            if (primary != null) {
                double altitude = vessel.absolutePosition().subtract(primary.absolutePosition()).length() - primary.definition().radius();
                lineY += SIDE_LINE;
                drawScaledText(graphics, font, String.format("alt    %.0f km", altitude / 1000.0), x, lineY, COLOR_MUTED);
            }
        }
        return lineY;
    }

    private static int renderSelectedBodyPage(GuiGraphics graphics, Font font, KspSnapshot snapshot, int x, int y, int textWidth) {
        String highlightedBodyId = resolveSelectedBodyId(snapshot);
        KspBodyState highlightedBody = highlightedBodyId == null ? null : snapshot.bodies().get(highlightedBodyId);
        if (highlightedBody == null) {
            drawScaledText(graphics, font, "No selected body", x, y + SIDE_TITLE, COLOR_MUTED);
            return y + SIDE_TITLE;
        }
        return renderSelectedBodyStats(graphics, font, highlightedBody, snapshot, x, y, textWidth);
    }

    private static int renderSelectedBodyStats(GuiGraphics graphics, Font font, KspBodyState body, KspSnapshot snapshot, int x, int y, int textWidth) {
        drawScaledText(graphics, font, fitLine(font, body.definition().displayName(), textWidth), x, y + SIDE_TITLE, bodyColor(body.definition().id()));
        drawScaledText(graphics, font, String.format("speed  %.0f m/s", body.absoluteVelocity().length()), x, y + SIDE_TITLE + SIDE_LINE, COLOR_MUTED);
        drawScaledText(graphics, font, "history " + snapshot.bodyHistory().getOrDefault(body.definition().id(), List.of()).size(), x, y + SIDE_TITLE + SIDE_LINE * 2, COLOR_MUTED);
        int nextLine = 3;
        if (body.referenceBodyId() != null) {
            KspBodyState parent = snapshot.bodies().get(body.referenceBodyId());
            if (parent != null) {
                double distance = body.absolutePosition().subtract(parent.absolutePosition()).length();
                drawScaledText(graphics, font, String.format("dist   %.0f km", distance / 1000.0), x, y + SIDE_TITLE + SIDE_LINE * nextLine, COLOR_MUTED);
                nextLine++;
                double roche = parent.definition().rocheLimit(body.definition());
                if (roche > 0.0) {
                    drawScaledText(graphics, font, String.format("roche  %.0f km", roche / 1000.0), x, y + SIDE_TITLE + SIDE_LINE * nextLine, COLOR_MUTED);
                    nextLine++;
                    if (distance < roche) {
                        drawScaledText(graphics, font, "ROCHE VIOLATION", x, y + SIDE_TITLE + SIDE_LINE * nextLine, 0xFFFF4444);
                        nextLine++;
                    }
                }
            }
        }
        double rotPeriod = body.definition().rotationPeriodSeconds();
        if (rotPeriod > 0) {
            int totalMinutes = (int) Math.round(rotPeriod / 60.0);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            drawScaledText(graphics, font, String.format("rot    %dh %dm", hours, minutes), x, y + SIDE_TITLE + SIDE_LINE * nextLine, COLOR_MUTED);
            nextLine++;
            double tiltDeg = Math.toDegrees(body.definition().axialTiltRadians());
            drawScaledText(graphics, font, String.format("tilt   %.2f\u00B0", tiltDeg), x, y + SIDE_TITLE + SIDE_LINE * nextLine, COLOR_MUTED);
            nextLine++;
            double phaseDeg = Math.toDegrees(body.rotationPhaseRadians());
            drawScaledText(graphics, font, String.format("phase  %.1f\u00B0", phaseDeg), x, y + SIDE_TITLE + SIDE_LINE * nextLine, COLOR_MUTED);
            nextLine++;
        }
        if (body.perturbed()) {
            drawScaledText(graphics, font, "PERTURBED", x, y + SIDE_TITLE + SIDE_LINE * nextLine, 0xFFFF6644);
            nextLine++;
            drawScaledText(graphics, font, String.format("thrust %.4f m/s^2", body.thrustAcceleration().length()), x, y + SIDE_TITLE + SIDE_LINE * nextLine, COLOR_MUTED);
            nextLine++;
        }
        return y + SIDE_TITLE + SIDE_LINE * nextLine;
    }

    private static void renderLegendPage(GuiGraphics graphics, Font font, int x, int y, int width) {
        int viewportMinX = sidePanelX + 1;
        int viewportMinY = sidePanelY + TAB_HEIGHT + 1;
        int viewportMaxX = sidePanelX + sidePanelWidth - 1;
        int viewportMaxY = sidePanelY + sidePanelHeight - 1;
        int viewportHeight = Math.max(1, viewportMaxY - viewportMinY);
        int contentHeight = estimateLegendHeight();
        int maxScroll = Math.max(0, contentHeight - viewportHeight + 10);
        legendScroll = clampInt(legendScroll, 0, maxScroll);

        graphics.enableScissor(viewportMinX, viewportMinY, viewportMaxX, viewportMaxY);
        renderLegend(graphics, font, x, y - legendScroll, width);
        graphics.disableScissor();
    }

    private static void renderLegend(GuiGraphics graphics, Font font, int x, int y, int width) {
        int lineY = y;
        int columnWidth = Math.max(120, (width - 12) / 2);
        drawScaledText(graphics, font, "Bodies", x, lineY, COLOR_TEXT);
        lineY += SIDE_TITLE;
        legendRow(graphics, font, x, lineY, COLOR_ROOT, "Sun");
        legendRow(graphics, font, x, lineY + SIDE_LINE, COLOR_EARTH, "Earth");
        legendRow(graphics, font, x, lineY + SIDE_LINE * 2, COLOR_MOON, "Moon");
        legendRow(graphics, font, x, lineY + SIDE_LINE * 3, COLOR_VESSEL, "Vessel");

        lineY += SIDE_SECTION + SIDE_LINE * 4;
        drawScaledText(graphics, font, "Orbits", x, lineY, COLOR_TEXT);
        lineY += SIDE_TITLE;
        legendRow(graphics, font, x, lineY, COLOR_REFERENCE_TRACK, "Guide orbit");
        legendRow(graphics, font, x, lineY + SIDE_LINE, COLOR_TRACK, "History path");
        legendRow(graphics, font, x, lineY + SIDE_LINE * 2, COLOR_SELECTED_TRACK, "Selected track");
        legendRow(graphics, font, x, lineY + SIDE_LINE * 3, COLOR_VESSEL_ORBIT, "Vessel orbit");
        legendRow(graphics, font, x, lineY + SIDE_LINE * 4, COLOR_PERIAPSIS, "Periapsis");
        legendRow(graphics, font, x, lineY + SIDE_LINE * 5, COLOR_APOAPSIS, "Apoapsis");

        lineY += SIDE_SECTION + SIDE_LINE * 6;
        drawScaledText(graphics, font, "Projection", x, lineY, COLOR_TEXT);
        lineY += SIDE_TITLE;
        drawScaledText(graphics, font, "Projection: 3D perspective", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Distance: logarithmic scaled", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, fitLine(font, "Selected track follows target", width), x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, fitLine(font, "Moon trails stay parent anchored", width), x, lineY, COLOR_MUTED);

        lineY += SIDE_SECTION + SIDE_LINE * 2;
        drawScaledText(graphics, font, "Controls", x, lineY, COLOR_TEXT);
        lineY += SIDE_TITLE;
        drawScaledText(graphics, font, "Left drag: pan", x, lineY, COLOR_MUTED);
        drawScaledText(graphics, font, "Middle drag: rotate", x + columnWidth, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Wheel: zoom / scroll", x, lineY, COLOR_MUTED);
        drawScaledText(graphics, font, "Tab: cycle body", x + columnWidth, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "F6 / Esc: close", x, lineY, COLOR_MUTED);
        drawScaledText(graphics, font, "Click tabs: switch page", x + columnWidth, lineY, COLOR_MUTED);

        lineY += SIDE_SECTION + SIDE_LINE;
        drawScaledText(graphics, font, "Body Colors", x, lineY, COLOR_TEXT);
        lineY += SIDE_TITLE;
        drawScaledText(graphics, font, "Mercury: dusty inner-rock palette", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Venus: warm dense-atmosphere palette", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Earth: blue oceanic palette", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Moon: neutral gray satellite palette", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Outer giants: stylized gas/ice tones", x, lineY, COLOR_MUTED);

        lineY += SIDE_SECTION + SIDE_LINE * 4;
        drawScaledText(graphics, font, "Track Meaning", x, lineY, COLOR_TEXT);
        lineY += SIDE_TITLE;
        drawScaledText(graphics, font, "Guide orbit uses reference-frame elements", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "History path uses runtime cached positions", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Selected track increases alpha and emphasis", x, lineY, COLOR_MUTED);
        lineY += SIDE_LINE;
        drawScaledText(graphics, font, "Vessel orbit uses current osculating state", x, lineY, COLOR_MUTED);
    }

    private static int estimateLegendHeight() {
        return 360;
    }

    private static void legendRow(GuiGraphics graphics, Font font, int x, int y, int color, String label) {
        graphics.fill(x, y + 3, x + 8, y + 11, color);
        drawScaledText(graphics, font, label, x + 14, y, COLOR_MUTED);
    }

    private static void drawOrbitPath(
            GuiGraphics graphics, KspSnapshot snapshot, KspBodyState body,
            List<SpaceVector> history, int centerX, int centerY, double scale,
            boolean highlighted, int baseColor
    ) {
        if (history == null || history.size() < 2) {
            return;
        }
        List<SpaceVector> renderPath = resolveRenderPath(snapshot, body, history);
        int pathSize = renderPath.size();
        int step = Math.max(1, pathSize / 512);
        ProjectedPoint previous = null;
        int segmentCount = (pathSize - 1) / step;
        int drawn = 0;
        for (int i = 0; i < pathSize; i += step) {
            ProjectedPoint point = projectPoint(renderPath.get(i), centerX, centerY, scale);
            if (previous != null) {
                float ageFactor = segmentCount <= 0 ? 1.0f : drawn / (float) segmentCount;
                int color = fadeColor(highlighted ? COLOR_SELECTED_TRACK : baseColor, highlighted ? ageFactor : ageFactor * 0.75f, highlighted ? 0.95f : 0.45f);
                drawLine(graphics, previous.x, previous.y, point.x, point.y, color);
            }
            previous = point;
            drawn++;
        }
        int lastIndex = pathSize - 1;
        if (lastIndex % step != 0 && lastIndex > 0) {
            ProjectedPoint point = projectPoint(renderPath.get(lastIndex), centerX, centerY, scale);
            if (previous != null) {
                int color = fadeColor(highlighted ? COLOR_SELECTED_TRACK : baseColor, highlighted ? 1.0f : 0.75f, highlighted ? 0.95f : 0.45f);
                drawLine(graphics, previous.x, previous.y, point.x, point.y, color);
            }
        }
    }

    private static void drawReferenceOrbitPath(
            GuiGraphics graphics, KspSnapshot snapshot, KspBodyState body,
            int centerX, int centerY, double scale, boolean highlighted
    ) {
        KspBodyDefinition definition = body.definition();
        if (definition.referenceFrameKind() == KspReferenceFrameKind.SYSTEM_CENTER || definition.orbitPeriodSeconds() <= 0.0) {
            return;
        }
        if (body.perturbed()) {
            return;
        }
        SpaceVector anchor = resolveReferenceAnchor(snapshot, body);
        double centralMu = resolveCentralMu(snapshot, body);
        int samples = 72;
        ProjectedPoint previous = null;
        for (int i = 0; i <= samples; i++) {
            double fraction = i / (double) samples;
            double seconds = snapshot.simulationSeconds() + definition.orbitPeriodSeconds() * fraction;
            SpaceVector absolute = anchor.add(definition.positionAt(seconds, centralMu));
            ProjectedPoint point = projectPoint(absolute, centerX, centerY, scale);
            if (previous != null) {
                int color = highlighted ? fadeColor(COLOR_SELECTED_TRACK, 0.45f, 0.55f) : COLOR_REFERENCE_TRACK;
                drawLine(graphics, previous.x, previous.y, point.x, point.y, color);
            }
            previous = point;
        }
    }

    private static Optional<OsculatingOrbit> computeOsculatingOrbit(KspSnapshot snapshot, KspVesselState vessel) {
        KspBodyState primary = snapshot.bodies().get(vessel.primaryBodyId());
        if (primary == null) {
            return Optional.empty();
        }

        SpaceVector r = vessel.absolutePosition().subtract(primary.absolutePosition());
        SpaceVector v = vessel.absoluteVelocity();
        double mu = primary.definition().gravitationalParameter();

        SpaceVector h = cross(r, v);
        if (h.length() < 1.0E-6) {
            return Optional.empty();
        }

        SpaceVector eVector = cross(v, h).scale(1.0 / mu).subtract(r.normalize());
        double e = eVector.length();
        double energy = v.lengthSquared() * 0.5 - mu / r.length();
        if (Math.abs(energy) < 1.0E-9) {
            return Optional.empty();
        }

        double semiMajorAxis = -mu / (2.0 * energy);
        if (semiMajorAxis <= 0.0 || e >= 1.0) {
            return Optional.empty();
        }

        SpaceVector periDirection = e < 1.0E-5 ? r.normalize() : eVector.normalize();
        SpaceVector qDirection = cross(h.normalize(), periDirection);
        double periapsis = semiMajorAxis * (1.0 - e);
        double apoapsis = semiMajorAxis * (1.0 + e);

        return Optional.of(new OsculatingOrbit(primary.absolutePosition(), periDirection, qDirection, semiMajorAxis, e, periapsis, apoapsis));
    }

    private static void drawVesselOrbit(GuiGraphics graphics, OsculatingOrbit orbit, int centerX, int centerY, double scale) {
        int samples = 160;
        ProjectedPoint previous = null;
        for (int i = 0; i <= samples; i++) {
            double anomaly = (i / (double) samples) * Math.PI * 2.0;
            ProjectedPoint point = projectPoint(orbit.positionAt(anomaly), centerX, centerY, scale);
            if (previous != null) {
                drawLine(graphics, previous.x, previous.y, point.x, point.y, COLOR_VESSEL_ORBIT);
            }
            previous = point;
        }
        ProjectedPoint periapsis = projectPoint(orbit.positionAt(0.0), centerX, centerY, scale);
        ProjectedPoint apoapsis = projectPoint(orbit.positionAt(Math.PI), centerX, centerY, scale);
        drawLine(graphics, periapsis.x - 4, periapsis.y, periapsis.x + 4, periapsis.y, COLOR_PERIAPSIS);
        drawLine(graphics, periapsis.x, periapsis.y - 4, periapsis.x, periapsis.y + 4, COLOR_PERIAPSIS);
        drawLine(graphics, apoapsis.x - 4, apoapsis.y, apoapsis.x + 4, apoapsis.y, COLOR_APOAPSIS);
        drawLine(graphics, apoapsis.x, apoapsis.y - 4, apoapsis.x, apoapsis.y + 4, COLOR_APOAPSIS);
    }

    private static float[] compressedPosition(SpaceVector pos) {
        double distance = pos.length();
        if (distance < 1.0) return new float[]{0.0f, 0.0f, 0.0f};
        double compressed = compressDistance(distance);
        double s = compressed / distance;
        return new float[]{(float) (pos.x() * s), (float) (pos.y() * s), (float) (pos.z() * s)};
    }

    private static float cubeHalfSize(KspBodyDefinition definition) {
        double radius = definition.radius();
        if (radius <= 0.0) return 0.01f;
        float size = (float) (0.012 * Math.log10(radius) * bodyScale);
        return Math.clamp(size, 0.004f, 0.16f);
    }

    private static ProjectedPoint projectPoint(SpaceVector worldPosition, int centerX, int centerY, double scale) {
        double distance = worldPosition.length();
        SpaceVector compressed = distance < 1.0
                ? SpaceVector.zero()
                : worldPosition.normalize().scale(compressDistance(distance));

        double yawCos = Math.cos(cameraYaw);
        double yawSin = Math.sin(cameraYaw);
        double pitchCos = Math.cos(cameraPitch);
        double pitchSin = Math.sin(cameraPitch);

        double x1 = compressed.x() * yawCos - compressed.z() * yawSin;
        double z1 = compressed.x() * yawSin + compressed.z() * yawCos;
        double y2 = compressed.y() * pitchCos - z1 * pitchSin;
        double z2 = compressed.y() * pitchSin + z1 * pitchCos;

        double perspective = 1.0 / (1.0 + z2 * 0.012);
        int screenX = centerX + (int) Math.round(panX + x1 * scale * perspective);
        int screenY = centerY - (int) Math.round(panY + y2 * scale * perspective);
        return new ProjectedPoint(screenX, screenY, z2);
    }

    private static ProjectedPoint projectCompressed(float cx, float cy, float cz, int centerX, int centerY, double scale) {
        double yawCos = Math.cos(cameraYaw);
        double yawSin = Math.sin(cameraYaw);
        double pitchCos = Math.cos(cameraPitch);
        double pitchSin = Math.sin(cameraPitch);

        double x1 = cx * yawCos - cz * yawSin;
        double z1 = cx * yawSin + cz * yawCos;
        double y2 = cy * pitchCos - z1 * pitchSin;
        double z2 = cy * pitchSin + z1 * pitchCos;

        double perspective = 1.0 / (1.0 + z2 * 0.012);
        int screenX = centerX + (int) Math.round(panX + x1 * scale * perspective);
        int screenY = centerY - (int) Math.round(panY + y2 * scale * perspective);
        return new ProjectedPoint(screenX, screenY, z2);
    }

    private static void renderCubes(GuiGraphics graphics, List<CubeRenderable> cubes, int centerX, int centerY, double scale) {
        List<CubeFace> faces = new ArrayList<>();
        for (CubeRenderable cube : cubes) {
            float cx = cube.cx, cy = cube.cy, cz = cube.cz, h = cube.half;
            int color = cube.color;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            float[][] corners = {
                    {cx - h, cy - h, cz - h}, {cx + h, cy - h, cz - h},
                    {cx + h, cy + h, cz - h}, {cx - h, cy + h, cz - h},
                    {cx - h, cy - h, cz + h}, {cx + h, cy - h, cz + h},
                    {cx + h, cy + h, cz + h}, {cx - h, cy + h, cz + h}
            };
            ProjectedPoint[] p = new ProjectedPoint[8];
            for (int i = 0; i < 8; i++) {
                p[i] = projectCompressed(corners[i][0], corners[i][1], corners[i][2], centerX, centerY, scale);
            }

            // 6 faces: indices + brightness multiplier
            int[][] faceIndices = {{3, 2, 6, 7}, {0, 1, 5, 4}, {4, 5, 6, 7}, {0, 1, 2, 3}, {1, 2, 6, 5}, {0, 3, 7, 4}};
            float[] faceBright = {1.0f, 0.45f, 0.8f, 0.6f, 0.7f, 0.55f};
            for (int f = 0; f < 6; f++) {
                int[] idx = faceIndices[f];
                double avgDepth = (p[idx[0]].depth + p[idx[1]].depth + p[idx[2]].depth + p[idx[3]].depth) * 0.25;
                float m = faceBright[f];
                faces.add(new CubeFace(p[idx[0]], p[idx[1]], p[idx[2]], p[idx[3]], r * m, g * m, b * m, a, avgDepth));
            }
        }

        faces.sort(Comparator.comparingDouble(f -> -f.depth));

        graphics.enableScissor(currentModelMinX, currentModelMinY, currentModelMaxX, currentModelMaxY);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (CubeFace face : faces) {
            buffer.addVertex(matrix, face.pa.x, face.pa.y, 0.0f).setColor(face.cr, face.cg, face.cb, face.ca);
            buffer.addVertex(matrix, face.pb.x, face.pb.y, 0.0f).setColor(face.cr, face.cg, face.cb, face.ca);
            buffer.addVertex(matrix, face.pc.x, face.pc.y, 0.0f).setColor(face.cr, face.cg, face.cb, face.ca);
            buffer.addVertex(matrix, face.pd.x, face.pd.y, 0.0f).setColor(face.cr, face.cg, face.cb, face.ca);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        graphics.disableScissor();
    }

    private static void drawAxisLine(GuiGraphics graphics, int centerX, int centerY, SpaceVector axis, double scale, int color) {
        ProjectedPoint start = projectPoint(SpaceVector.zero(), centerX, centerY, scale);
        ProjectedPoint end = projectPoint(axis.scale(DISTANCE_UNIT * 120.0), centerX, centerY, scale);
        drawLine(graphics, start.x, start.y, end.x, end.y, color);
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        pendingLines.add(new float[]{x0, y0, x1, y1, r, g, b, a});
    }

    private static void flushLines(GuiGraphics graphics) {
        if (pendingLines.isEmpty()) {
            return;
        }
        graphics.enableScissor(currentModelMinX, currentModelMinY, currentModelMaxX, currentModelMaxY);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (float[] line : pendingLines) {
            buffer.addVertex(matrix, line[0], line[1], 0.0f).setColor(line[4], line[5], line[6], line[7]);
            buffer.addVertex(matrix, line[2], line[3], 0.0f).setColor(line[4], line[5], line[6], line[7]);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        graphics.disableScissor();
        pendingLines.clear();
    }

    private static double computeMaxDistance(KspSnapshot snapshot) {
        double maxDistance = 1.0;
        for (KspBodyState body : snapshot.bodies().values()) {
            maxDistance = Math.max(maxDistance, body.absolutePosition().length());
        }
        for (KspVesselState vessel : snapshot.vessels().values()) {
            maxDistance = Math.max(maxDistance, vessel.absolutePosition().length());
        }
        return maxDistance;
    }

    private static double compressDistance(double distance) {
        return Math.log10(1.0 + distance / DISTANCE_UNIT);
    }

    private static int bodyColor(String id) {
        if (id.startsWith("asteroid_belt_")) {
            return 0xFF8D8271;
        }
        return switch (id) {
            case "sun" -> COLOR_ROOT;
            case "earth" -> COLOR_EARTH;
            case "moon" -> COLOR_MOON;
            case "mercury" -> 0xFFCCB99D;
            case "venus" -> 0xFFE0B480;
            case "mars" -> 0xFFD37253;
            case "jupiter" -> 0xFFC9A07B;
            case "saturn" -> 0xFFCEBF8A;
            case "uranus" -> 0xFF8BD4D3;
            case "neptune" -> 0xFF5B7DDA;
            default -> 0xFFFFFFFF;
        };
    }

    private static String fitLine(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = builder.toString() + text.charAt(i);
            if (font.width(next) + ellipsisWidth > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder.isEmpty() ? ellipsis : builder + ellipsis;
    }

    private static void drawScaledText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        drawScaledText(graphics, font, text, x, y, color, SIDE_TEXT_SCALE);
    }

    private static void drawScaledText(GuiGraphics graphics, Font font, String text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static String resolveSelectedBodyId(KspSnapshot snapshot) {
        if (selectedBodyId != null && snapshot.bodies().containsKey(selectedBodyId)) {
            return selectedBodyId;
        }
        if (snapshot.bodies().containsKey("earth")) {
            selectedBodyId = "earth";
            return selectedBodyId;
        }
        if (!snapshot.bodies().isEmpty()) {
            selectedBodyId = snapshot.bodies().keySet().iterator().next();
            return selectedBodyId;
        }
        selectedBodyId = null;
        return null;
    }

    private static int fadeColor(int rgbColor, float ageFactor, float maxAlpha) {
        float clampedAge = (float) clamp(ageFactor, 0.0, 1.0);
        float clampedMaxAlpha = (float) clamp(maxAlpha, 0.0, 1.0);
        int alpha = Math.clamp(Math.round((0.08f + clampedAge * 0.92f) * clampedMaxAlpha * 255.0f), 12, 255);
        return (alpha << 24) | (rgbColor & 0x00FFFFFF);
    }

    private static List<SpaceVector> resolveRenderPath(KspSnapshot snapshot, KspBodyState body, List<SpaceVector> history) {
        if (body.referenceBodyId() == null) {
            return history;
        }

        KspBodyState parent = snapshot.bodies().get(body.referenceBodyId());
        List<SpaceVector> parentHistory = snapshot.bodyHistory().get(body.referenceBodyId());
        if (parent == null || parentHistory == null || parentHistory.isEmpty()) {
            return history;
        }

        SpaceVector anchor = parent.absolutePosition();
        int parentSize = parentHistory.size();
        List<SpaceVector> renderPath = new ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            int parentIndex = Math.min(i, parentSize - 1);
            SpaceVector relative = history.get(i).subtract(parentHistory.get(parentIndex));
            renderPath.add(anchor.add(relative));
        }
        return renderPath;
    }

    private static SpaceVector resolveReferenceAnchor(KspSnapshot snapshot, KspBodyState body) {
        if (body.referenceBodyId() == null) {
            return SpaceVector.zero();
        }

        KspBodyState parent = snapshot.bodies().get(body.referenceBodyId());
        return parent == null ? SpaceVector.zero() : parent.absolutePosition();
    }

    private static double resolveCentralMu(KspSnapshot snapshot, KspBodyState body) {
        if (body.referenceBodyId() == null) {
            return body.definition().gravitationalParameter();
        }

        KspBodyState parent = snapshot.bodies().get(body.referenceBodyId());
        return parent == null ? body.definition().gravitationalParameter() : parent.definition().gravitationalParameter();
    }

    private static SpaceVector cross(SpaceVector a, SpaceVector b) {
        return new SpaceVector(
                a.y() * b.z() - a.z() * b.y(),
                a.z() * b.x() - a.x() * b.z(),
                a.x() * b.y() - a.y() * b.x()
        );
    }

    private record ProjectedPoint(int x, int y, double depth) {
    }

    private record CubeRenderable(float cx, float cy, float cz, float half, int color) {
    }

    private record CubeFace(ProjectedPoint pa, ProjectedPoint pb, ProjectedPoint pc, ProjectedPoint pd, float cr, float cg, float cb, float ca, double depth) {
    }

    private record OsculatingOrbit(
            SpaceVector primaryPosition,
            SpaceVector periDirection,
            SpaceVector qDirection,
            double semiMajorAxis,
            double eccentricity,
            double periapsis,
            double apoapsis
    ) {
        private SpaceVector positionAt(double anomaly) {
            double radius = semiMajorAxis * (1.0 - eccentricity * eccentricity) / (1.0 + eccentricity * Math.cos(anomaly));
            SpaceVector inPlane = periDirection.scale(radius * Math.cos(anomaly)).add(qDirection.scale(radius * Math.sin(anomaly)));
            return primaryPosition.add(inPlane);
        }
    }
}
