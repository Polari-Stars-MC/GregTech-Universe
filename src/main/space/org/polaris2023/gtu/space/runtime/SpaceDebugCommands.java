package org.polaris2023.gtu.space.runtime;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID)
public final class SpaceDebugCommands {
    private SpaceDebugCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("gtu")
                        .then(Commands.literal("space")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("enter").executes(context -> execute(context, Action.ENTER)))
                                .then(Commands.literal("leave").executes(context -> execute(context, Action.LEAVE)))
                                .then(Commands.literal("status").executes(context -> execute(context, Action.STATUS)))
                                .then(Commands.literal("start_takeoff").executes(context -> execute(context, Action.START_TAKEOFF)))
                                .then(Commands.literal("start_landing").executes(context -> execute(context, Action.START_LANDING)))
                                .then(Commands.literal("bodies").executes(context -> execute(context, Action.BODIES)))
                                .then(Commands.literal("focus")
                                        .then(Commands.argument("body", StringArgumentType.word())
                                                .executes(context -> execute(context, Action.FOCUS_BODY)))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context, Action action) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        SpaceManager manager = SpaceManager.get(source.getServer());
        try {
            switch (action) {
                case ENTER -> {
                    manager.enterSpace(player);
                    manager.syncPlayer(player);
                    source.sendSuccess(() -> Component.literal("Entered SD immediately."), false);
                }
                case LEAVE -> {
                    manager.leaveSpace(player);
                    manager.syncPlayer(player);
                    source.sendSuccess(() -> Component.literal("Returned to the stable PD position."), false);
                }
                case STATUS -> source.sendSuccess(() -> Component.literal(manager.statusSummary(player)), false);
                case START_TAKEOFF -> {
                    manager.beginTakeoff(player);
                    manager.syncPlayer(player);
                    source.sendSuccess(() -> Component.literal("Takeoff transition started."), false);
                }
                case START_LANDING -> {
                    manager.beginLanding(player);
                    manager.syncPlayer(player);
                    source.sendSuccess(() -> Component.literal("Landing transition started."), false);
                }
                case BODIES -> source.sendSuccess(
                        () -> Component.literal("Bodies: " + String.join(", ", manager.availableBodyIds())),
                        false
                );
                case FOCUS_BODY -> {
                    String body = StringArgumentType.getString(context, "body");
                    manager.debugFocusBody(player, body);
                    manager.syncPlayer(player);
                    source.sendSuccess(() -> Component.literal("Focused space session on body: " + body), false);
                }
            }
            return 1;
        } catch (IllegalStateException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private enum Action {
        ENTER,
        LEAVE,
        STATUS,
        START_TAKEOFF,
        START_LANDING,
        BODIES,
        FOCUS_BODY
    }
}
