package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class EarthWorldgenDecider {
    private EarthWorldgenDecider() {
    }

    public static BlockDecision decideBlock(int blockX, int blockY, int blockZ) {
        return decideBlock(EarthProjectedContext.of(blockX, blockY, blockZ));
    }

    public static BlockDecision decideBlock(DensityFunction.FunctionContext source) {
        return decideBlock(new EarthProjectedContext(source));
    }

    public static BlockDecision decideBlock(EarthProjectedContext context) {
        if (context.insideProjectedEarth()) {
            return new BlockDecision(context, Region.PROJECTED_EARTH, Blocks.AIR.defaultBlockState());
        }
        return new BlockDecision(context, Region.OUT_OF_BOUNDS_VOID, Blocks.VOID_AIR.defaultBlockState());
    }

    public static ChunkDecision decideChunk(int chunkX, int chunkZ) {
        EarthProjectedChunkState chunkState = EarthProjectedChunkState.of(chunkX, chunkZ);
        Region region = chunkState.insideProjectedEarth() ? Region.PROJECTED_EARTH : Region.OUT_OF_BOUNDS_VOID;
        return new ChunkDecision(chunkState, region);
    }

    public enum Region {
        PROJECTED_EARTH,
        OUT_OF_BOUNDS_AIR,
        OUT_OF_BOUNDS_VOID
    }

    public record BlockDecision(
            EarthProjectedContext context,
            Region region,
            BlockState fallbackBlockState
    ) {
        public boolean shouldGenerateTerrain() {
            return region == Region.PROJECTED_EARTH;
        }
    }

    public record ChunkDecision(
            EarthProjectedChunkState chunkState,
            Region region
    ) {
        public boolean shouldGenerateChunk() {
            return region == Region.PROJECTED_EARTH;
        }
    }
}
