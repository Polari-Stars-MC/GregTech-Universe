package org.polaris2023.gtu.modpacks.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.tag.BlockTags;
import org.polaris2023.gtu.modpacks.worldgen.feature.GroundStickDisplayFeature;

import java.util.UUID;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class PlayerEvents {

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        Player player = event.getPlayer();
        if (player.isCreative()) return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.isDamageableItem()) return;
        if (stack.isEmpty() && !state.is(BlockTags.WHITE_LIST_BREAK)) {
            event.setCanceled(true);
        }
        if (!stack.isCorrectToolForDrops(state) && !state.is(BlockTags.WHITE_LIST_BREAK)) {
            event.setCanceled(true);
        }
        if (stack.getDamageValue() == stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }
    }

    @SubscribeEvent
    public static void attackGroundStick(AttackEntityEvent event) {
        if (handleGroundStickInteraction(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void interactGroundStick(PlayerInteractEvent.EntityInteract event) {
        if (handleGroundStickInteraction(event.getEntity(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tickGroundStickInteraction(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Interaction interaction) || interaction.level().isClientSide()) {
            return;
        }
        if (!interaction.getTags().contains(GroundStickDisplayFeature.GROUND_STICK_INTERACTION_TAG)) {
            return;
        }
        ServerLevel level = (ServerLevel) interaction.level();
        if (!hasGroundSupport(interaction)) {
            destroyGroundStick(level, interaction, resolvePairedDisplay(level, interaction), true);
            return;
        }
        if (interaction.getLastAttacker() instanceof Player player) {
            handleGroundStickInteraction(player, interaction);
        }
    }

    @SubscribeEvent
    public static void tickGroundStickDisplay(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Display display) || display.level().isClientSide()) {
            return;
        }
        if (!display.getTags().contains(GroundStickDisplayFeature.GROUND_STICK_DISPLAY_TAG)) {
            return;
        }
        ServerLevel level = (ServerLevel) display.level();
        if (!hasGroundSupport(display)) {
            destroyGroundStick(level, resolvePairedInteraction(level, display), display, true);
        }
    }

    private static boolean handleGroundStickInteraction(Player player, Entity target) {
        if (player.level().isClientSide()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();

        if (target instanceof Interaction interaction && interaction.getTags().contains(GroundStickDisplayFeature.GROUND_STICK_INTERACTION_TAG)) {
            destroyGroundStick(player, level, interaction, resolvePairedDisplay(level, interaction));
            return true;
        }

        if (target instanceof Display display && display.getTags().contains(GroundStickDisplayFeature.GROUND_STICK_DISPLAY_TAG)) {
            destroyGroundStick(player, level, resolvePairedInteraction(level, display), display);
            return true;
        }

        return false;
    }

    private static void destroyGroundStick(Player player, ServerLevel level, Interaction interaction, Display display) {
        destroyGroundStick(level, interaction, display, !player.isCreative());
    }

    private static void destroyGroundStick(ServerLevel level, Interaction interaction, Display display, boolean dropStick) {
        if (interaction == null && display == null) {
            return;
        }
        if (dropStick) {
            Entity dropSource = interaction != null ? interaction : display;
            dropSource.spawnAtLocation(new ItemStack(Items.STICK));
        }

        if (display != null) {
            display.discard();
        }
        if (interaction != null) {
            interaction.discard();
        }
    }

    private static boolean hasGroundSupport(Entity entity) {
        BlockState belowState = entity.level().getBlockState(entity.blockPosition().below());
        return belowState.isFaceSturdy(entity.level(), entity.blockPosition().below(), net.minecraft.core.Direction.UP);
    }

    private static Display resolvePairedDisplay(ServerLevel level, Interaction interaction) {
        if (!interaction.getPersistentData().hasUUID(GroundStickDisplayFeature.PAIRED_DISPLAY_UUID)) {
            return null;
        }
        UUID displayUuid = interaction.getPersistentData().getUUID(GroundStickDisplayFeature.PAIRED_DISPLAY_UUID);
        Entity display = level.getEntity(displayUuid);
        if (display instanceof Display typedDisplay && display.getTags().contains(GroundStickDisplayFeature.GROUND_STICK_DISPLAY_TAG)) {
            return typedDisplay;
        }
        return null;
    }

    private static Interaction resolvePairedInteraction(ServerLevel level, Display display) {
        if (!display.getPersistentData().hasUUID(GroundStickDisplayFeature.PAIRED_INTERACTION_UUID)) {
            return null;
        }
        UUID interactionUuid = display.getPersistentData().getUUID(GroundStickDisplayFeature.PAIRED_INTERACTION_UUID);
        Entity interaction = level.getEntity(interactionUuid);
        if (interaction instanceof Interaction typedInteraction && interaction.getTags().contains(GroundStickDisplayFeature.GROUND_STICK_INTERACTION_TAG)) {
            return typedInteraction;
        }
        return null;
    }
}
