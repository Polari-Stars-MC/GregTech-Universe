package org.polaris2023.gtu.physics.load;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家负重管理器
 * <p>
 * 计算玩家当前负重并影响移动能力
 */
public class PlayerLoadManager {

    /**
     * 玩家基础质量 (kg) - 不包括装备
     */
    public static final float PLAYER_BASE_MASS = 70.0f;

    /**
     * 基础负重上限 (kg)
     */
    public static final float BASE_CARRY_CAPACITY = 60.0f;

    /**
     * 计算玩家当前总负重 (kg)
     *
     * @param player 玩家
     * @return 总负重 (千克)
     */
    public static float getTotalMass(Player player) {
        float totalMass = PLAYER_BASE_MASS;

        // 计算背包物品质量
        totalMass += getInventoryMass(player.getInventory());

        // 计算盔甲质量
        totalMass += getArmorMass(player);

        // 计算副手物品质量
        totalMass += ItemMassProvider.getMass(player.getOffhandItem());

        return totalMass;
    }

    /**
     * 计算背包总质量
     */
    public static float getInventoryMass(Inventory inventory) {
        float mass = 0.0f;

        // 主背包
        for (int i = 0; i < inventory.items.size(); i++) {
            mass += ItemMassProvider.getMass(inventory.items.get(i));
        }

        // 快捷栏已包含在 items 中

        return mass;
    }

    /**
     * 计算盔甲总质量
     */
    public static float getArmorMass(Player player) {
        float mass = 0.0f;
        for (ItemStack armor : player.getArmorSlots()) {
            mass += ItemMassProvider.getMass(armor);
        }
        return mass;
    }

    /**
     * 获取负重比例 (当前负重 / 负重上限)
     * <p>
     * 只计算物品负重，不包括玩家自身体重
     *
     * @param player 玩家
     * @return 负重比例, 0.0 = 无负重, 1.0 = 满载, >1.0 = 超载
     */
    public static float getLoadRatio(Player player) {
        // 只计算物品质量，不包括玩家自身体重
        float itemMass = getTotalMass(player) - PLAYER_BASE_MASS;
        float capacity = getCarryCapacity(player);
        return itemMass / capacity;
    }

    /**
     * 获取玩家负重上限
     */
    public static float getCarryCapacity(Player player) {
        // 可以根据玩家属性、附魔等调整
        return BASE_CARRY_CAPACITY;
    }

    /**
     * 检查玩家是否超载
     */
    public static boolean isOverloaded(Player player) {
        return getLoadRatio(player) > 1.0f;
    }

    /**
     * 获取负重等级
     *
     * @return 0=轻装, 1=正常, 2=重载, 3=超载
     */
    public static int getLoadLevel(Player player) {
        float ratio = getLoadRatio(player);
        if (ratio < 0.5f) return 0;      // 轻装
        if (ratio < 0.8f) return 1;      // 正常
        if (ratio < 1.0f) return 2;      // 重载
        return 3;                         // 超载
    }

    /**
     * 计算移动速度修正系数
     * <p>
     * 基于负重比例返回速度修正
     *
     * @param player 玩家
     * @return 速度修正系数 (0.0 ~ 1.0+)
     */
    public static float getSpeedModifier(Player player) {
        float ratio = getLoadRatio(player);

        if (ratio <= 0.5f) {
            // 轻装：正常速度
            return 1.0f;
        } else if (ratio <= 0.8f) {
            // 正常负重：轻微减速
            return 0.95f;
        } else if (ratio <= 1.0f) {
            // 重载：明显减速
            return 0.8f - (ratio - 0.8f) * 0.5f;
        } else {
            // 超载：严重减速
            float overload = ratio - 1.0f;
            return Math.max(0.3f, 0.7f - overload * 0.2f);
        }
    }

    /**
     * 计算跳跃高度修正系数
     *
     * @param player 玩家
     * @return 跳跃高度修正系数
     */
    public static float getJumpModifier(Player player) {
        float ratio = getLoadRatio(player);

        if (ratio <= 0.5f) {
            return 1.0f;
        } else if (ratio <= 0.8f) {
            return 0.9f;
        } else if (ratio <= 1.0f) {
            return 0.7f;
        } else {
            // 超载时可能无法跳跃
            float overload = ratio - 1.0f;
            return Math.max(0.0f, 0.5f - overload * 0.3f);
        }
    }

    /**
     * 获取负重信息字符串
     */
    public static String getLoadInfo(Player player) {
        float totalMass = getTotalMass(player);
        float capacity = getCarryCapacity(player);
        float ratio = getLoadRatio(player);

        String status = switch (getLoadLevel(player)) {
            case 0 -> "§a轻装";
            case 1 -> "§e正常";
            case 2 -> "§6重载";
            case 3 -> "§c超载";
            default -> "§f未知";
        };

        return String.format("%s | 质量: %.1f / %.1f kg (%.0f%%)",
                status, totalMass, capacity, ratio * 100);
    }
}
