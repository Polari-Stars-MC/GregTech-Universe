package org.polaris2023.gtu.core.worldgen.river;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class RiverAquiferProxy implements Aquifer {
    private final Aquifer delegate;
    private boolean shouldScheduleFluidUpdate;

    public RiverAquiferProxy(Aquifer delegate) {
        this.delegate = delegate;
    }

    @Override
    public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
        BlockState state = delegate.computeSubstance(context, substance);
        shouldScheduleFluidUpdate = delegate.shouldScheduleFluidUpdate();
        return state;
    }

    public void markRiverFluidUpdate() {
        shouldScheduleFluidUpdate = true;
    }

    public void clearFluidUpdate() {
        shouldScheduleFluidUpdate = false;
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return shouldScheduleFluidUpdate;
    }
}
