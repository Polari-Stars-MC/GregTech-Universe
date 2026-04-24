package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;

public final class EarthProjectionSampler {
    private EarthProjectionSampler() {
    }

    public static double sampleWrapped(DensityFunction densityFunction, DensityFunction.FunctionContext source) {
        return densityFunction.compute(new EarthProjectedContext(source));
    }

    public static double sampleWrappedOrElse(DensityFunction densityFunction, DensityFunction.FunctionContext source, double fallback) {
        EarthProjectedContext projected = new EarthProjectedContext(source);
        return projected.insideProjectedEarth() ? densityFunction.compute(projected) : fallback;
    }

    public static DensityFunction.SinglePointContext projectedPoint(int blockX, int blockY, int blockZ) {
        return EarthProjectedContext.of(blockX, blockY, blockZ).projectedPoint();
    }

    public static List<EarthProjectedChunkWindow> projectedChunkWindows(int chunkX, int chunkZ) {
        return EarthProjectedChunkState.of(chunkX, chunkZ).projectedWindows();
    }

    public static boolean isOutsideProjectedEarth(DensityFunction.FunctionContext source) {
        return !new EarthProjectedContext(source).insideProjectedEarth();
    }
}
