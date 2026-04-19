package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.ChunkPos;

import java.util.List;

public record EarthProjectedChunkState(
        int sourceChunkX,
        int sourceChunkZ,
        int wrappedChunkX,
        int minBlockX,
        int maxBlockX,
        int minBlockZ,
        int maxBlockZ,
        boolean insideProjectedEarth,
        boolean wrapsAcrossAntimeridian
) {
    public static final int CHUNK_SIZE = 16;

    public static EarthProjectedChunkState of(int chunkX, int chunkZ) {
        int minBlockX = chunkX << 4;
        int minBlockZ = chunkZ << 4;
        int maxBlockX = minBlockX + CHUNK_SIZE - 1;
        int maxBlockZ = minBlockZ + CHUNK_SIZE - 1;

        int wrappedMinBlockX = EarthBounds.wrapBlockX(minBlockX);
        int wrappedMaxBlockX = EarthBounds.wrapBlockX(maxBlockX);

        return new EarthProjectedChunkState(
                chunkX,
                chunkZ,
                EarthChunkBounds.wrapChunkX(chunkX),
                wrappedMinBlockX,
                wrappedMaxBlockX,
                EarthBounds.clampBlockZ(minBlockZ),
                EarthBounds.clampBlockZ(maxBlockZ),
                maxBlockZ >= EarthBounds.MIN_BLOCK_Z && minBlockZ <= EarthBounds.MAX_BLOCK_Z,
                wrappedMinBlockX > wrappedMaxBlockX
        );
    }

    public ChunkPos sourceChunkPos() {
        return new ChunkPos(sourceChunkX, sourceChunkZ);
    }

    public ChunkPos wrappedChunkPos() {
        return new ChunkPos(wrappedChunkX, sourceChunkZ);
    }

    public List<EarthProjectedChunkWindow> projectedWindows() {
        if (!wrapsAcrossAntimeridian) {
            return List.of(new EarthProjectedChunkWindow(
                    sourceChunkX,
                    sourceChunkZ,
                    sourceChunkX << 4,
                    (sourceChunkX << 4) + CHUNK_SIZE - 1,
                    minBlockX,
                    maxBlockX
            ));
        }

        int sourceMinBlockX = sourceChunkX << 4;
        int sourceMaxBlockX = sourceMinBlockX + CHUNK_SIZE - 1;
        int leftWidth = EarthBounds.MAX_BLOCK_X_EXCLUSIVE - minBlockX;
        int rightWidth = CHUNK_SIZE - leftWidth;

        return List.of(
                new EarthProjectedChunkWindow(
                        sourceChunkX,
                        sourceChunkZ,
                        sourceMinBlockX,
                        sourceMinBlockX + leftWidth - 1,
                        minBlockX,
                        EarthBounds.MAX_BLOCK_X_EXCLUSIVE - 1
                ),
                new EarthProjectedChunkWindow(
                        sourceChunkX,
                        sourceChunkZ,
                        sourceMinBlockX + leftWidth,
                        sourceMaxBlockX,
                        EarthBounds.MIN_BLOCK_X,
                        EarthBounds.MIN_BLOCK_X + rightWidth - 1
                )
        );
    }
}
