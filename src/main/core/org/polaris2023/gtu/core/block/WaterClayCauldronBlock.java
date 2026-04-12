package org.polaris2023.gtu.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class WaterClayCauldronBlock extends LayeredCauldronBlock {
    public static final MapCodec<WaterClayCauldronBlock> CODEC = simpleCodec(WaterClayCauldronBlock::new);

    public WaterClayCauldronBlock(BlockBehaviour.Properties properties) {
        super(Biome.Precipitation.RAIN, ClayCauldronInteractions.WATER, properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<LayeredCauldronBlock> codec() {
        return (MapCodec<LayeredCauldronBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ItemRegistries.CLAY_CAULDRON.get());
    }
}
