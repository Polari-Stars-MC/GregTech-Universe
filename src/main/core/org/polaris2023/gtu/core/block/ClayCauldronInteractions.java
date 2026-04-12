package org.polaris2023.gtu.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

public final class ClayCauldronInteractions {
    public static final CauldronInteraction.InteractionMap EMPTY = CauldronInteraction.newInteractionMap("gtu_core_clay_empty");
    public static final CauldronInteraction.InteractionMap WATER = CauldronInteraction.newInteractionMap("gtu_core_clay_water");
    private static boolean bootstrapped;

    private ClayCauldronInteractions() {
    }

    public static BlockState fullWaterCauldron() {
        return levelWaterCauldron(3);
    }

    public static BlockState levelWaterCauldron(int level) {
        return BlockRegistries.WATER_CLAY_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level);
    }

    public static void lowerFillLevel(BlockState state, Level level, BlockPos pos) {
        int nextLevel = state.getValue(LayeredCauldronBlock.LEVEL) - 1;
        BlockState nextState = nextLevel <= 0
                ? BlockRegistries.CLAY_CAULDRON.get().defaultBlockState()
                : state.setValue(LayeredCauldronBlock.LEVEL, nextLevel);
        level.setBlockAndUpdate(pos, nextState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(nextState));
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        EMPTY.map().put(Items.WATER_BUCKET, (state, level, pos, player, hand, stack) ->
                fillEmpty(level, pos, player, hand, stack, new ItemStack(Items.BUCKET)));
        EMPTY.map().put(ItemRegistries.WATER_CLAY_BUCKET.get(), (state, level, pos, player, hand, stack) ->
                fillEmpty(level, pos, player, hand, stack, new ItemStack(ItemRegistries.CLAY_BUCKET.get())));

        WATER.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) ->
                fillBucket(level, pos, player, hand, stack, new ItemStack(Items.WATER_BUCKET)));
        WATER.map().put(ItemRegistries.CLAY_BUCKET.get(), (state, level, pos, player, hand, stack) ->
                fillBucket(level, pos, player, hand, stack, new ItemStack(ItemRegistries.WATER_CLAY_BUCKET.get())));
        WATER.map().put(Items.WATER_BUCKET, (state, level, pos, player, hand, stack) ->
                topUp(level, pos, player, hand, stack, new ItemStack(Items.BUCKET)));
        WATER.map().put(ItemRegistries.WATER_CLAY_BUCKET.get(), (state, level, pos, player, hand, stack) ->
                topUp(level, pos, player, hand, stack, new ItemStack(ItemRegistries.CLAY_BUCKET.get())));
    }

    private static ItemInteractionResult fillEmpty(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack filledStack, ItemStack emptiedResult) {
        if (!level.isClientSide()) {
            Item item = filledStack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(filledStack, player, emptiedResult));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(item));
            BlockState full = fullWaterCauldron();
            level.setBlockAndUpdate(pos, full);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static ItemInteractionResult topUp(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack filledStack, ItemStack emptiedResult) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LayeredCauldronBlock.LEVEL) || state.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            Item item = filledStack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(filledStack, player, emptiedResult));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(item));
            BlockState full = fullWaterCauldron();
            level.setBlockAndUpdate(pos, full);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static ItemInteractionResult fillBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack emptyStack, ItemStack filledResult) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LayeredCauldronBlock.LEVEL) || state.getValue(LayeredCauldronBlock.LEVEL) != 3) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            Item item = emptyStack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(emptyStack, player, filledResult));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(item));
            BlockState empty = BlockRegistries.CLAY_CAULDRON.get().defaultBlockState();
            level.setBlockAndUpdate(pos, empty);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
