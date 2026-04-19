package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.blending.Blender;

public record EarthProjectedContext(
        int sourceBlockX,
        int sourceBlockY,
        int sourceBlockZ,
        int wrappedBlockX,
        int clampedBlockZ,
        boolean insideProjectedEarth,
        Blender blender
) implements DensityFunction.FunctionContext {

    public EarthProjectedContext(DensityFunction.FunctionContext source) {
        this(
                source.blockX(),
                source.blockY(),
                source.blockZ(),
                EarthProjection.wrapToEarth(source.blockX(), source.blockZ()).wrappedBlockX(),
                EarthProjection.wrapToEarth(source.blockX(), source.blockZ()).clampedBlockZ(),
                EarthProjection.wrapToEarth(source.blockX(), source.blockZ()).insideProjectedEarth(),
                source.getBlender()
        );
    }

    public static EarthProjectedContext of(int blockX, int blockY, int blockZ) {
        EarthProjection.ProjectedEarthPoint point = EarthProjection.wrapToEarth(blockX, blockZ);
        return new EarthProjectedContext(
                blockX,
                blockY,
                blockZ,
                point.wrappedBlockX(),
                point.clampedBlockZ(),
                point.insideProjectedEarth(),
                Blender.empty()
        );
    }

    public DensityFunction.SinglePointContext projectedPoint() {
        return new DensityFunction.SinglePointContext(wrappedBlockX, sourceBlockY, clampedBlockZ);
    }

    @Override
    public int blockX() {
        return wrappedBlockX;
    }

    @Override
    public int blockY() {
        return sourceBlockY;
    }

    @Override
    public int blockZ() {
        return clampedBlockZ;
    }

    @Override
    public Blender getBlender() {
        return blender;
    }
}
