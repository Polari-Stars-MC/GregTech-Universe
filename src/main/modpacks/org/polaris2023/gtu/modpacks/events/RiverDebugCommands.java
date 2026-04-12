package org.polaris2023.gtu.modpacks.events;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

import java.util.Locale;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class RiverDebugCommands {
    private static final Axis[] AXES = new Axis[]{
            new Axis(1, 0, 0, 1),
            new Axis(0, 1, -1, 0),
            new Axis(1, 1, -1, 1),
            new Axis(1, -1, 1, 1)
    };

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("gtu")
                        .then(Commands.literal("river_tp")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> teleportToRiver(context, 1024))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(128, 8192))
                                        .executes(context -> teleportToRiver(
                                                context,
                                                IntegerArgumentType.getInteger(context, "radius")
                                        ))))
        );
    }

    private static int teleportToRiver(CommandContext<CommandSourceStack> context, int radius) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This debug command can only be used by a player."));
            return 0;
        }

        try {
            RiverTarget target = findRiverTarget(player.serverLevel(), player.blockPosition(), radius);
            if (target == null) {
                source.sendFailure(Component.literal("No clear river candidate found within radius " + radius + "."));
                return 0;
            }

            BlockPos safePos = findSafeTeleportPos(player.serverLevel(), target);
            player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            Locale.ROOT,
                            "Teleported to river candidate: x=%d z=%d width=%d continuity=%d score=%.2f",
                            target.x(),
                            target.z(),
                            target.width(),
                            target.continuity(),
                            target.score()
                    )),
                    false
            );
            return 1;
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "(no message)";
            }
            source.sendFailure(Component.literal(
                    "River debug command failed: " + exception.getClass().getSimpleName() + " - " + message
            ));
            return 0;
        }
    }

    private static RiverTarget findRiverTarget(ServerLevel level, BlockPos origin, int radius) {
        int step = 16;
        RiverTarget best = null;
        int originX = origin.getX();
        int originZ = origin.getZ();

        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radius * radius) {
                    continue;
                }

                int x = originX + dx;
                int z = originZ + dz;
                RiverTarget candidate = evaluateRiverCandidate(level, x, z);
                if (candidate == null) {
                    continue;
                }

                double weightedScore = candidate.score() - Math.sqrt(distanceSquared) * 0.01;
                if (best == null || weightedScore > best.score() - best.distance() * 0.01) {
                    best = candidate.withDistance((int) Math.sqrt(distanceSquared));
                }
            }
        }

        return best;
    }

    private static RiverTarget evaluateRiverCandidate(ServerLevel level, int x, int z) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (surfaceY <= level.getMinBuildHeight() + 1) {
            return null;
        }

        BlockPos surfacePos = new BlockPos(x, surfaceY, z);
        BlockState surfaceState = level.getBlockState(surfacePos);
        if (!surfaceState.is(Blocks.WATER)) {
            return null;
        }

        int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        int depth = surfaceY - floorY;
        if (depth < 2) {
            return null;
        }

        RiverTarget best = null;
        for (Axis axis : AXES) {
            int leftWidth = waterWidth(level, x, z, axis.perpX(), axis.perpZ(), 20);
            int rightWidth = waterWidth(level, x, z, -axis.perpX(), -axis.perpZ(), 20);
            int width = leftWidth + rightWidth + 1;
            if (width < 2 || width > 28) {
                continue;
            }

            int continuity = waterWidth(level, x, z, axis.dx(), axis.dz(), 10)
                    + waterWidth(level, x, z, -axis.dx(), -axis.dz(), 10)
                    + 1;
            if (continuity < 4) {
                continue;
            }

            int leftBankY = bankHeight(level, x + axis.perpX() * (leftWidth + 1), z + axis.perpZ() * (leftWidth + 1));
            int rightBankY = bankHeight(level, x - axis.perpX() * (rightWidth + 1), z - axis.perpZ() * (rightWidth + 1));
            double bankRise = ((leftBankY + rightBankY) * 0.5) - surfaceY;
            if (bankRise < 1.0) {
                continue;
            }

            double score = continuity * 1.5 + bankRise * 1.1 + depth * 1.4 - width * 0.45;
            if (best == null || score > best.score()) {
                best = new RiverTarget(x, surfaceY, z, width, continuity, score, axis, 0);
            }
        }

        return best;
    }

    private static int waterWidth(ServerLevel level, int startX, int startZ, int dx, int dz, int limit) {
        int count = 0;
        for (int step = 1; step <= limit; step++) {
            int x = startX + dx * step;
            int z = startZ + dz * step;
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (surfaceY <= level.getMinBuildHeight() + 1 || !level.getBlockState(new BlockPos(x, surfaceY, z)).is(Blocks.WATER)) {
                break;
            }
            count++;
        }
        return count;
    }

    private static int bankHeight(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static BlockPos findSafeTeleportPos(ServerLevel level, RiverTarget target) {
        Axis axis = target.axis();
        for (int offset = Math.max(2, target.width() / 2); offset <= target.width() / 2 + 6; offset++) {
            BlockPos left = findStandablePos(level, target.x() + axis.perpX() * offset, target.z() + axis.perpZ() * offset);
            if (left != null) {
                return left;
            }

            BlockPos right = findStandablePos(level, target.x() - axis.perpX() * offset, target.z() - axis.perpZ() * offset);
            if (right != null) {
                return right;
            }
        }

        return new BlockPos(target.x(), target.y() + 2, target.z());
    }

    private static BlockPos findStandablePos(ServerLevel level, int x, int z) {
        int y = bankHeight(level, x, z);
        BlockPos ground = new BlockPos(x, y, z);
        BlockState groundState = level.getBlockState(ground);
        if (groundState.isAir() || groundState.is(Blocks.WATER)) {
            return null;
        }

        BlockState above = level.getBlockState(ground.above());
        BlockState aboveTwo = level.getBlockState(ground.above(2));
        if (!above.isAir() || !aboveTwo.isAir()) {
            return null;
        }

        return ground.above();
    }

    private record Axis(int dx, int dz, int perpX, int perpZ) {
    }

    private record RiverTarget(int x, int y, int z, int width, int continuity, double score, Axis axis, int distance) {
        RiverTarget withDistance(int value) {
            return new RiverTarget(x, y, z, width, continuity, score, axis, value);
        }
    }
}
