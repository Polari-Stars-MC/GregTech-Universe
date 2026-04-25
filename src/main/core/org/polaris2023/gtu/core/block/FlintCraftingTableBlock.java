package org.polaris2023.gtu.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.api.multiblock.runtime.cache.StructureTemplateServices;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureValidationResult;
import org.polaris2023.gtu.core.menu.FlintCraftingMenu;

public class FlintCraftingTableBlock extends CraftingTableBlock {
    public static final MapCodec<FlintCraftingTableBlock> CODEC = simpleCodec(FlintCraftingTableBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("block.gtu_core.flint_crafting_table");

    public FlintCraftingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (int containerId, Inventory inventory, net.minecraft.world.entity.player.Player player) ->
                        new FlintCraftingMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            StructureValidationResult result = StructureTemplateServices.validation().validate(
                    level,
                    pos,
                    GregtechUniverseCore.id("test_multiblock"),
                    level.getGameTime()
            );
            serverPlayer.displayClientMessage(Component.literal(
                    "test_multiblock formed=" + result.formed()
                            + ", completion=" + String.format("%.2f", result.completion())
                            + ", mismatches=" + result.mismatches().size()
                            + ", quickProbe=" + result.quickProbePassed()), false);
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
