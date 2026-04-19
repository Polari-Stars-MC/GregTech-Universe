package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class EarthProjectionService {
    private static volatile EarthProjectionService instance =
            new EarthProjectionService(EarthProjectionPreset.WEB_MERCATOR_1_TO_1_VOID);

    private final EarthProjectionConfig config;

    public EarthProjectionService(EarthProjectionConfig config) {
        this.config = config;
    }

    public EarthProjectionService(EarthProjectionPreset preset) {
        this(preset.config());
    }

    public static EarthProjectionService get() {
        return instance;
    }

    public static void configure(EarthProjectionConfig config) {
        instance = new EarthProjectionService(config);
    }

    public static void configure(EarthProjectionPreset preset) {
        instance = new EarthProjectionService(preset);
    }

    public EarthProjectionConfig config() {
        return config;
    }

    public EarthProjectedContext project(DensityFunction.FunctionContext source) {
        return new EarthProjectedContext(source);
    }

    public EarthProjectedChunkState projectChunk(int chunkX, int chunkZ) {
        return EarthProjectedChunkState.of(chunkX, chunkZ);
    }

    public EarthWorldgenDecider.BlockDecision decideBlock(DensityFunction.FunctionContext source) {
        EarthProjectedContext context = project(source);
        if (context.insideProjectedEarth()) {
            return new EarthWorldgenDecider.BlockDecision(
                    context,
                    EarthWorldgenDecider.Region.PROJECTED_EARTH,
                    config.outOfBoundsPolicy().fallbackBlockState()
            );
        }
        return new EarthWorldgenDecider.BlockDecision(
                context,
                config.outOfBoundsPolicy().region(),
                config.outOfBoundsPolicy().fallbackBlockState()
        );
    }

    public EarthWorldgenDecider.ChunkDecision decideChunk(int chunkX, int chunkZ) {
        EarthProjectedChunkState chunkState = projectChunk(chunkX, chunkZ);
        EarthWorldgenDecider.Region region = chunkState.insideProjectedEarth()
                ? EarthWorldgenDecider.Region.PROJECTED_EARTH
                : config.outOfBoundsPolicy().region();
        return new EarthWorldgenDecider.ChunkDecision(chunkState, region);
    }

    public double sampleProjected(DensityFunction densityFunction, DensityFunction.FunctionContext source, double fallback) {
        EarthProjectedContext projected = project(source);
        return projected.insideProjectedEarth() ? densityFunction.compute(projected) : fallback;
    }

    public BlockState resolveOutOfBoundsBlock(DensityFunction.FunctionContext source) {
        return decideBlock(source).fallbackBlockState();
    }

    public boolean shouldGenerateChunk(int chunkX, int chunkZ) {
        return decideChunk(chunkX, chunkZ).shouldGenerateChunk();
    }

    public boolean shouldGenerateTerrain(DensityFunction.FunctionContext source) {
        return decideBlock(source).shouldGenerateTerrain();
    }
}
