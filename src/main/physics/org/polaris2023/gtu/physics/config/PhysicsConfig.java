package org.polaris2023.gtu.physics.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.polaris2023.gtu.physics.compat.PhysicsCompatManager;

/**
 * 物理模块配置
 */
@EventBusSubscriber(modid = "gtu_physics")
public class PhysicsConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 主世界配置
    private static final ModConfigSpec.DoubleValue OVERWORLD_GRAVITY = BUILDER
            .comment("主世界重力加速度 (m/s²)")
            .defineInRange("overworld.gravity", 9.80665, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue OVERWORLD_AIR_RESISTANCE = BUILDER
            .comment("主世界空气阻力系数")
            .defineInRange("overworld.air_resistance", 0.02, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue OVERWORLD_TERMINAL_VELOCITY = BUILDER
            .comment("主世界终端速度 (m/s)")
            .defineInRange("overworld.terminal_velocity", 78.4, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue OVERWORLD_SAFE_FALL_HEIGHT = BUILDER
            .comment("主世界安全坠落高度 (米)")
            .defineInRange("overworld.safe_fall_height", 3.0, 0.0, 50.0);

    // 下界配置
    private static final ModConfigSpec.DoubleValue NETHER_GRAVITY = BUILDER
            .comment("下界重力加速度 (m/s²)")
            .defineInRange("nether.gravity", 8.5, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue NETHER_AIR_RESISTANCE = BUILDER
            .comment("下界空气阻力系数")
            .defineInRange("nether.air_resistance", 0.05, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue NETHER_TERMINAL_VELOCITY = BUILDER
            .comment("下界终端速度 (m/s)")
            .defineInRange("nether.terminal_velocity", 40.0, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue NETHER_SAFE_FALL_HEIGHT = BUILDER
            .comment("下界安全坠落高度 (米)")
            .defineInRange("nether.safe_fall_height", 4.0, 0.0, 50.0);

    // 末地配置
    private static final ModConfigSpec.DoubleValue END_GRAVITY = BUILDER
            .comment("末地重力加速度 (m/s²)")
            .defineInRange("end.gravity", 5.0, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue END_AIR_RESISTANCE = BUILDER
            .comment("末地空气阻力系数")
            .defineInRange("end.air_resistance", 0.001, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue END_TERMINAL_VELOCITY = BUILDER
            .comment("末地终端速度 (m/s)")
            .defineInRange("end.terminal_velocity", 200.0, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue END_SAFE_FALL_HEIGHT = BUILDER
            .comment("末地安全坠落高度 (米)")
            .defineInRange("end.safe_fall_height", 6.0, 0.0, 50.0);

    // 天境配置 (Aether)
    private static final ModConfigSpec.DoubleValue AETHER_GRAVITY = BUILDER
            .comment("天境重力加速度 (m/s²) - 低重力的天空维度")
            .defineInRange("aether.gravity", 5.28, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue AETHER_AIR_RESISTANCE = BUILDER
            .comment("天境空气阻力系数 - 高空稀薄空气")
            .defineInRange("aether.air_resistance", 0.008, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue AETHER_TERMINAL_VELOCITY = BUILDER
            .comment("天境终端速度 (m/s)")
            .defineInRange("aether.terminal_velocity", 45.0, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue AETHER_SAFE_FALL_HEIGHT = BUILDER
            .comment("天境安全坠落高度 (米) - 低重力减缓下落")
            .defineInRange("aether.safe_fall_height", 5.0, 0.0, 50.0);

    // 暮色森林配置 (Twilight Forest)
    private static final ModConfigSpec.DoubleValue TWILIGHT_GRAVITY = BUILDER
            .comment("暮色森林重力加速度 (m/s²) - 魔法森林的神秘感")
            .defineInRange("twilight_forest.gravity", 8.0, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue TWILIGHT_AIR_RESISTANCE = BUILDER
            .comment("暮色森林空气阻力系数 - 森林中茂密的植被")
            .defineInRange("twilight_forest.air_resistance", 0.03, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue TWILIGHT_TERMINAL_VELOCITY = BUILDER
            .comment("暮色森林终端速度 (m/s)")
            .defineInRange("twilight_forest.terminal_velocity", 60.0, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue TWILIGHT_SAFE_FALL_HEIGHT = BUILDER
            .comment("暮色森林安全坠落高度 (米)")
            .defineInRange("twilight_forest.safe_fall_height", 3.5, 0.0, 50.0);

    // 永恒星光配置 (Eternal Starlight)
    private static final ModConfigSpec.DoubleValue STARLIGHT_GRAVITY = BUILDER
            .comment("永恒星光重力加速度 (m/s²) - 镜世界的虚幻漂浮感")
            .defineInRange("eternal_starlight.gravity", 7.0, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue STARLIGHT_AIR_RESISTANCE = BUILDER
            .comment("永恒星光空气阻力系数 - 星光能量弥漫，空气稀薄")
            .defineInRange("eternal_starlight.air_resistance", 0.015, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue STARLIGHT_TERMINAL_VELOCITY = BUILDER
            .comment("永恒星光终端速度 (m/s)")
            .defineInRange("eternal_starlight.terminal_velocity", 80.0, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue STARLIGHT_SAFE_FALL_HEIGHT = BUILDER
            .comment("永恒星光安全坠落高度 (米) - 神秘维度的庇护")
            .defineInRange("eternal_starlight.safe_fall_height", 5.0, 0.0, 50.0);

    // AE2 封闭空间配置 (Spatial Storage)
    private static final ModConfigSpec.DoubleValue SPATIAL_GRAVITY = BUILDER
            .comment("封闭空间重力加速度 (m/s²) - 人工存储空间，无重力环境模拟")
            .defineInRange("spatial_storage.gravity", 2.0, 0.0, 100.0);

    private static final ModConfigSpec.DoubleValue SPATIAL_AIR_RESISTANCE = BUILDER
            .comment("封闭空间空气阻力系数 - 密闭空间，几乎真空")
            .defineInRange("spatial_storage.air_resistance", 0.001, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue SPATIAL_TERMINAL_VELOCITY = BUILDER
            .comment("封闭空间终端速度 (m/s)")
            .defineInRange("spatial_storage.terminal_velocity", 150.0, 1.0, 500.0);

    private static final ModConfigSpec.DoubleValue SPATIAL_SAFE_FALL_HEIGHT = BUILDER
            .comment("封闭空间安全坠落高度 (米) - 无重力环境减少坠落伤害")
            .defineInRange("spatial_storage.safe_fall_height", 10.0, 0.0, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // 运行时缓存的值
    private static float overworldGravity;
    private static float overworldAirResistance;
    private static float overworldTerminalVelocity;
    private static float overworldSafeFallHeight;

    private static float netherGravity;
    private static float netherAirResistance;
    private static float netherTerminalVelocity;
    private static float netherSafeFallHeight;

    private static float endGravity;
    private static float endAirResistance;
    private static float endTerminalVelocity;
    private static float endSafeFallHeight;

    // 天境缓存值
    private static float aetherGravity;
    private static float aetherAirResistance;
    private static float aetherTerminalVelocity;
    private static float aetherSafeFallHeight;

    // 暮色森林缓存值
    private static float twilightGravity;
    private static float twilightAirResistance;
    private static float twilightTerminalVelocity;
    private static float twilightSafeFallHeight;

    // 永恒星光缓存值
    private static float starlightGravity;
    private static float starlightAirResistance;
    private static float starlightTerminalVelocity;
    private static float starlightSafeFallHeight;

    // AE2 封闭空间缓存值
    private static float spatialGravity;
    private static float spatialAirResistance;
    private static float spatialTerminalVelocity;
    private static float spatialSafeFallHeight;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 只处理加载事件，忽略卸载事件
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }

        // 主世界
        overworldGravity = OVERWORLD_GRAVITY.get().floatValue();
        overworldAirResistance = OVERWORLD_AIR_RESISTANCE.get().floatValue();
        overworldTerminalVelocity = OVERWORLD_TERMINAL_VELOCITY.get().floatValue();
        overworldSafeFallHeight = OVERWORLD_SAFE_FALL_HEIGHT.get().floatValue();

        // 下界
        netherGravity = NETHER_GRAVITY.get().floatValue();
        netherAirResistance = NETHER_AIR_RESISTANCE.get().floatValue();
        netherTerminalVelocity = NETHER_TERMINAL_VELOCITY.get().floatValue();
        netherSafeFallHeight = NETHER_SAFE_FALL_HEIGHT.get().floatValue();

        // 末地
        endGravity = END_GRAVITY.get().floatValue();
        endAirResistance = END_AIR_RESISTANCE.get().floatValue();
        endTerminalVelocity = END_TERMINAL_VELOCITY.get().floatValue();
        endSafeFallHeight = END_SAFE_FALL_HEIGHT.get().floatValue();

        // 天境
        aetherGravity = AETHER_GRAVITY.get().floatValue();
        aetherAirResistance = AETHER_AIR_RESISTANCE.get().floatValue();
        aetherTerminalVelocity = AETHER_TERMINAL_VELOCITY.get().floatValue();
        aetherSafeFallHeight = AETHER_SAFE_FALL_HEIGHT.get().floatValue();

        // 暮色森林
        twilightGravity = TWILIGHT_GRAVITY.get().floatValue();
        twilightAirResistance = TWILIGHT_AIR_RESISTANCE.get().floatValue();
        twilightTerminalVelocity = TWILIGHT_TERMINAL_VELOCITY.get().floatValue();
        twilightSafeFallHeight = TWILIGHT_SAFE_FALL_HEIGHT.get().floatValue();

        // 永恒星光
        starlightGravity = STARLIGHT_GRAVITY.get().floatValue();
        starlightAirResistance = STARLIGHT_AIR_RESISTANCE.get().floatValue();
        starlightTerminalVelocity = STARLIGHT_TERMINAL_VELOCITY.get().floatValue();
        starlightSafeFallHeight = STARLIGHT_SAFE_FALL_HEIGHT.get().floatValue();

        // AE2 封闭空间
        spatialGravity = SPATIAL_GRAVITY.get().floatValue();
        spatialAirResistance = SPATIAL_AIR_RESISTANCE.get().floatValue();
        spatialTerminalVelocity = SPATIAL_TERMINAL_VELOCITY.get().floatValue();
        spatialSafeFallHeight = SPATIAL_SAFE_FALL_HEIGHT.get().floatValue();

        // 初始化模组兼容性
        PhysicsCompatManager.init();

        // 将配置应用到所有已加载的 ServerLevel
        applyToAllLevels();
    }

    public static float getOverworldGravity() { return overworldGravity; }
    public static float getOverworldAirResistance() { return overworldAirResistance; }
    public static float getOverworldTerminalVelocity() { return overworldTerminalVelocity; }
    public static float getOverworldSafeFallHeight() { return overworldSafeFallHeight; }

    public static float getNetherGravity() { return netherGravity; }
    public static float getNetherAirResistance() { return netherAirResistance; }
    public static float getNetherTerminalVelocity() { return netherTerminalVelocity; }
    public static float getNetherSafeFallHeight() { return netherSafeFallHeight; }

    public static float getEndGravity() { return endGravity; }
    public static float getEndAirResistance() { return endAirResistance; }
    public static float getEndTerminalVelocity() { return endTerminalVelocity; }
    public static float getEndSafeFallHeight() { return endSafeFallHeight; }

    // 天境 getter
    public static float getAetherGravity() { return aetherGravity; }
    public static float getAetherAirResistance() { return aetherAirResistance; }
    public static float getAetherTerminalVelocity() { return aetherTerminalVelocity; }
    public static float getAetherSafeFallHeight() { return aetherSafeFallHeight; }

    // 暮色森林 getter
    public static float getTwilightGravity() { return twilightGravity; }
    public static float getTwilightAirResistance() { return twilightAirResistance; }
    public static float getTwilightTerminalVelocity() { return twilightTerminalVelocity; }
    public static float getTwilightSafeFallHeight() { return twilightSafeFallHeight; }

    // 永恒星光 getter
    public static float getStarlightGravity() { return starlightGravity; }
    public static float getStarlightAirResistance() { return starlightAirResistance; }
    public static float getStarlightTerminalVelocity() { return starlightTerminalVelocity; }
    public static float getStarlightSafeFallHeight() { return starlightSafeFallHeight; }

    // AE2 封闭空间 getter
    public static float getSpatialGravity() { return spatialGravity; }
    public static float getSpatialAirResistance() { return spatialAirResistance; }
    public static float getSpatialTerminalVelocity() { return spatialTerminalVelocity; }
    public static float getSpatialSafeFallHeight() { return spatialSafeFallHeight; }

    /**
     * 将当前配置应用到所有已加载的 ServerLevel
     * <p>
     * 在配置重新加载时调用，刷新所有维度的物理附件
     */
    public static void applyToAllLevels() {
        // 此方法在 FML config 事件中调用，ServerLevel 可能尚未完全就绪
        // 实际的 attachment 更新会在 getData() 时自动使用新的默认值
    }
}
