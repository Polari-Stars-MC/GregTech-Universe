package org.polaris2023.gtu.core.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.block.ClayCauldronInteractions;
import org.polaris2023.gtu.core.init.AttachmentRegistries;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;
import org.polaris2023.gtu.core.init.ModProperties;
import org.polaris2023.gtu.core.init.tag.BlockTags;

@EventBusSubscriber(modid = GregtechUniverseCore.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void init(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player entity = event.getEntity();
        ItemStack stack = entity.getItemInHand(event.getHand());
        if (tryWashMineral(event, level, pos, state, entity, stack)) {
            return;
        }

        float destroyTime = state.getBlock().defaultDestroyTime();
        // 100 25 15 0.2
        if (destroyTime >= 1.5F && stack.is(Items.FLINT)) {
            ItemStack result = ItemRegistries.FLINT_SHARD.toStack();
            RandomSource random = level.random;
            float v = random.nextFloat();
            if (v <= 0.2F) {
                result.setCount(4);
            } else if (v <= 15.F) {
                result.setCount(3);
            } else if (v < 25F) {
                result.setCount(2);
            }
            entity.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            stack.shrink(1);

            level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), result));
        }
    }

    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent event) {
        BlockState placedBlock = event.getPlacedBlock();
        if (!placedBlock.is(BlockTags.PLACE) || !placedBlock.hasProperty(ModProperties.PLACE)) {
            return;
        }
        if (placedBlock.getValue(ModProperties.PLACE)) {
            return;
        }

        BlockState updatedState = placedBlock.setValue(ModProperties.PLACE, true);
        if (updatedState != placedBlock) {
            event.getLevel().setBlock(event.getPos(), updatedState, Block.UPDATE_ALL);
        }
    }

    private static boolean tryWashMineral(
            PlayerInteractEvent.RightClickBlock event,
            Level level,
            BlockPos pos,
            BlockState state,
            Player player,
            ItemStack held
    ) {
        ItemStack washed = washedResult(held);
        if (washed.isEmpty() || !isWaterCauldron(state)) {
            return false;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (level.isClientSide()) {
            return true;
        }

        if (!player.hasInfiniteMaterials()) {
            held.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
        giveOrDrop(player, washed);
        giveOrDrop(player, new ItemStack(Items.GRAVEL));
        consumeCauldronWater(level, pos, state);
        return true;
    }

    private static boolean isWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON) || state.is(BlockRegistries.WATER_CLAY_CAULDRON.get());
    }

    private static void consumeCauldronWater(Level level, BlockPos pos, BlockState state) {
        if (state.is(BlockRegistries.WATER_CLAY_CAULDRON.get())) {
            ClayCauldronInteractions.lowerFillLevel(state, level, pos);
        } else {
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        }
    }

    private static ItemStack washedResult(ItemStack held) {
        if (held.is(ItemRegistries.GRAVELY_COPPER.get())) {
            return ItemRegistries.WASHED_COPPER_CONCENTRATE.toStack();
        }
        if (held.is(ItemRegistries.GRAVELY_TIN.get())) {
            return ItemRegistries.WASHED_TIN_CONCENTRATE.toStack();
        }
        if (held.is(ItemRegistries.GRAVELY_IRON.get())) {
            return ItemRegistries.WASHED_IRON_CONCENTRATE.toStack();
        }
        return ItemStack.EMPTY;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack, false);
        }
    }
}
