package org.polaris2023.gtu.physics.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

/**
 * 物理模块兼容性管理器
 * <p>
 * 管理所有第三方模组的物理联动
 */
public class PhysicsCompatManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("GTU Physics Compat");

    /**
     * 初始化所有兼容性模块
     * <p>
     * 在配置加载后调用，标记已加载的模组
     */
    public static void init() {
        if (TwilightForestPhysics.isLoaded()) {
            LOGGER.info("Twilight Forest is Loaded");
        }
        if (AetherPhysics.isLoaded()) {
            LOGGER.info("Aether is Loaded");
        }
        if (EternalStarLightPhysics.isLoaded()) {
            LOGGER.info("Eternal StarLight is Loaded");
        }
        if (AE2SpatialPhysics.isLoaded()) {
            LOGGER.info("AE2 Spatial Storage is Loaded");
        }
    }

    /**
     * 根据维度 key 返回兼容模组的默认物理配置
     *
     * @param dim 维度 ResourceKey
     * @return 对应模组维度的物理配置，如果不匹配则返回 null
     */
    public static DimensionPhysics getDefaultForDimension(ResourceKey<Level> dim) {
        if (TwilightForestPhysics.isLoaded() && TwilightForestPhysics.isTwilightForest(dim)) {
            return TwilightForestPhysics.getTwilightForestPhysics(dim);
        }
        if (AetherPhysics.isLoaded() && AetherPhysics.isAether(dim)) {
            return AetherPhysics.getAetherPhysics();
        }
        if (EternalStarLightPhysics.isLoaded() && EternalStarLightPhysics.isStarLight(dim)) {
            return EternalStarLightPhysics.getStarLightPhysics(dim);
        }
        if (AE2SpatialPhysics.isLoaded() && AE2SpatialPhysics.isSpatialStorage(dim)) {
            return AE2SpatialPhysics.getSpatialPhysics(dim);
        }
        return null;
    }
}
