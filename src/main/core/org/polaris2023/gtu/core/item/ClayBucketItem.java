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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.polaris2023.gtu.core.init.ItemRegistries;

public class ClayBucketItem extends BucketItem {
    public ClayBucketItem(Properties properties) {
        super(Fluids.EMPTY, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return hitResult.getType() == HitResult.Type.MISS ? InteractionResultHolder.pass(held) : InteractionResultHolder.fail(held);
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        BlockPos relativePos = pos.relative(direction);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(relativePos, direction, held)) {
            return InteractionResultHolder.fail(held);
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BucketPickup bucketPickup)
                || !state.getFluidState().isSource()
                || state.getFluidState().getType() != Fluids.WATER) {
            return InteractionResultHolder.fail(held);
        }

        ItemStack pickedUp = bucketPickup.pickupBlock(player, level, pos, state);
        if (pickedUp.isEmpty()) {
            return InteractionResultHolder.fail(held);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        bucketPickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        ItemStack filled = ItemUtils.createFilledResult(held, player, new ItemStack(ItemRegistries.WATER_CLAY_BUCKET.get()));
        if (!level.isClientSide()) {
            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, filled);
        }

        return InteractionResultHolder.sidedSuccess(filled, level.isClientSide());
    }
}
