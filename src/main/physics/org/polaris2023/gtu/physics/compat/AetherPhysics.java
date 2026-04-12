package org.polaris2023.gtu.physics.compat;

import com.aetherteam.aether.data.resources.registries.AetherDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.polaris2023.gtu.physics.config.PhysicsConfig;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

/**
 * 天境物理联动
 * <p>
 * 为天境维度添加独特的物理配置
 */
public class AetherPhysics {

    /**
     * 天境模组 ID
     */
    public static final String MOD_ID = "aether";

    /**
     * 获取天境物理配置
     * <p>
     * 天境是一个天空维度，重力较低（浮空岛屿的特性）
     * 空气阻力较低（高空稀薄空气）
     * 终端速度较低（低重力环境）
     *
     * @return 从配置文件读取的天境物理配置
     */
    public static DimensionPhysics getAetherPhysics() {
        return new DimensionPhysics(
                "Aether",
                PhysicsConfig.getAetherGravity(),
                PhysicsConfig.getAetherAirResistance(),
                PhysicsConfig.getAetherTerminalVelocity(),
                PhysicsConfig.getAetherSafeFallHeight()
        );
    }

    /**
     * 检查天境模组是否已加载
     *
     * @return true 如果天境模组已加载
     */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * 检查给定维度是否为天境维度
     */
    public static boolean isAether(ResourceKey<Level> dim) {
        return dim.equals(AetherDimensions.AETHER_LEVEL);
    }
}
