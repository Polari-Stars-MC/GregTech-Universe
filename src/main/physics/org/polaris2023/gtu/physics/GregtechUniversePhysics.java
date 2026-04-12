package org.polaris2023.gtu.physics;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsRigidBody;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.polaris2023.gtu.physics.config.PhysicsConfig;
import org.polaris2023.gtu.physics.init.DataAttachments;
import org.polaris2023.gtu.physics.init.Physics;
import org.polaris2023.gtu.physics.init.DataComponents;

import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(GregtechUniversePhysics.MOD_ID)
public class GregtechUniversePhysics {
    public static final String MOD_ID = "gtu_physics";

    public GregtechUniversePhysics(IEventBus modEventBus, ModContainer modContainer) {

        // 注册数据组件
        DataComponents.init(modEventBus);
        DataAttachments.register(modEventBus);

        // TODO: Register registries here
        Physics.init();

        // 禁用 Libbulletjme 的 INFO 日志
        disableBulletLogging();

        // 注册配置
        modContainer.registerConfig(ModConfig.Type.SERVER, PhysicsConfig.SPEC);

    }

    /**
     * 禁用 Libbulletjme 库的 INFO 级别日志
     */
    private static void disableBulletLogging() {
        PhysicsRigidBody.logger2.setLevel(Level.OFF);
        PhysicsSpace.logger.setLevel(Level.OFF);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
