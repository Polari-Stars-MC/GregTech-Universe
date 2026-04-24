package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EarthSeamlessTravelService {
    private static final double SEAM_EPSILON = 1.0E-4D;
    private static final double MAX_CROSSING_OVERSHOOT_BLOCKS = 32.0D;
    private static final long WRAP_COOLDOWN_TICKS = 10L;
    private static final Map<UUID, Long> LAST_WRAP_TICK = new ConcurrentHashMap<>();

    private EarthSeamlessTravelService() {
    }

    public static boolean shouldHandle(ServerPlayer player) {
        return player.level().dimension() == Level.OVERWORLD && !player.isRemoved() && !player.isPassenger();
    }

    public static boolean wrapPlayerIfNeeded(ServerPlayer player) {
        if (!shouldHandle(player)) {
            return false;
        }

        long gameTime = player.serverLevel().getGameTime();
        if (isOnCooldown(player.getUUID(), gameTime)) {
            return false;
        }

        double sourceX = player.getX();
        if (!isCrossingSeamNow(player, sourceX)) {
            return false;
        }

        double wrappedX = EarthBounds.wrapBlockX(sourceX);
        if (Math.abs(wrappedX - sourceX) < SEAM_EPSILON) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        player.teleportTo(level, wrappedX, player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        LAST_WRAP_TICK.put(player.getUUID(), gameTime);
        return true;
    }

    private static boolean isCrossingSeamNow(ServerPlayer player, double sourceX) {
        double previousX = player.xOld;

        if (sourceX > EarthBounds.MAX_BLOCK_X_DOUBLE) {
            double overshoot = sourceX - EarthBounds.MAX_BLOCK_X_DOUBLE;
            return overshoot <= MAX_CROSSING_OVERSHOOT_BLOCKS
                    && previousX <= EarthBounds.MAX_BLOCK_X_DOUBLE + SEAM_EPSILON
                    && player.getDeltaMovement().x > 0.0D;
        }

        if (sourceX < EarthBounds.MIN_BLOCK_X_DOUBLE) {
            double overshoot = EarthBounds.MIN_BLOCK_X_DOUBLE - sourceX;
            return overshoot <= MAX_CROSSING_OVERSHOOT_BLOCKS
                    && previousX >= EarthBounds.MIN_BLOCK_X_DOUBLE - SEAM_EPSILON
                    && player.getDeltaMovement().x < 0.0D;
        }

        return false;
    }

    private static boolean isOnCooldown(UUID playerId, long gameTime) {
        Long lastWrap = LAST_WRAP_TICK.get(playerId);
        return lastWrap != null && gameTime - lastWrap < WRAP_COOLDOWN_TICKS;
    }
}
