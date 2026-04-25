package org.polaris2023.gtu.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.api.multiblock.runtime.cache.StructureTemplateServices;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureValidationResult;
import org.polaris2023.gtu.core.block.entity.TestMultiblockControllerBlockEntity;

public class TestMultiblockControllerBlock extends Block implements EntityBlock {
    public static final MapCodec<TestMultiblockControllerBlock> CODEC = simpleCodec(TestMultiblockControllerBlock::new);
    public static final BooleanProperty FORMED = BlockStateProperties.LIT;

    public TestMultiblockControllerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TestMultiblockControllerBlockEntity blockEntity) {
            StructureValidationResult result = blockEntity.validateStructure((ServerLevel) level);
            boolean formed = result.formed();
            if (state.getValue(FORMED) != formed) {
                level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_ALL);
            }
            if (formed) {
                serverPlayer.openMenu(blockEntity);
            } else {
                serverPlayer.displayClientMessage(Component.literal(
                        "test_multiblock formed=false, completion=" + String.format("%.2f", result.completion())
                                + ", mismatches=" + result.mismatches().size()), false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TestMultiblockControllerBlockEntity blockEntity) {
            blockEntity.ensureNetworkId();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestMultiblockControllerBlockEntity(pos, state);
    }
}
