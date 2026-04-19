package org.polaris2023.gtu.space.simulation.earth;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum EarthOutOfBoundsPolicy {
    AIR(Blocks.AIR.defaultBlockState(), EarthWorldgenDecider.Region.OUT_OF_BOUNDS_AIR),
    VOID_AIR(Blocks.VOID_AIR.defaultBlockState(), EarthWorldgenDecider.Region.OUT_OF_BOUNDS_VOID);

    private final BlockState fallbackBlockState;
    private final EarthWorldgenDecider.Region region;

    EarthOutOfBoundsPolicy(BlockState fallbackBlockState, EarthWorldgenDecider.Region region) {
        this.fallbackBlockState = fallbackBlockState;
        this.region = region;
    }

    public BlockState fallbackBlockState() {
        return fallbackBlockState;
    }

    public EarthWorldgenDecider.Region region() {
        return region;
    }
}
