package org.polaris2023.gtu.core.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class WaterClayBucketItem extends BucketItem {
    public WaterClayBucketItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(held);
        } else if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.fail(held);
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        BlockPos relativePos = pos.relative(direction);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(relativePos, direction, held)) {
            return InteractionResultHolder.fail(held);
        }

        BlockState state = level.getBlockState(pos);
        BlockPos targetPos = canBlockContainFluid(player, level, pos, state) ? pos : relativePos;
        if (!this.emptyContents(player, level, targetPos, hitResult, held)) {
            return InteractionResultHolder.fail(held);
        }

        this.checkExtraContent(player, level, held, targetPos);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, targetPos, held);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        ItemStack emptied = player.hasInfiniteMaterials() ? held : new ItemStack(ItemRegistries.CLAY_BUCKET.get());
        return InteractionResultHolder.sidedSuccess(emptied, level.isClientSide());
    }
}
