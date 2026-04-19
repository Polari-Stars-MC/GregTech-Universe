package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;

public final class EarthWorldgenBridge {
    private static EarthProjectionService service = EarthProjectionService.get();

    private EarthWorldgenBridge() {
    }

    public static void configure(EarthProjectionConfig config) {
        EarthProjectionService.configure(config);
        service = EarthProjectionService.get();
    }

    public static void configure(EarthProjectionPreset preset) {
        EarthProjectionService.configure(preset);
        service = EarthProjectionService.get();
    }

    public static EarthProjectionConfig config() {
        return service.config();
    }

    public static EarthProjectedContext project(DensityFunction.FunctionContext source) {
        return service.project(source);
    }

    public static EarthProjectedChunkState projectChunk(int chunkX, int chunkZ) {
        return service.projectChunk(chunkX, chunkZ);
    }

    public static List<EarthProjectedChunkWindow> projectedChunkWindows(int chunkX, int chunkZ) {
        return EarthProjectionSampler.projectedChunkWindows(chunkX, chunkZ);
    }

    public static EarthWorldgenDecider.BlockDecision decideBlock(DensityFunction.FunctionContext source) {
        return service.decideBlock(source);
    }

    public static EarthWorldgenDecider.ChunkDecision decideChunk(int chunkX, int chunkZ) {
        return service.decideChunk(chunkX, chunkZ);
    }

    public static double sampleProjected(DensityFunction densityFunction, DensityFunction.FunctionContext source, double fallback) {
        return service.sampleProjected(densityFunction, source, fallback);
    }

    public static BlockState resolveOutOfBoundsBlock(DensityFunction.FunctionContext source) {
        return service.resolveOutOfBoundsBlock(source);
    }

    public static boolean shouldGenerateChunk(int chunkX, int chunkZ) {
        return service.shouldGenerateChunk(chunkX, chunkZ);
    }

    public static boolean shouldGenerateTerrain(DensityFunction.FunctionContext source) {
        return service.shouldGenerateTerrain(source);
    }
}
