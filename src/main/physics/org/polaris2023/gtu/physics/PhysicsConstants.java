package org.polaris2023.gtu.physics;

/**
 * 物理常数定义
 * <p>
 * Minecraft 单位转换:
 * - 1 方块 = 1 米
 * - 1 tick = 0.05 秒 (20 ticks = 1 秒)
 * <p>
 * 现实重力加速度: g = 9.80665 m/s²
 */
public final class PhysicsConstants {

    /**
     * 每秒 tick 数
     */
    public static final int TICKS_PER_SECOND = 20;

    /**
     * 每tick的秒数
     */
    public static final float SECONDS_PER_TICK = 1.0f / TICKS_PER_SECOND;

    /**
     * 现实重力加速度 (m/s²)
     */
    public static final float REAL_GRAVITY = 9.80665f;

    /**
     * 千克每单位质量 (用于实体质量计算)
     */
    public static final float KG_PER_MASS_UNIT = 100.0f;

    private PhysicsConstants() {
    }
}
