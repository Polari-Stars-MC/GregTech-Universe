package org.polaris2023.gtu.space.client.debug;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.client.render.EarthClientSeamController;
import org.polaris2023.gtu.space.client.render.EarthClientSeamState;
import org.polaris2023.gtu.space.simulation.earth.EarthBounds;
import org.polaris2023.gtu.space.simulation.earth.EarthProjection;
import org.polaris2023.gtu.space.simulation.earth.EarthProjectedChunkState;

import java.util.Locale;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class EarthProjectionDebugHud {
    private static final Lazy<KeyMapping> TOGGLE_KEY = Lazy.of(() ->
            new KeyMapping(
                    "key.gtu_space.earth_projection_hud",
                    GLFW.GLFW_KEY_F7,
                    "key.categories.gtu_space"
            )
    );

    private static boolean enabled;

    private EarthProjectionDebugHud() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY.get());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (!TOGGLE_KEY.get().consumeClick()) {
            return;
        }

        enabled = !enabled;
        minecraft.player.displayClientMessage(
                Component.literal("Earth projection HUD: " + (enabled ? "ON" : "OFF")),
                true
        );
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        double x = player.getX();
        double z = player.getZ();
        double wrappedX = EarthBounds.wrapBlockX(x);
        double clampedZ = EarthBounds.clampBlockZ(z);
        double eastDistance = EarthBounds.MAX_BLOCK_X_DOUBLE - x;
        double westDistance = x - EarthBounds.MIN_BLOCK_X_DOUBLE;
        double northDistance = EarthBounds.MAX_BLOCK_Z_DOUBLE - z;
        double southDistance = z - EarthBounds.MIN_BLOCK_Z_DOUBLE;
        EarthProjection.ProjectedEarthPoint projected = EarthProjection.wrapToEarth((int) Math.floor(x), (int) Math.floor(z));
        EarthProjectedChunkState chunkState = EarthProjectedChunkState.of(player.chunkPosition().x, player.chunkPosition().z);
        EarthClientSeamState seamState = EarthClientSeamController.currentState();

        int baseX = 8;
        int baseY = 8;
        int color = 0xE0E0E0;
        int line = 0;

        draw(event, "[Earth Projection / F7]", baseX, baseY, line++, 0xFFFFFF);
        draw(event, String.format(Locale.ROOT, "pos x=%.3f z=%.3f", x, z), baseX, baseY, line++, color);
        draw(event, String.format(Locale.ROOT, "wrapped x=%.3f clamped z=%.3f", wrappedX, clampedZ), baseX, baseY, line++, color);
        draw(event, String.format(Locale.ROOT, "lon=%.6f lat=%.6f", projected.longitudeDegrees(), projected.latitudeDegrees()), baseX, baseY, line++, color);
        draw(event, String.format(Locale.ROOT, "to east seam=%.3f to west seam=%.3f", eastDistance, westDistance), baseX, baseY, line++, color);
        draw(event, String.format(Locale.ROOT, "to north cap=%.3f to south cap=%.3f", northDistance, southDistance), baseX, baseY, line++, color);
        draw(event, "chunk wrapped=" + chunkState.wrapsAcrossAntimeridian() + " windows=" + chunkState.projectedWindows().size(), baseX, baseY, line++, color);
        draw(event, "inside projected earth=" + projected.insideProjectedEarth(), baseX, baseY, line++, color);
        draw(event, "seam active=" + seamState.active() + " camera switched=" + seamState.cameraSwitched(), baseX, baseY, line++, color);
        draw(event, String.format(Locale.ROOT, "seam wrapped x=%.3f", seamState.wrappedX()), baseX, baseY, line, color);
    }

    private static void draw(RenderGuiEvent.Post event, String text, int baseX, int baseY, int line, int color) {
        event.getGuiGraphics().drawString(
                Minecraft.getInstance().font,
                text,
                baseX,
                baseY + line * 10,
                color,
                true
        );
    }
}
