package org.polaris2023.gtu.space.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT)
public final class EarthClientSeamController {
    private static final EarthClientPlayerProxy PROXY = new EarthClientPlayerProxy();
    private static final double WRAP_JUMP_THRESHOLD = EarthClientSeamState.WRAP_WIDTH_BLOCKS * 0.5D;

    private static EarthClientSeamState currentState = EarthClientSeamState.inactive(0.0D, 0.0D, 0.0D);
    private static double previousX;
    private static boolean initializedPosition;

    private EarthClientSeamController() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || player.level().dimension() != Level.OVERWORLD) {
            reset(minecraft, level);
            return;
        }

        if (!initializedPosition) {
            previousX = player.getX();
            initializedPosition = true;
        }

        double deltaX = player.getX() - previousX;
        previousX = player.getX();

        if (Math.abs(deltaX) >= WRAP_JUMP_THRESHOLD) {
            currentState = EarthClientSeamState.inactive(player.getX(), player.getY(), player.getZ());
            restorePrimaryView(minecraft, player);
            PROXY.setVisible(false);
            return;
        }

        currentState = EarthClientSeamState.from(player.getX(), player.getY(), player.getZ(), deltaX);

        if (!currentState.active()) {
            restorePrimaryView(minecraft, player);
            PROXY.setVisible(false);
            return;
        }

        PROXY.ensurePresent(level, player);
        PROXY.syncFrom(player, currentState);
        PROXY.setVisible(true);

        if (currentState.cameraSwitched()) {
            player.setInvisible(true);
            RemotePlayer proxyEntity = PROXY.entity();
            if (proxyEntity != null && minecraft.getCameraEntity() != proxyEntity) {
                minecraft.setCameraEntity(proxyEntity);
            }
        } else {
            restorePrimaryView(minecraft, player);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = event.getPlayer() != null ? event.getPlayer().clientLevel : minecraft.level;
        reset(minecraft, level);
    }

    public static EarthClientSeamState currentState() {
        return currentState;
    }

    private static void reset(Minecraft minecraft, ClientLevel level) {
        initializedPosition = false;
        currentState = EarthClientSeamState.inactive(0.0D, 0.0D, 0.0D);
        if (minecraft.player != null) {
            restorePrimaryView(minecraft, minecraft.player);
        }
        PROXY.discard(level);
    }

    private static void restorePrimaryView(Minecraft minecraft, LocalPlayer player) {
        player.setInvisible(false);
        if (minecraft.getCameraEntity() != player) {
            minecraft.setCameraEntity(player);
        }
    }
}
