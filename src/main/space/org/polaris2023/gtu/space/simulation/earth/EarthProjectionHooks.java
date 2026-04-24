package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class EarthProjectionHooks {
    private EarthProjectionHooks() {
    }

    public static boolean shouldProject(Level level) {
        return level != null && level.dimension() == Level.OVERWORLD;
    }

    public static boolean shouldProject(ServerLevel level) {
        return shouldProject((Level) level);
    }

    public static boolean shouldProject(ClientLevel level) {
        return shouldProject((Level) level);
    }

    public static boolean shouldProjectChunk(Level level, int chunkX, int chunkZ) {
        return shouldProject(level) && EarthWorldgenBridge.shouldGenerateChunk(chunkX, chunkZ);
    }

    public static int wrapChunkX(int chunkX) {
        return EarthChunkBounds.wrapChunkX(chunkX);
    }
}
