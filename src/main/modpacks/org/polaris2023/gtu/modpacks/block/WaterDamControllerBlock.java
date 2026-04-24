package org.polaris2023.gtu.modpacks.block;

import com.mojang.serialization.MapCodec;
import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamControllerBlockEntity;
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.init.BlockEntityRegistries;
import org.polaris2023.gtu.modpacks.menu.WaterDamMenu;

/**
 * 水坝控制器方块。
 * <p>
 * 右键空手：尝试成型/打开UI。
 * 持有对应等级的机壳方块右键：设置等级。
 * </p>
 */
public class WaterDamControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<WaterDamControllerBlock> CODEC = simpleCodec(WaterDamControllerBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 13);

    public WaterDamControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(TIER, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    // ---- 方块放置 ----

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // ---- 交互逻辑 ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WaterDamControllerBlockEntity damBE)) {
            return InteractionResult.PASS;
        }

        if (!damBE.isFormed()) {
            // 尝试成型
            boolean formed = damBE.tryForm();
            if (formed) {
                BlockState newState = state.setValue(FORMED, true);
                level.setBlockAndUpdate(pos, newState);
                player.displayClientMessage(
                        Component.literal("水坝多方块成型成功！").withStyle(ChatFormatting.GREEN),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("水坝结构不完整，请检查蓝图。").withStyle(ChatFormatting.RED),
                        true
                );
            }
        } else {
            // 已成型 — 打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntityUIFactory.INSTANCE.openUI(damBE, serverPlayer);
            }
        }

        if (!damBE.isFormed() && player instanceof ServerPlayer serverPlayer) {
            BlockEntityUIFactory.INSTANCE.openUI(damBE, serverPlayer);
        }

        return InteractionResult.SUCCESS;
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

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WaterDamControllerBlockEntity damBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 检查手持物品是否是某个等级的机壳方块
        Block heldBlock = Block.byItem(stack.getItem());
        if (heldBlock != null) {
            DamTier matchedTier = DamTier.fromCasingBlock(heldBlock);
            if (matchedTier != damBE.getTier()) {
                damBE.setTier(matchedTier);
                BlockState newState = state.setValue(TIER, matchedTier.getIndex());
                level.setBlockAndUpdate(pos, newState);
                player.displayClientMessage(
                        Component.literal("水坝等级设置为: ").withStyle(ChatFormatting.GOLD)
                                .append(matchedTier.getDisplayComponent()),
                        true
                );
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private void sendDamInfo(Player player, WaterDamControllerBlockEntity damBE) {
        player.displayClientMessage(Component.literal("=== 水坝控制器信息 ===").withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(
                Component.literal("等级: ").withStyle(ChatFormatting.GRAY)
                        .append(damBE.getTier().getDisplayComponent()),
                false
        );
        player.displayClientMessage(
                Component.literal(String.format("应力输出: %.0f SU", damBE.getStressOutput()))
                        .withStyle(ChatFormatting.AQUA),
                false
        );
        player.displayClientMessage(
                Component.literal(String.format("旋转速度: %.1f RPM", damBE.getRotationSpeed()))
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        player.displayClientMessage(
                Component.literal(String.format("河流流速: %.2f", damBE.getRiverFlowSpeed()))
                        .withStyle(ChatFormatting.BLUE),
                false
        );
        player.displayClientMessage(
                Component.literal(String.format("连接水坝: %d", damBE.getConnectedDamCount() + 1))
                        .withStyle(ChatFormatting.YELLOW),
                false
        );
    }

    // ---- 方块实体 ----

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterDamControllerBlockEntity(BlockEntityRegistries.WATER_DAM_CONTROLLER_BE.get(), pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != BlockEntityRegistries.WATER_DAM_CONTROLLER_BE.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (l, p, s, be) -> WaterDamControllerBlockEntity.clientTick(l, p, s, (WaterDamControllerBlockEntity) be);
        } else {
            return (l, p, s, be) -> WaterDamControllerBlockEntity.serverTick(l, p, s, (WaterDamControllerBlockEntity) be);
        }
    }

    // ---- 方块状态和旋转 ----

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
        builder.add(FACING, FORMED, TIER);
    }

    // ---- 方块移除 ----

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WaterDamControllerBlockEntity damBE) {
                damBE.disassemble();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
