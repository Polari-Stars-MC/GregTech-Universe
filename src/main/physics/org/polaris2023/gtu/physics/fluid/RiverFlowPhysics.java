package org.polaris2023.gtu.physics.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class RiverFlowPhysics {
    public static final int MAX_SCAN = 6;
    public static final int MIN_CHANNEL_SPAN = 3;
    public static final int MAX_CHANNEL_WIDTH = 4;
    public static final int WHEEL_RADIUS = 2;
    public static final int MIN_WHEEL_RIVER_CONTACTS = 2;
    public static final double MIN_DRIVING_FLOW_SPEED = 0.01D;

    private RiverFlowPhysics() {
    }

    /**
     * Samples the horizontal river current at one water block.
     *
     * Formula:
     * channel = max(xSpan, zSpan) >= MIN_CHANNEL_SPAN and min(xSpan, zSpan) <= MAX_CHANNEL_WIDTH
     * gradient(dir) = sum(max(0, floor(origin) - floor(origin + dir * i)) / i)
     *                 + max(0, surface(origin) - surface(origin + dir * reach)) * 0.35
     * widthFactor = lerp(1.0, 0.55, (width - 1) / (MAX_CHANNEL_WIDTH - 1))
     * continuity = clamp(reach / MAX_SCAN, 0.2, 1.0)
     * speed = clamp((gradient * 0.12 + continuity * 0.22) * widthFactor, 0.0, 0.85)
     */
    public static Vec3 sampleRiverFlow(Level level, BlockPos pos) {
        if (level == null || !isWater(level.getBlockState(pos))) {
            return Vec3.ZERO;
        }

        int east = span(level, pos, Direction.EAST);
        int west = span(level, pos, Direction.WEST);
        int north = span(level, pos, Direction.NORTH);
        int south = span(level, pos, Direction.SOUTH);

        int xSpan = east + west + 1;
        int zSpan = north + south + 1;
        boolean eastWestChannel = xSpan >= MIN_CHANNEL_SPAN && zSpan <= MAX_CHANNEL_WIDTH && xSpan > zSpan + 1;
        boolean northSouthChannel = zSpan >= MIN_CHANNEL_SPAN && xSpan <= MAX_CHANNEL_WIDTH && zSpan > xSpan + 1;
        if (!eastWestChannel && !northSouthChannel) {
            return Vec3.ZERO;
        }

        Direction positive = eastWestChannel ? Direction.EAST : Direction.SOUTH;
        Direction negative = eastWestChannel ? Direction.WEST : Direction.NORTH;
        int width = eastWestChannel ? zSpan : xSpan;

        FlowScore positiveScore = sampleDirection(level, pos, positive);
        FlowScore negativeScore = sampleDirection(level, pos, negative);
        if (positiveScore.speed() <= 0.0D && negativeScore.speed() <= 0.0D) {
            return Vec3.ZERO;
        }

        boolean preferPositive = positiveScore.speed() > negativeScore.speed();
        Direction downstream = preferPositive ? positive : negative;
        FlowScore chosen = preferPositive ? positiveScore : negativeScore;
        double widthFactor = Mth.clampedMap(width, 1.0D, MAX_CHANNEL_WIDTH, 1.0D, 0.55D);
        double continuity = Mth.clamp(chosen.reach() / (double) MAX_SCAN, 0.2D, 1.0D);
        double speed = Mth.clamp((chosen.speed() * 0.12D + continuity * 0.22D) * widthFactor, 0.0D, 0.85D);
        return speed <= MIN_DRIVING_FLOW_SPEED
                ? Vec3.ZERO
                : new Vec3(downstream.getStepX() * speed, 0.0D, downstream.getStepZ() * speed);
    }

    public static double sampleRiverSpeed(Level level, BlockPos pos) {
        return sampleRiverFlow(level, pos).length();
    }

    public static double sampleAverageFrontFlow(Level level, BlockPos pos, Direction front) {
        Direction right = front.getClockWise();

        double total = 0.0D;
        int samples = 0;
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos samplePos = pos.relative(front).relative(right, offset);
            double speed = sampleRiverSpeed(level, samplePos);
            if (speed <= 0.0D) {
                continue;
            }
            total += speed;
            samples++;
        }
        return samples == 0 ? 0.0D : total / samples;
    }

    /**
     * Samples only river water touching either face of a wheel centered on axisPos.
     * Single isolated water blocks are rejected by both the river-channel detector and
     * the minimum contact count.
     */
    public static WheelFlow sampleWheelFlow(Level level, BlockPos axisPos, Direction wheelAxis) {
        if (level == null || axisPos == null || wheelAxis == null || wheelAxis.getAxis() == Direction.Axis.Y) {
            return WheelFlow.NONE;
        }

        Direction lateral = wheelAxis.getClockWise();
        double weightedSpeed = 0.0D;
        double weightSum = 0.0D;
        int waterContacts = 0;
        int riverContacts = 0;
        Vec3 flowTotal = Vec3.ZERO;

        for (int side : new int[]{-1, 1}) {
            Direction sideDirection = side < 0 ? wheelAxis.getOpposite() : wheelAxis;
            for (int horizontal = -WHEEL_RADIUS; horizontal <= WHEEL_RADIUS; horizontal++) {
                for (int vertical = -WHEEL_RADIUS; vertical <= WHEEL_RADIUS; vertical++) {
                    if (horizontal == 0 && vertical == 0) {
                        continue;
                    }

                    BlockPos samplePos = axisPos.relative(sideDirection)
                            .relative(lateral, horizontal)
                            .above(vertical);
                    BlockState state = level.getBlockState(samplePos);
                    if (!isWater(state)) {
                        continue;
                    }

                    waterContacts++;
                    Vec3 flow = sampleRiverFlow(level, samplePos);
                    double speed = flow.length();
                    if (speed <= MIN_DRIVING_FLOW_SPEED) {
                        continue;
                    }

                    riverContacts++;
                    double weight = 1.0D;
                    if (vertical < 0) {
                        weight += 0.25D;
                    }
                    if (Math.abs(horizontal) <= 1) {
                        weight += 0.15D;
                    }

                    weightedSpeed += speed * weight;
                    weightSum += weight;
                    flowTotal = flowTotal.add(flow);
                }
            }
        }

        if (riverContacts < MIN_WHEEL_RIVER_CONTACTS || weightSum <= 0.0D) {
            return new WheelFlow(0.0D, waterContacts, riverContacts, Vec3.ZERO);
        }

        return new WheelFlow(weightedSpeed / weightSum, waterContacts, riverContacts, flowTotal.scale(1.0D / riverContacts));
    }

    public static double sampleWheelSpeed(Level level, BlockPos axisPos, Direction wheelAxis) {
        return sampleWheelFlow(level, axisPos, wheelAxis).speed();
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
        double gradient = 0.0D;

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

        double surfaceGradient = Math.max(0, surfaceY(level, origin) - surfaceY(level, origin.relative(direction, reach))) * 0.35D;
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
        private static final FlowScore ZERO = new FlowScore(0.0D, 0);
    }

    public record WheelFlow(double speed, int waterContacts, int riverContacts, Vec3 averageFlow) {
        private static final WheelFlow NONE = new WheelFlow(0.0D, 0, 0, Vec3.ZERO);

        public boolean canDrive() {
            return speed > MIN_DRIVING_FLOW_SPEED && riverContacts >= MIN_WHEEL_RIVER_CONTACTS;
        }
    }
}
