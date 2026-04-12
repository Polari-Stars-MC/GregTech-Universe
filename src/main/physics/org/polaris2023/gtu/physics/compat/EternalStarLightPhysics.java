package org.polaris2023.gtu.physics.compat;

import cn.leolezury.eternalstarlight.common.data.ESDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.polaris2023.gtu.physics.config.PhysicsConfig;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

public class EternalStarLightPhysics {

    public static final String MOD_ID = "eternal_starlight";

    /**
     * 获取永恒星光物理配置
     * <p>
     * 永恒星光是一个神秘的镜世界维度，充满星光与晶体
     * - 较低的重力：镜世界的虚幻漂浮感，便于在高耸的晶体结构间移动
     * - 较低的空气阻力：星光能量弥漫，空气稀薄而通透
     * - 较高的终端速度：低阻力环境下自由落体更快
     * - 较高的安全坠落高度：神秘维度的庇护，减少坠落伤害
     *
     * @return 从配置文件读取的永恒星光物理配置
     */
    public static DimensionPhysics getStarLightPhysics(ResourceKey<Level> dim) {
        return new DimensionPhysics(
                dim.location().getPath(),
                PhysicsConfig.getStarlightGravity(),
                PhysicsConfig.getStarlightAirResistance(),
                PhysicsConfig.getStarlightTerminalVelocity(),
                PhysicsConfig.getStarlightSafeFallHeight()
        );
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isStarLight(ResourceKey<Level> dim) {
        return dim.equals(ESDimensions.STARLIGHT_KEY);
    }
}
