package org.polaris2023.gtu.modpacks.worldgen.river;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.polaris2023.gtu.physics.fluid.RiverFlowPhysics;

public final class RiverCurrentSampler {
    private RiverCurrentSampler() {
    }

    public static Vec3 sampleFlow(Level level, BlockPos pos) {
        return RiverFlowPhysics.sampleRiverFlow(level, pos);
    }

    public static double sampleSpeed(Level level, BlockPos pos) {
        return RiverFlowPhysics.sampleRiverSpeed(level, pos);
    }

    public static double sampleAverageFrontFlow(Level level, BlockPos pos, Direction front) {
        return RiverFlowPhysics.sampleAverageFrontFlow(level, pos, front);
    }

    public static double sampleWheelFrontFlow(Level level, BlockPos axisPos, Direction front) {
        return RiverFlowPhysics.sampleWheelSpeed(level, axisPos, front);
    }
}
