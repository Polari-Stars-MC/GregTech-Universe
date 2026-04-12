package org.polaris2023.gtu.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class ClayCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<ClayCauldronBlock> CODEC = simpleCodec(ClayCauldronBlock::new);
    private static final float RAIN_FILL_CHANCE = 0.05F;

    public ClayCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties, ClayCauldronInteractions.EMPTY);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return false;
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid == Fluids.WATER;
    }

    @Override
    protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        if (fluid == Fluids.WATER) {
            BlockState filled = ClayCauldronInteractions.fullWaterCauldron();
            level.setBlockAndUpdate(pos, filled);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(filled));
            level.levelEvent(1047, pos, 0);
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN && level.getRandom().nextFloat() < RAIN_FILL_CHANCE) {
            BlockState filled = ClayCauldronInteractions.levelWaterCauldron(1);
            level.setBlockAndUpdate(pos, filled);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(filled));
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ItemRegistries.CLAY_CAULDRON.get());
    }
}
