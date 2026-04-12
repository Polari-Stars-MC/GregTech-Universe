package org.polaris2023.gtu.physics.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.polaris2023.gtu.physics.config.PhysicsConfig;
import org.polaris2023.gtu.physics.world.DimensionPhysics;
import twilightforest.init.TFDimension;

/**
 * 暮色森林物理联动
 * <p>
 * 为暮色森林维度添加独特的物理配置
 */
public class TwilightForestPhysics {

    /**
     * 暮色森林模组 ID
     */
    public static final String MOD_ID = "twilightforest";

    /**
     * 获取暮色森林物理配置
     * <p>
     * 暮色森林是一个魔法森林维度，重力略低（神秘的浮空特性）
     * 空气阻力略高（森林中茂密的植被）
     *
     * @return 从配置文件读取的暮色森林物理配置
     */
    public static DimensionPhysics getTwilightForestPhysics(ResourceKey<Level> dim) {
        return new DimensionPhysics(
                dim.location().getPath(),
                PhysicsConfig.getTwilightGravity(),
                PhysicsConfig.getTwilightAirResistance(),
                PhysicsConfig.getTwilightTerminalVelocity(),
                PhysicsConfig.getTwilightSafeFallHeight()
        );
    }

    /**
     * 检查暮色森林模组是否已加载
     *
     * @return true 如果暮色森林模组已加载
     */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * 检查给定维度是否为暮色森林维度
     */
    public static boolean isTwilightForest(ResourceKey<Level> dim) {
        return dim.equals(TFDimension.DIMENSION_KEY);
    }
}
