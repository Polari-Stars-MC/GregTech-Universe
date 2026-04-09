package org.polaris2023.gtu.core.event;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.ItemRegistries;

@EventBusSubscriber(modid = GregtechUniverseCore.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void init(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        float destroyTime = state.getBlock().defaultDestroyTime();
        Player entity = event.getEntity();
        ItemStack stack = entity.getItemInHand(event.getHand());
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

}
