package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class EarthChunkBounds {
    public static final int MIN_CHUNK_X = SectionPos.blockToSectionCoord(EarthBounds.MIN_BLOCK_X);
    public static final int MAX_CHUNK_X = SectionPos.blockToSectionCoord(EarthBounds.MAX_BLOCK_X_EXCLUSIVE - 1);
    public static final int MIN_CHUNK_Z = SectionPos.blockToSectionCoord(EarthBounds.MIN_BLOCK_Z);
    public static final int MAX_CHUNK_Z = SectionPos.blockToSectionCoord(EarthBounds.MAX_BLOCK_Z);
    public static final int WRAP_WIDTH_CHUNKS = MAX_CHUNK_X - MIN_CHUNK_X + 1;

    private EarthChunkBounds() {
    }

    public static boolean isWithinProjectedEarth(int chunkX, int chunkZ) {
        return chunkZ >= MIN_CHUNK_Z && chunkZ <= MAX_CHUNK_Z;
    }

    public static boolean isOutsideProjectedEarth(int chunkX, int chunkZ) {
        return !isWithinProjectedEarth(chunkX, chunkZ);
    }

    public static int wrapChunkX(int chunkX) {
        return MIN_CHUNK_X + Math.floorMod(chunkX - MIN_CHUNK_X, WRAP_WIDTH_CHUNKS);
    }

    public static ChunkPos wrapChunkPos(ChunkPos chunkPos) {
        return new ChunkPos(wrapChunkX(chunkPos.x), chunkPos.z);
    }
}
