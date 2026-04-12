package org.polaris2023.gtu.modpacks.worldgen.river;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class RiverCurrentSampler {
    private static final int MAX_SCAN = 6;
    private static final int MIN_AXIS_SPAN = 3;
    private static final int MAX_CHANNEL_WIDTH = 4;

    private RiverCurrentSampler() {
    }

    public static Vec3 sampleFlow(Level level, BlockPos pos) {
        if (level == null || !isWater(level.getBlockState(pos))) {
            return Vec3.ZERO;
        }

        int east = span(level, pos, Direction.EAST);
        int west = span(level, pos, Direction.WEST);
        int north = span(level, pos, Direction.NORTH);
        int south = span(level, pos, Direction.SOUTH);

        int xSpan = east + west + 1;
        int zSpan = north + south + 1;
        boolean eastWestChannel = xSpan >= MIN_AXIS_SPAN && zSpan <= MAX_CHANNEL_WIDTH && xSpan > zSpan + 1;
        boolean northSouthChannel = zSpan >= MIN_AXIS_SPAN && xSpan <= MAX_CHANNEL_WIDTH && zSpan > xSpan + 1;
        if (!eastWestChannel && !northSouthChannel) {
            return Vec3.ZERO;
        }

        Direction positive = eastWestChannel ? Direction.EAST : Direction.SOUTH;
        Direction negative = eastWestChannel ? Direction.WEST : Direction.NORTH;
        int width = eastWestChannel ? zSpan : xSpan;

        FlowScore positiveScore = sampleDirection(level, pos, positive);
        FlowScore negativeScore = sampleDirection(level, pos, negative);
        if (positiveScore.speed() <= 0.0 && negativeScore.speed() <= 0.0) {
            return Vec3.ZERO;
        }

        boolean preferPositive = positiveScore.speed() > negativeScore.speed();
        Direction downstream = preferPositive ? positive : negative;
        FlowScore chosen = preferPositive ? positiveScore : negativeScore;
        double widthPenalty = Mth.clampedMap(width, 1.0, MAX_CHANNEL_WIDTH, 1.0, 0.55);
        double continuity = Mth.clamp(chosen.reach() / (double) MAX_SCAN, 0.2, 1.0);
        double speed = Mth.clamp((chosen.speed() * 0.12 + continuity * 0.22) * widthPenalty, 0.0, 0.85);
        return speed <= 0.01 ? Vec3.ZERO : new Vec3(downstream.getStepX() * speed, 0.0, downstream.getStepZ() * speed);
    }

    public static double sampleSpeed(Level level, BlockPos pos) {
        return sampleFlow(level, pos).length();
    }

    public static double sampleAverageFrontFlow(Level level, BlockPos pos, Direction front) {
        Direction right = switch (front) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> Direction.SOUTH;
        };

        double total = 0.0;
        int samples = 0;
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos samplePos = pos.relative(front).relative(right, offset);
            double speed = sampleSpeed(level, samplePos);
            if (speed <= 0.0) {
                continue;
            }
            total += speed;
            samples++;
        }
        return samples == 0 ? 0.0 : total / samples;
    }

    private static int span(Level level, BlockPos origin, Direction direction) {
        int span = 0;
        for (int i = 1; i <= MAX_SCAN; i++) {
            BlockPos cursor = origin.relative(direction, i);
            if (!isWater(level.getBlockState(cursor))) {
                break;
            }
            span++;
        }
        return span;
    }

    private static FlowScore sampleDirection(Level level, BlockPos origin, Direction direction) {
        int originFloor = floorY(level, origin);
        int reach = 0;
        double gradient = 0.0;

        for (int i = 1; i <= MAX_SCAN; i++) {
            BlockPos cursor = origin.relative(direction, i);
            if (!isWater(level.getBlockState(cursor))) {
                break;
            }
            reach++;
            int cursorFloor = floorY(level, cursor);
            gradient += Math.max(0, originFloor - cursorFloor) / (double) i;
        }

        if (reach == 0) {
            return FlowScore.ZERO;
        }

        double surfaceGradient = Math.max(0, surfaceY(level, origin) - surfaceY(level, origin.relative(direction, reach))) * 0.35;
        return new FlowScore(gradient + surfaceGradient, reach);
    }

    private static int floorY(Level level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ()) - 1;
    }

    private static int surfaceY(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        while (mutable.getY() < level.getMaxBuildHeight() - 1 && isWater(level.getBlockState(mutable.above()))) {
            mutable.move(Direction.UP);
        }
        return mutable.getY();
    }

    private static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.BUBBLE_COLUMN);
    }

    private record FlowScore(double speed, int reach) {
        private static final FlowScore ZERO = new FlowScore(0.0, 0);
    }
}
