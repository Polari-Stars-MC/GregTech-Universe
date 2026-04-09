package org.polaris2023.gtu.modpacks.events;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.tag.BlockTags;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        Player player = event.getPlayer();
        if (player.isCreative()) return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty() && !state.is(BlockTags.WHITE_LIST_BREAK)) {
            event.setCanceled(true);
        }
        if (!stack.isCorrectToolForDrops(state) && !state.is(BlockTags.WHITE_LIST_BREAK)) {
            event.setCanceled(true);
        }
    }
}
