package org.polaris2023.gtu.physics.load;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块摩擦力管理器
 * <p>
 * 为不同材质的方块提供基于现实物理的摩擦系数。
 * 采用混合模式：特殊方块（冰、粘液等）保留原版值，普通方块根据材质计算。
 */
public class BlockFrictionManager {

    /**
     * 默认摩擦系数（MC 原版默认值）
     */
    public static final float DEFAULT_FRICTION = 0.6f;

    /**
     * 判断方块是否为特殊方块（保留原版摩擦力）
     * <p>
     * 这些方块的摩擦力由原版或其他模组控制，不做修改
     */
    private static boolean isSpecialBlock(String id) {
        // 冰类 - 高滑度
        if (id.contains("ice") && !id.contains("packed_ice")) return true;
        if (id.contains("packed_ice") || id.contains("blue_ice")) return true;

        // 粘液和蜂蜜 - 特殊物理效果
        if (id.contains("slime") || id.contains("honey")) return true;

        // 其他模组的特殊滑块
        if (containsAny(id, "slippery", "slippy", "slick")) return true;

        return false;
    }

    /**
     * 获取方块的摩擦系数
     * <p>
     * 混合模式：特殊方块返回原版值，普通方块根据材质计算
     *
     * @param state   方块状态
     * @param level   世界
     * @param pos     位置
     * @param entity  实体（可为 null）
     * @param original 原版摩擦系数
     * @return 修改后的摩擦系数
     */
    public static float getFriction(BlockState state, LevelReader level, BlockPos pos,
                                    Entity entity, float original) {
        String id = state.getBlock().builtInRegistryHolder().key().location().toString();

        // 特殊方块保留原版值
        if (isSpecialBlock(id)) {
            return original;
        }

        // 根据材质计算摩擦力
        return calculateFriction(id, original);
    }

    /**
     * 根据方块 ID 计算摩擦系数
     * <p>
     * 参考现实物理摩擦系数，转换为 MC 的摩擦力系统。
     * MC 中摩擦系数越高 = 越滑（冰 0.98，普通 0.6）
     * 现实中摩擦系数越高 = 越粗糙
     * <p>
     * 转换公式：mcFriction = 0.6 + (1.0 - realFriction) * 0.4
     * 这样 realFriction=0.4（较滑）→ mcFriction=0.84
     * realFriction=0.8（较粗糙）→ mcFriction=0.68
     */
    private static float calculateFriction(String id, float original) {
        // ── 金属方块 ──
        if (containsAny(id, "iron_block", "gold_block", "copper_block", "netherite_block")) {
            return 0.55f; // 金属表面较滑
        }
        if (containsAny(id, "iron_door", "iron_trapdoor", "iron_bars")) {
            return 0.55f;
        }

        // ── 石材（粗糙表面）──
        if (containsAny(id, "stone", "cobblestone", "andesite", "diorite", "granite")) {
            return 0.58f; // 石头表面粗糙
        }
        if (id.contains("deepslate")) return 0.62f; // 深板岩更粗糙
        if (containsAny(id, "obsidian", "crying_obsidian")) return 0.52f; // 黑曜石光滑
        if (containsAny(id, "basalt", "blackstone")) return 0.60f;
        if (id.contains("bedrock")) return 0.65f;

        // ── 砖类 ──
        if (containsAny(id, "bricks") && !id.contains("nether")) return 0.58f;
        if (id.contains("nether_brick")) return 0.55f;
        if (id.contains("prismarine")) return 0.50f; // 海晶湿润，较滑

        // ── 木材（中等摩擦）──
        if (containsAny(id, "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "bamboo")) {
            if (containsAny(id, "planks", "button", "pressure_plate")) return 0.52f;
            if (containsAny(id, "log", "stem")) return 0.55f;
            return 0.52f;
        }
        if (containsAny(id, "crimson", "warped")) {
            if (containsAny(id, "stem", "hyphae")) return 0.50f;
            return 0.52f;
        }

        // ── 沙土类（较滑）──
        if (containsAny(id, "sand", "red_sand")) return 0.42f; // 沙子较滑
        if (id.contains("gravel")) return 0.45f;
        if (containsAny(id, "dirt", "podzol", "farmland", "rooted_dirt")) return 0.50f;
        if (containsAny(id, "grass_block", "mycelium")) return 0.48f;
        if (id.contains("mud") || id.contains("muddy")) return 0.35f; // 泥很滑
        if (containsAny(id, "clay")) return 0.40f;
        if (containsAny(id, "soul_sand", "soul_soil")) return 0.38f; // 灵魂沙很滑

        // ── 雪和冰（冰已在特殊方块中排除）──
        if (id.contains("snow") || id.contains("powder_snow")) return 0.45f;

        // ── 羊毛和织物（中等摩擦）──
        if (id.contains("wool")) return 0.55f;
        if (id.contains("carpet")) return 0.58f;

        // ── 玻璃（光滑）──
        if (id.contains("glass")) return 0.48f;

        // ── 农作物和植物（较滑）──
        if (id.contains("hay_block") || id.contains("hay_bale")) return 0.50f;
        if (containsAny(id, "leaves", "grass", "fern", "flower")) return 0.40f;
        if (id.contains("moss_block")) return 0.45f;
        if (id.contains("sponge")) return 0.50f;

        // ── 下界和末地 ──
        if (id.contains("netherrack")) return 0.55f;
        if (id.contains("glowstone")) return 0.50f;
        if (id.contains("end_stone")) return 0.58f;
        if (id.contains("purpur")) return 0.52f;
        if (id.contains("end_portal_frame")) return 0.50f;

        // ── 功能方块 ──
        if (containsAny(id, "crafting_table", "furnace", "blast_furnace", "smoker")) return 0.55f;
        if (containsAny(id, "chest", "trapped_chest", "ender_chest")) return 0.52f;
        if (id.contains("anvil")) return 0.50f;
        if (id.contains("cauldron")) return 0.48f;
        if (id.contains("enchanting_table")) return 0.52f;
        if (id.contains("beacon")) return 0.48f;

        // ── 红石和机械 ──
        if (containsAny(id, "rail", "powered_rail", "detector_rail", "activator_rail")) return 0.45f;
        if (id.contains("hopper")) return 0.50f;
        if (id.contains("dispenser") || id.contains("dropper")) return 0.52f;
        if (id.contains("piston")) return 0.52f;

        // ── 矿石（使用原版值）──
        if (id.contains("_ore") || id.contains("ore_")) return original;

        // ── 默认：保留原版值 ──
        return original;
    }

    // ─── 工具方法 ──────────────────────────────────────────

    private static boolean containsAny(String str, String... keywords) {
        for (String kw : keywords) {
            if (str.contains(kw)) return true;
        }
        return false;
    }
}
