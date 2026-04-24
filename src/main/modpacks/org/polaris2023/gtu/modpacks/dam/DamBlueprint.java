package org.polaris2023.gtu.modpacks.dam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public final class DamBlueprint {
    public static final int WIDTH = 7;
    public static final int HEIGHT = 7;
    public static final int DEPTH = 3;
    public static final int STACK_SPACING = DEPTH;
    public static final int MAX_SEGMENTS = 16;

    public static final BlockPos CONTROLLER_LOCAL = new BlockPos(6, 0, 1);
    public static final BlockPos AXIS_LOCAL = new BlockPos(3, 3, 1);
    public static final BlockPos SHAFT_LOCAL = new BlockPos(3, 3, 3);
    public static final BlockPos HATCH_LOCAL = new BlockPos(3, 3, 4);

    private static final List<BlockPos> BLADE_OFFSETS = List.of(
            new BlockPos(-1, -2, 0), new BlockPos(0, -2, 0), new BlockPos(1, -2, 0),
            new BlockPos(-2, -1, 0), new BlockPos(2, -1, 0),
            new BlockPos(-2, 0, 0), new BlockPos(2, 0, 0),
            new BlockPos(-2, 1, 0), new BlockPos(2, 1, 0),
            new BlockPos(-1, 2, 0), new BlockPos(0, 2, 0), new BlockPos(1, 2, 0),
            new BlockPos(-1, -1, 0), new BlockPos(0, -1, 0), new BlockPos(1, -1, 0),
            new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
            new BlockPos(-1, 1, 0), new BlockPos(0, 1, 0), new BlockPos(1, 1, 0)
    );

    private DamBlueprint() {
    }

    public static List<BlockPos> bladeOffsets() {
        return BLADE_OFFSETS;
    }

    public static BlockPos toWorld(BlockPos origin, BlockPos local, Direction facing) {
        return origin.offset(rotateX(local.getX() - CONTROLLER_LOCAL.getX(), local.getZ() - CONTROLLER_LOCAL.getZ(), facing),
                local.getY() - CONTROLLER_LOCAL.getY(),
                rotateZ(local.getX() - CONTROLLER_LOCAL.getX(), local.getZ() - CONTROLLER_LOCAL.getZ(), facing));
    }

    public static BlockPos rotateAxisOffset(BlockPos axisPos, BlockPos axisRelativeOffset, Direction facing) {
        return axisPos.offset(
                rotateX(axisRelativeOffset.getX(), axisRelativeOffset.getZ(), facing),
                axisRelativeOffset.getY(),
                rotateZ(axisRelativeOffset.getX(), axisRelativeOffset.getZ(), facing)
        );
    }

    public static BlockPos stackControllerPos(BlockPos masterControllerPos, Direction facing, int segmentIndex) {
        if (segmentIndex == 0) {
            return masterControllerPos;
        }
        Direction stackDirection = facing.getClockWise();
        return masterControllerPos.relative(stackDirection, STACK_SPACING * segmentIndex);
    }

    public static Direction rotationAxisDirection(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    public static Direction stackDirection(Direction facing) {
        return facing.getClockWise();
    }

    private static int rotateX(int localX, int localZ, Direction facing) {
        return switch (facing) {
            case NORTH -> -localX;
            case SOUTH -> localX;
            case WEST -> localZ;
            case EAST -> -localZ;
            default -> localX;
        };
    }

    private static int rotateZ(int localX, int localZ, Direction facing) {
        return switch (facing) {
            case NORTH -> -localZ;
            case SOUTH -> localZ;
            case WEST -> -localX;
            case EAST -> localX;
            default -> localZ;
        };
    }
}
