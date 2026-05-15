package org.polaris2023.gtu.modpacks;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.data.material.GTMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class CopperToolVeinMining {
    private static final int MAX_BLOCKS = 64;
    private static final int DURABILITY_COST = 16;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> MINING = ThreadLocal.withInitial(() -> false);

    private CopperToolVeinMining() {
    }

    public static void setEnabled(Player player, boolean enabled) {
        if (enabled) {
            ENABLED_PLAYERS.add(player.getUUID());
        } else {
            ENABLED_PLAYERS.remove(player.getUUID());
        }
    }

    public static void clear(Player player) {
        ENABLED_PLAYERS.remove(player.getUUID());
    }

    public static boolean tryMine(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState,
                                  Predicate<BlockState> canBreak) {
        if (MINING.get() || !ENABLED_PLAYERS.contains(player.getUUID()) || originState.isAir()) {
            return false;
        }

        ItemStack tool = player.getMainHandItem();
        if (!isCopperTool(tool) || !tool.isCorrectToolForDrops(originState)) {
            return false;
        }

        List<BlockPos> targets = collectTargets(level, origin, originState, tool, canBreak);
        if (targets.size() <= 1) {
            return false;
        }

        int originalDamage = tool.isDamageableItem() ? tool.getDamageValue() : 0;
        int broken = 0;
        MINING.set(true);
        try {
            for (BlockPos target : targets) {
                if (player.getMainHandItem() != tool || tool.isEmpty()) {
                    break;
                }

                BlockState state = level.getBlockState(target);
                if (!isSameBlock(originState, state) || state.isAir() || !canBreak.test(state)
                        || !tool.isCorrectToolForDrops(state)) {
                    continue;
                }

                if (tool.isDamageableItem()) {
                    tool.setDamageValue(0);
                }
                boolean destroyed = ToolHelper.destroyBlock(player, tool, target, target.equals(origin));
                if (tool.isDamageableItem() && !tool.isEmpty()) {
                    tool.setDamageValue(originalDamage);
                }
                if (destroyed) {
                    broken++;
                }
            }
        } finally {
            MINING.set(false);
            if (tool.isDamageableItem() && !tool.isEmpty()) {
                tool.setDamageValue(originalDamage);
            }
        }

        if (broken > 0 && !tool.isEmpty()) {
            ToolHelper.damageItem(tool, player, DURABILITY_COST);
        }
        return true;
    }

    public static boolean isCopperTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IGTTool tool && tool.getMaterial() == GTMaterials.Copper;
    }

    public static List<BlockPos> collectTargets(Level level, BlockPos origin, BlockState originState,
                                                ItemStack tool, Predicate<BlockState> canBreak) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> targets = new ArrayList<>(MAX_BLOCKS);

        BlockPos immutableOrigin = origin.immutable();
        queue.add(immutableOrigin);
        visited.add(immutableOrigin);

        while (!queue.isEmpty() && targets.size() < MAX_BLOCKS) {
            BlockPos current = queue.removeFirst();
            if (!level.hasChunkAt(current)) {
                continue;
            }

            BlockState state = level.getBlockState(current);
            if (!isSameBlock(originState, state) || state.isAir()
                    || state.getDestroySpeed(level, current) < 0.0F
                    || !canBreak.test(state)
                    || !tool.isCorrectToolForDrops(state)) {
                continue;
            }

            targets.add(current);
            if (targets.size() >= MAX_BLOCKS) {
                break;
            }

            for (Direction direction : DIRECTIONS) {
                BlockPos next = current.relative(direction).immutable();
                if (visited.add(next)) {
                    queue.addLast(next);
                }
            }
        }

        return targets;
    }

    private static boolean isSameBlock(BlockState originState, BlockState state) {
        Block originBlock = originState.getBlock();
        return state.getBlock() == originBlock;
    }
}
