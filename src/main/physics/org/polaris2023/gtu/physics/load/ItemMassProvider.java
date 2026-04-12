package org.polaris2023.gtu.physics.load;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;
import org.polaris2023.gtu.physics.init.DataComponents;

/**
 * 物品质量提供器
 * <p>
 * 通过 {@link ModifyDefaultComponentsEvent} 在物品注册阶段为所有物品写入默认质量组件，
 * 运行时 {@link #getMass(ItemStack)} 直接从 DataComponent 读取。
 * <p>
 * 质量参考现实物理数据，按材质和物品类型分别设定。
 */
@EventBusSubscriber(modid = GregtechUniversePhysics.MOD_ID)
public class ItemMassProvider {

    public static final float DEFAULT_MASS = 0.5f;

    // ─── 材质枚举 ─────────────────────────────────────────

    private enum Material {
        WOOD, STONE, IRON, GOLD, DIAMOND, NETHERITE, LEATHER, CHAIN, TURTLE, COPPER
    }

    private enum ToolType {
        SWORD, PICKAXE, AXE, SHOVEL, HOE
    }

    // ─── 工具质量表 [材质][类型] (kg) ──────────────────────
    //                   剑     镐     斧     铲     锄
    private static final float[][] TOOL_MASS = {
        /* WOOD      */ { 0.5f,  0.8f,  1.0f,  0.6f,  0.7f },
        /* STONE     */ { 1.0f,  1.5f,  2.0f,  1.0f,  1.2f },
        /* IRON      */ { 1.5f,  2.0f,  2.5f,  1.2f,  1.5f },
        /* GOLD      */ { 2.0f,  2.5f,  3.5f,  1.8f,  2.0f },
        /* DIAMOND   */ { 1.2f,  1.5f,  2.0f,  1.0f,  1.2f },
        /* NETHERITE */ { 2.0f,  2.5f,  3.5f,  1.8f,  2.0f },
        /* LEATHER   */ { 0.3f,  0.5f,  0.6f,  0.3f,  0.4f },
        /* CHAIN     */ { 1.2f,  1.8f,  2.0f,  1.0f,  1.2f },
        /* TURTLE    */ { 0.8f,  1.0f,  1.2f,  0.6f,  0.8f },
        /* COPPER    */ { 1.6f,  2.2f,  2.8f,  1.4f,  1.6f },
    };

    // ─── 盔甲质量表 [材质][槽位] (kg) ──────────────────────
    //                  头盔   胸甲   腿甲   靴子   身体
    private static final float[][] ARMOR_MASS = {
        /* WOOD      */ {    0,     0,     0,     0,     0 },
        /* STONE     */ {    0,     0,     0,     0,     0 },
        /* IRON      */ { 2.5f,  6.5f,  4.5f,  2.5f,  5.0f },
        /* GOLD      */ { 3.5f, 10.0f,  7.0f,  3.5f,  8.0f },
        /* DIAMOND   */ { 2.0f,  5.5f,  3.5f,  2.0f,  4.5f },
        /* NETHERITE */ { 3.0f,  8.0f,  5.5f,  3.0f,  6.5f },
        /* LEATHER   */ { 1.0f,  3.5f,  2.5f,  1.0f,  3.0f },
        /* CHAIN     */ { 2.5f,  7.0f,  5.0f,  2.0f,  5.5f },
        /* TURTLE    */ { 2.0f,     0,     0,     0,     0 },
        /* COPPER    */ { 2.8f,  7.5f,  5.0f,  2.8f,  6.0f },
    };

    // ─── 事件处理：写入默认质量组件 ─────────────────────────

    @SubscribeEvent
    static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.getAllItems().forEach(item -> {
            float mass = calculateMass(item);
            if (mass > 0) {
                event.modify(item, builder -> builder.set(DataComponents.ITEM_MASS.get(), mass));
            }
        });
    }

    // ─── 运行时读取 ────────────────────────────────────────

    /**
     * 获取物品堆的质量 (含数量)
     *
     * @param stack 物品堆
     * @return 质量 (千克)
     */
    public static float getMass(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        Float mass = stack.get(DataComponents.ITEM_MASS.get());
        return mass != null ? mass * stack.getCount() : DEFAULT_MASS * stack.getCount();
    }

    // ─── 质量计算 ──────────────────────────────────────────

    private static float calculateMass(Item item) {

        String id = item.builtInRegistryHolder().key().location().toString();

        // 盔甲 (按槽位 + 材质)
        if (item instanceof ArmorItem armor) {
            return getArmorMass(armor, id);
        }

        // 工具 (按类型 + 材质)
        if (item instanceof SwordItem)   return getToolMass(id, ToolType.SWORD);
        if (item instanceof PickaxeItem) return getToolMass(id, ToolType.PICKAXE);
        if (item instanceof AxeItem)     return getToolMass(id, ToolType.AXE);
        if (item instanceof ShovelItem)  return getToolMass(id, ToolType.SHOVEL);
        if (item instanceof HoeItem)     return getToolMass(id, ToolType.HOE);

        // 其他武器和工具
        if (item instanceof BowItem)       return 0.5f;
        if (item instanceof CrossbowItem)  return 3.0f;
        if (item instanceof ShieldItem)    return 3.5f;
        if (item instanceof TridentItem)   return 2.0f;
        if (item instanceof MaceItem)      return 3.5f;
        if (item instanceof FishingRodItem) return 0.3f;
        if (item instanceof ShearsItem)    return 0.3f;

        // 方块 (按材质密度)
        if (item instanceof BlockItem) return getBlockMass(id);

        // 食物
        if (item.getDefaultInstance().getFoodProperties(null) != null) return getFoodMass(id);

        // 桶
        if (item instanceof BucketItem) {
            if (containsAny(id, "water", "lava", "milk", "powder_snow")) return 1.5f;
            return 1.0f;
        }

        // 矿物原材料
        if (id.contains("nugget"))                       return 0.05f;
        if (id.contains("ingot"))                        return getIngotMass(id);
        if (id.contains("gem_") || id.contains("_gem"))   return 0.01f;
        if (id.contains("diamond") && !id.contains("block")) return 0.01f;
        if (id.contains("emerald") && !id.contains("block")) return 0.01f;
        if (id.contains("dust"))                          return 0.3f;
        if (id.contains("raw_") && !id.contains("block")) return 0.5f;

        // GregTech 风格的材料形态
        if (id.contains("plate"))  return 0.6f;
        if (id.contains("rod"))    return 0.3f;
        if (id.contains("wire"))   return 0.15f;
        if (id.contains("foil"))   return 0.05f;
        if (id.contains("gear"))   return 1.0f;
        if (id.contains("frame"))  return 2.0f;

        // 箭矢
        if (item instanceof ArrowItem) return 0.025f;

        // 投掷物
        if (item instanceof ThrowablePotionItem || item instanceof SnowballItem) return 0.3f;
        if (item instanceof EggItem || item instanceof EnderpearlItem)          return 0.1f;
        if (item instanceof FireChargeItem)                                    return 0.1f;
        if (item instanceof ExperienceBottleItem)                              return 0.6f;

        // 书籍和纸张
        if (id.contains("book"))     return 0.3f;
        if (id.contains("paper"))    return 0.05f;
        if (id.contains("map"))      return 0.05f;
        if (containsAny(id, "record", "music_disc")) return 0.1f;

        // 杂项工具
        if (containsAny(id, "compass", "clock", "recovery_compass")) return 0.1f;
        if (id.contains("flint_and_steel")) return 0.2f;
        if (id.contains("ender_eye"))       return 0.1f;
        if (id.contains("lead") || id.contains("leash")) return 0.2f;
        if (id.contains("name_tag"))       return 0.01f;
        if (id.contains("saddle"))         return 3.0f;
        if (id.contains("spyglass"))       return 0.3f;
        if (id.contains("elytra"))         return 3.0f;
        if (id.contains("totem"))          return 0.1f;
        if (id.contains("bundle"))         return 0.2f;

        // 载具
        if (containsAny(id, "chest_minecart", "hopper_minecart", "tnt_minecart")) return 15.0f;
        if (id.contains("minecart"))  return 5.0f;
        if (id.contains("boat"))      return 2.0f;

        return DEFAULT_MASS;
    }

    // ─── 材质检测 ──────────────────────────────────────────

    private static Material detectToolMaterial(String id) {
        if (id.contains("wooden"))    return Material.WOOD;
        if (id.contains("stone"))     return Material.STONE;
        if (id.contains("iron"))      return Material.IRON;
        if (id.contains("golden"))    return Material.GOLD;
        if (id.contains("diamond"))   return Material.DIAMOND;
        if (id.contains("netherite")) return Material.NETHERITE;
        if (id.contains("copper"))    return Material.COPPER;
        return Material.STONE;
    }

    private static Material detectArmorMaterial(String id) {
        if (id.contains("leather"))    return Material.LEATHER;
        if (id.contains("chainmail"))  return Material.CHAIN;
        if (id.contains("iron"))       return Material.IRON;
        if (id.contains("golden"))     return Material.GOLD;
        if (id.contains("diamond"))    return Material.DIAMOND;
        if (id.contains("netherite"))  return Material.NETHERITE;
        if (id.contains("turtle"))     return Material.TURTLE;
        return Material.IRON;
    }

    private static float getToolMass(String id, ToolType type) {
        return TOOL_MASS[detectToolMaterial(id).ordinal()][type.ordinal()];
    }

    private static float getArmorMass(ArmorItem armor, String id) {
        Material mat = detectArmorMaterial(id);
        int slot = armor.getType().ordinal();
        float mass = ARMOR_MASS[mat.ordinal()][slot];
        return mass > 0 ? mass : DEFAULT_MASS;
    }

    // ─── 方块质量 ──────────────────────────────────────────

    private static float getBlockMass(String id) {
        // ── 金属完整方块 (密度参考现实) ──
        if (id.contains("iron_block")      || id.contains("block_of_iron"))     return 7.87f;
        if (id.contains("gold_block")      || id.contains("block_of_gold"))     return 19.3f;
        if (id.contains("copper_block")    || id.contains("block_of_copper"))   return 8.96f;
        if (id.contains("diamond_block")   || id.contains("block_of_diamond"))  return 3.51f;
        if (id.contains("emerald_block")   || id.contains("block_of_emerald"))  return 2.76f;
        if (id.contains("netherite_block") || id.contains("block_of_netherite")) return 10.0f;
        if (id.contains("lapis_block")     || id.contains("block_of_lapis"))    return 3.0f;
        if (id.contains("redstone_block")  || id.contains("block_of_redstone")) return 2.5f;
        if (id.contains("amethyst_block")) return 2.65f;
        if (id.contains("coal_block"))     return 1.4f;
        if (containsAny(id, "raw_iron_block", "raw_gold_block", "raw_copper_block")) return 5.0f;

        // ── 金属半成品 (门/栏杆/活板门等) ──
        if (containsAny(id, "iron")   && containsAny(id, "door", "trapdoor", "bars"))   return 4.0f;
        if (containsAny(id, "gold")   && containsAny(id, "door", "trapdoor", "bars"))   return 6.0f;
        if (containsAny(id, "copper") && containsAny(id, "door", "trapdoor", "bars"))   return 4.5f;

        // ── 矿石方块 ──
        if (containsAny(id, "iron_ore", "deepslate_iron_ore"))      return 5.0f;
        if (containsAny(id, "gold_ore", "deepslate_gold_ore"))      return 8.0f;
        if (containsAny(id, "copper_ore", "deepslate_copper_ore"))  return 6.0f;
        if (containsAny(id, "diamond_ore", "deepslate_diamond_ore")) return 3.0f;
        if (containsAny(id, "emerald_ore", "deepslate_emerald_ore")) return 2.5f;
        if (containsAny(id, "lapis_ore", "deepslate_lapis_ore"))     return 2.8f;
        if (containsAny(id, "redstone_ore", "deepslate_redstone_ore")) return 2.5f;
        if (containsAny(id, "coal_ore", "deepslate_coal_ore"))       return 2.0f;
        if (id.contains("_ore") || id.contains("ore_"))              return 3.5f;

        // ── 木材 ──
        if (containsAny(id, "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
            if (containsAny(id, "log", "stem", "hyphae"))  return 0.8f;
            if (id.contains("planks") || id.contains("button")) return 0.5f;
            if (id.contains("slab") || id.contains("stairs"))     return 0.35f;
            if (containsAny(id, "fence", "gate", "door"))         return 0.4f;
            if (id.contains("sign"))          return 0.3f;
            if (id.contains("trapdoor"))      return 0.3f;
            if (id.contains("leaves"))        return 0.1f;
            return 0.6f;
        }
        if (id.contains("bamboo")) return 0.3f;
        if (containsAny(id, "crimson", "warped")) {
            if (containsAny(id, "stem", "hyphae")) return 0.6f;
            if (id.contains("planks"))            return 0.4f;
            return 0.5f;
        }

        // ── 石材 (密度 2.0~3.5) ──
        if (containsAny(id, "obsidian", "crying_obsidian")) return 3.5f;
        if (containsAny(id, "stone", "cobblestone", "andesite", "diorite", "granite")) return 2.5f;
        if (id.contains("deepslate"))   return 3.0f;
        if (containsAny(id, "basalt", "blackstone", "magma_block")) return 2.8f;
        if (containsAny(id, "tuff", "calcite")) return 2.3f;
        if (id.contains("bedrock"))     return 5.0f;
        if (id.contains("end_stone"))   return 2.6f;
        if (id.contains("purpur"))      return 2.2f;
        if (containsAny(id, "sandstone", "red_sandstone")) return 2.2f;

        // ── 砖类 ──
        if (containsAny(id, "bricks") && !id.contains("nether")) return 2.0f;
        if (id.contains("nether_brick")) return 2.0f;
        if (id.contains("prismarine"))   return 2.3f;

        // ── 羊毛和织物 ──
        if (id.contains("wool"))     return 0.3f;
        if (id.contains("carpet"))   return 0.15f;
        if (id.contains("banner"))   return 0.5f;
        if (id.contains("bed"))      return 2.0f;

        // ── 土壤和自然 ──
        if (containsAny(id, "dirt", "podzol", "farmland", "rooted_dirt", "grass_block", "moss_block", "mycelium")) return 1.5f;
        if (containsAny(id, "sand", "suspicious_sand")) return 1.6f;
        if (id.contains("gravel"))              return 1.8f;
        if (id.contains("clay"))                return 1.4f;
        if (containsAny(id, "mud", "muddy"))    return 1.8f;
        if (containsAny(id, "soul_sand", "soul_soil")) return 1.6f;
        if (id.contains("snow") || id.contains("powder_snow")) return 0.3f;
        if (containsAny(id, "ice", "packed_ice", "blue_ice", "frosted_ice")) return 0.9f;
        if (id.contains("sculk"))               return 1.5f;

        // ── 玻璃和光源 ──
        if (id.contains("glass"))                                   return 2.5f;
        if (containsAny(id, "glowstone", "shroomlight"))            return 1.5f;
        if (id.contains("sea_lantern"))                              return 2.0f;
        if (id.contains("lantern"))                                  return 1.5f;
        if (containsAny(id, "torch", "soul_torch"))                  return 0.1f;
        if (containsAny(id, "campfire", "soul_campfire"))            return 1.0f;
        if (containsAny(id, "rail", "powered_rail", "detector_rail", "activator_rail")) return 1.0f;

        // ── 植物 ──
        if (id.contains("hay_block") || id.contains("hay_bale")) return 0.4f;
        if (id.contains("bone_block"))    return 1.8f;
        if (id.contains("sponge"))        return 0.2f;
        if (containsAny(id, "log", "stem") && !id.contains("mushroom")) return 0.8f;

        // ── 功能方块 ──
        if (id.contains("tnt"))            return 0.6f;
        if (id.contains("bookshelf"))      return 1.5f;
        if (id.contains("crafting_table")) return 2.0f;
        if (containsAny(id, "furnace", "blast_furnace", "smoker")) return 3.0f;
        if (containsAny(id, "chest", "trapped_chest", "ender_chest")) return 3.0f;
        if (id.contains("hopper"))         return 2.5f;
        if (containsAny(id, "anvil", "chipped_anvil", "damaged_anvil")) return 5.0f;
        if (id.contains("cauldron"))       return 2.5f;
        if (id.contains("brewing_stand"))  return 1.5f;
        if (id.contains("enchanting_table")) return 5.0f;
        if (containsAny(id, "spawner", "dragon_egg", "conduit", "beacon", "respawn_anchor")) return 5.0f;

        return 2.5f;
    }

    // ─── 锭质量 (按金属密度) ────────────────────────────────

    private static float getIngotMass(String id) {
        // 轻金属
        if (id.contains("aluminum") || id.contains("aluminium")) return 0.15f;
        if (id.contains("magnesium"))  return 0.1f;
        if (id.contains("titanium"))   return 0.3f;

        // 中等金属
        if (id.contains("tin"))        return 0.4f;
        if (id.contains("zinc"))       return 0.4f;
        if (id.contains("nickel"))     return 0.5f;

        // 常见金属
        if (id.contains("iron"))       return 0.5f;
        if (id.contains("copper"))     return 0.5f;
        if (id.contains("steel"))      return 0.5f;

        // 合金
        if (id.contains("brass"))      return 0.5f;
        if (id.contains("bronze"))     return 0.6f;
        if (id.contains("invar"))      return 0.55f;
        if (id.contains("electrum"))   return 0.8f;
        if (id.contains("stainless_steel")) return 0.5f;

        // 重金属
        if (id.contains("silver"))     return 0.5f;
        if (id.contains("lead"))       return 0.7f;
        if (id.contains("tungsten") || id.contains("wolfram")) return 0.6f;
        if (id.contains("gold"))       return 1.0f;
        if (id.contains("platinum"))   return 1.0f;
        if (id.contains("uranium"))    return 1.0f;
        if (id.contains("netherite"))  return 2.0f;

        return 0.5f;
    }

    // ─── 食物质量 ──────────────────────────────────────────

    private static float getFoodMass(String id) {
        if (containsAny(id, "apple", "golden_apple", "enchanted_golden_apple")) return 0.2f;
        if (id.contains("bread"))                                           return 0.4f;
        if (containsAny(id, "steak", "porkchop", "mutton", "horse"))        return 0.5f;
        if (containsAny(id, "chicken", "rabbit"))                            return 0.3f;
        if (containsAny(id, "cod", "salmon", "tropical_fish", "pufferfish")) return 0.3f;
        if (containsAny(id, "cookie", "dried_kelp"))                         return 0.1f;
        if (id.contains("melon") || id.contains("pumpkin_pie"))              return 0.8f;
        if (id.contains("cake"))                                             return 1.5f;
        if (containsAny(id, "stew", "soup", "beetroot_soup", "suspicious_stew",
                "mushroom_stew", "rabbit_stew"))                              return 0.6f;
        if (containsAny(id, "carrot", "potato", "baked_potato", "beetroot")) return 0.15f;
        if (containsAny(id, "sweet_berries", "glow_berries"))                return 0.05f;
        if (id.contains("chorus"))       return 0.1f;
        if (id.contains("rotten_flesh")) return 0.4f;
        if (id.contains("spider_eye"))   return 0.05f;

        return 0.3f;
    }

    // ─── 工具方法 ──────────────────────────────────────────

    private static boolean containsAny(String str, String... keywords) {
        for (String kw : keywords) {
            if (str.contains(kw)) return true;
        }
        return false;
    }
}
