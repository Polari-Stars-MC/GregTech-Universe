package org.polaris2023.gtu.modpacks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverCurrentSampler;

public class WaterDamControllerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WaterDamControllerBlock> CODEC = simpleCodec(WaterDamControllerBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public WaterDamControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        boolean formed = matchesPlaceholderStructure(level, pos, state.getValue(FACING));
        BlockState nextState = state.setValue(FORMED, formed);
        if (nextState != state) {
            level.setBlockAndUpdate(pos, nextState);
        }

        double flow = RiverCurrentSampler.sampleAverageFrontFlow(level, pos, state.getValue(FACING));
        player.displayClientMessage(
                Component.literal(formed ? "Water dam placeholder formed" : "Water dam placeholder not formed")
                        .withStyle(formed ? ChatFormatting.GREEN : ChatFormatting.RED),
                false
        );
        player.displayClientMessage(
                Component.literal(String.format("Detected river flow: %.2f", flow)).withStyle(ChatFormatting.AQUA),
                false
        );
        player.displayClientMessage(
                Component.literal("Final dam layout will be replaced when NBT is provided.").withStyle(ChatFormatting.GRAY),
                false
        );
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    private boolean matchesPlaceholderStructure(Level level, BlockPos controllerPos, Direction front) {
        Direction left = front.getClockWise();
        BlockPos origin = controllerPos.relative(front.getOpposite());

        for (int depth = 0; depth < 3; depth++) {
            for (int height = 0; height < 3; height++) {
                for (int width = -1; width <= 1; width++) {
                    BlockPos target = origin.relative(front.getOpposite(), depth)
                            .relative(Direction.UP, 1 - height)
                            .relative(left, width);

                    boolean controllerSpot = depth == 0 && height == 2 && width == 0;
                    boolean airSpot = depth == 1 && height == 1 && width == 0;
                    BlockState targetState = level.getBlockState(target);
                    if (controllerSpot) {
                        if (target.getX() != controllerPos.getX() || target.getY() != controllerPos.getY() || target.getZ() != controllerPos.getZ()) {
                            return false;
                        }
                        continue;
                    }
                    if (airSpot) {
                        if (!targetState.isAir()) {
                            return false;
                        }
                        continue;
                    }
                    if (!targetState.is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
