package org.polaris2023.gtu.modpacks.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.polaris2023.gtu.modpacks.blockentity.DamShaftBlockEntity;
import org.polaris2023.gtu.modpacks.init.BlockEntityRegistries;

/**
 * 水坝传动杆方块。
 * <p>
 * 继承 Create 的 {@link ShaftBlock}，连接水坝轴心和应力输出仓。
 * 从侧面延伸，具有 Create 标准的旋转动画。
 * 动画由 Create 的 {@code KineticBlockEntityRenderer} 自动渲染。
 * </p>
 */
public class DamShaftBlock extends RotatedPillarKineticBlock implements IBE<DamShaftBlockEntity> {
    public static final MapCodec<DamShaftBlock> CODEC = simpleCodec(DamShaftBlock::new);

    public DamShaftBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<DamShaftBlockEntity> getBlockEntityClass() {
        return DamShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DamShaftBlockEntity> getBlockEntityType() {
        return BlockEntityRegistries.DAM_SHAFT_BE.get();
    }

    @Override
    protected MapCodec<? extends RotatedPillarKineticBlock> codec() {
        return CODEC;
    }
}
