package org.polaris2023.gtu.physics.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;

/**
 * 物理模块数据组件和附件类型注册
 */
public class DataComponents {

    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GregtechUniversePhysics.MOD_ID);



    /**
     * 物品质量组件 (千克)
     * <p>
     * 存储物品的质量值，用于负重计算
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ITEM_MASS =
            REGISTER.registerComponentType(
                    "item_mass",
                    builder -> builder
                            .persistent(ExtraCodecs.POSITIVE_FLOAT)
                            .networkSynchronized(ByteBufCodecs.FLOAT)
            );

    public static void init(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
