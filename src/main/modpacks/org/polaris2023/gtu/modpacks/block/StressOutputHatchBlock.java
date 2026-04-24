package org.polaris2023.gtu.modpacks.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.polaris2023.gtu.modpacks.blockentity.StressOutputHatchBlockEntity;
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.init.BlockEntityRegistries;

/**
 * 应力输出仓方块。
 * <p>
 * 接入 Create 动力学网络，根据等级输出应力。
 * 每个等级的外壳使用对应的 GT 机壳材质（原始人等级使用砖块）。
 * 内含传动杆，接收水坝的旋转输出。
 * </p>
 */
public class StressOutputHatchBlock extends DirectionalKineticBlock implements IBE<StressOutputHatchBlockEntity> {

    private final DamTier tier;

    public StressOutputHatchBlock(Properties properties, DamTier tier) {
        super(properties);
        this.tier = tier;
    }

    public DamTier getTier() {
        return tier;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING) || face == state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = context.getNearestLookingDirection().getOpposite();
        if (preferred.getAxis() == Direction.Axis.Y) {
            preferred = context.getHorizontalDirection();
        }
        return defaultBlockState().setValue(FACING, preferred);
    }

    // ---- IBE ----

    @Override
    public Class<StressOutputHatchBlockEntity> getBlockEntityClass() {
        return StressOutputHatchBlockEntity.class;
    }

    @Override
    public BlockEntityType<StressOutputHatchBlockEntity> getBlockEntityType() {
        return BlockEntityRegistries.STRESS_OUTPUT_HATCH_BE.get();
    }

    @Override
    protected MapCodec<? extends DirectionalKineticBlock> codec() {
        return simpleCodec(p -> new StressOutputHatchBlock(p, this.tier));
    }
}
