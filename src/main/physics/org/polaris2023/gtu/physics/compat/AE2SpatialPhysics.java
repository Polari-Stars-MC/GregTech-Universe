package org.polaris2023.gtu.physics.compat;

import appeng.spatial.SpatialStorageDimensionIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.polaris2023.gtu.physics.config.PhysicsConfig;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

/**
 * AE2 封闭空间物理联动
 * <p>
 * 为 AE2 的空间存储维度添加独特的物理配置
 */
public class AE2SpatialPhysics {

    /**
     * AE2 模组 ID
     */
    public static final String MOD_ID = "ae2";

    /**
     * 获取封闭空间物理配置
     * <p>
     * 封闭空间是一个人工创造的存储维度：
     * - 较低的重力：无重力环境的模拟，物体漂浮感
     * - 极低的空气阻力：密闭空间，几乎真空
     * - 较高的终端速度：低阻力环境
     * - 较高的安全坠落高度：无重力环境减少坠落伤害
     *
     * @return 从配置文件读取的封闭空间物理配置
     */
    public static DimensionPhysics getSpatialPhysics(ResourceKey<Level> dim) {
        return new DimensionPhysics(
                dim.location().getPath(),
                PhysicsConfig.getSpatialGravity(),
                PhysicsConfig.getSpatialAirResistance(),
                PhysicsConfig.getSpatialTerminalVelocity(),
                PhysicsConfig.getSpatialSafeFallHeight()
        );
    }

    /**
     * 检查 AE2 模组是否已加载
     *
     * @return true 如果 AE2 模组已加载
     */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * 检查给定维度是否为封闭空间维度
     */
    public static boolean isSpatialStorage(ResourceKey<Level> dim) {
        return dim.equals(SpatialStorageDimensionIds.WORLD_ID);
    }
}
