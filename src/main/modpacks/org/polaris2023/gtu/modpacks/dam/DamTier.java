package org.polaris2023.gtu.modpacks.dam;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Supplier;

/**
 * 水坝技术等级定义。
 * <p>
 * 从原始人 (Primitive) 到 UXV，共14个等级。
 * 每个等级对应一种应力输出仓的机壳材质、基础应力输出值和显示颜色。
 * </p>
 *
 * <h3>应力计算公式</h3>
 * <pre>
 *   baseStress(tier)  = 32 × 4^tier
 *   flowMultiplier    = clamp(riverFlowSpeed × 2.5, 0.1, 3.0)
 *   damStress         = baseStress × (1 + connectedDamCount × 0.8) × flowMultiplier
 * </pre>
 */
public enum DamTier {
    PRIMITIVE("primitive", "原始人", 0, () -> Blocks.BRICKS, ChatFormatting.GRAY),
    ULV("ulv", "ULV", 1, DamTier::gtCasingULV, ChatFormatting.DARK_GRAY),
    LV("lv", "LV", 2, DamTier::gtCasingLV, ChatFormatting.WHITE),
    MV("mv", "MV", 3, DamTier::gtCasingMV, ChatFormatting.GOLD),
    HV("hv", "HV", 4, DamTier::gtCasingHV, ChatFormatting.YELLOW),
    EV("ev", "EV", 5, DamTier::gtCasingEV, ChatFormatting.DARK_PURPLE),
    IV("iv", "IV", 6, DamTier::gtCasingIV, ChatFormatting.DARK_BLUE),
    LuV("luv", "LuV", 7, DamTier::gtCasingLuV, ChatFormatting.LIGHT_PURPLE),
    ZPM("zpm", "ZPM", 8, DamTier::gtCasingZPM, ChatFormatting.RED),
    UV("uv", "UV", 9, DamTier::gtCasingUV, ChatFormatting.DARK_GREEN),
    UHV("uhv", "UHV", 10, DamTier::gtCasingUHV, ChatFormatting.DARK_RED),
    UEV("uev", "UEV", 11, DamTier::gtCasingUEV, ChatFormatting.GREEN),
    UIV("uiv", "UIV", 12, DamTier::gtCasingUIV, ChatFormatting.DARK_AQUA),
    UXV("uxv", "UXV", 13, DamTier::gtCasingUXV, ChatFormatting.AQUA);

    private final String name;
    private final String displayName;
    private final int index;
    private final Supplier<Block> casingBlock;
    private final ChatFormatting color;

    DamTier(String name, String displayName, int index, Supplier<Block> casingBlock, ChatFormatting color) {
        this.name = name;
        this.displayName = displayName;
        this.index = index;
        this.casingBlock = casingBlock;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getIndex() {
        return index;
    }

    public Block getCasingBlock() {
        return casingBlock.get();
    }

    public ChatFormatting getColor() {
        return color;
    }

    /**
     * 基础应力输出值 = 32 × 4^tier
     */
    public long getBaseStress() {
        return 32L * (long) Math.pow(4, index);
    }

    public double getFlowFactor(double riverFlowSpeed) {
        return Mth.clamp(riverFlowSpeed * 2.5, 0.1, 3.0);
    }

    public double getStackFactor(int segmentCount) {
        return 1.0 + Math.max(0, segmentCount - 1) * 0.8;
    }

    public double getBaseRpm() {
        return 8.0 + index * 2.5;
    }

    public double calculateRpm(double riverFlowSpeed) {
        return getBaseRpm() * getFlowFactor(riverFlowSpeed);
    }

    /**
     * 计算实际应力输出。
     *
     * @param connectedDamCount 连接的水坝数量（不含当前控制器自身的）
     * @param riverFlowSpeed    河流流速 (0.0 ~ 1.0+)
     * @return 最终应力输出 (SU)
     */
    public double calculateStress(int connectedDamCount, double riverFlowSpeed) {
        double baseStress = getBaseStress();
        double stackMultiplier = 1.0 + connectedDamCount * 0.8;
        double flowMultiplier = getFlowFactor(riverFlowSpeed);
        return baseStress * stackMultiplier * flowMultiplier;
    }

    public double calculateTotalStress(int segmentCount, double riverFlowSpeed) {
        return getBaseStress() * getStackFactor(segmentCount) * getFlowFactor(riverFlowSpeed);
    }

    public double calculateSegmentStress(int segmentCount, double riverFlowSpeed) {
        return calculateTotalStress(segmentCount, riverFlowSpeed) / Math.max(1, segmentCount);
    }

    /**
     * 获取对应等级的显示名组件（带颜色）。
     */
    public MutableComponent getDisplayComponent() {
        return Component.literal(displayName).withStyle(color);
    }

    /**
     * 根据索引获取等级。
     */
    public static DamTier byIndex(int index) {
        DamTier[] values = values();
        if (index < 0 || index >= values.length) {
            return PRIMITIVE;
        }
        return values[index];
    }

    /**
     * 根据机壳方块查找对应等级。
     */
    public static DamTier fromCasingBlock(Block block) {
        for (DamTier tier : values()) {
            if (tier.getCasingBlock() == block) {
                return tier;
            }
        }
        return PRIMITIVE;
    }

    // ---- GTCEu Machine Casing suppliers ----
    // 使用延迟加载避免类加载顺序问题

    private static Block gtCasingULV() {
        return getGTBlock("machine_casing_ulv");
    }

    private static Block gtCasingLV() {
        return getGTBlock("machine_casing_lv");
    }

    private static Block gtCasingMV() {
        return getGTBlock("machine_casing_mv");
    }

    private static Block gtCasingHV() {
        return getGTBlock("machine_casing_hv");
    }

    private static Block gtCasingEV() {
        return getGTBlock("machine_casing_ev");
    }

    private static Block gtCasingIV() {
        return getGTBlock("machine_casing_iv");
    }

    private static Block gtCasingLuV() {
        return getGTBlock("machine_casing_luv");
    }

    private static Block gtCasingZPM() {
        return getGTBlock("machine_casing_zpm");
    }

    private static Block gtCasingUV() {
        return getGTBlock("machine_casing_uv");
    }

    private static Block gtCasingUHV() {
        return getGTBlock("machine_casing_uhv");
    }

    private static Block gtCasingUEV() {
        return getGTBlock("machine_casing_uev");
    }

    private static Block gtCasingUIV() {
        return getGTBlock("machine_casing_uiv");
    }

    private static Block gtCasingUXV() {
        return getGTBlock("machine_casing_uxv");
    }

    private static Block getGTBlock(String name) {
        var rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gtceu", name);
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(rl);
    }
}
