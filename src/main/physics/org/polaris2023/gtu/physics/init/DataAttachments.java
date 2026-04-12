package org.polaris2023.gtu.physics.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.physics.GregtechUniversePhysics;
import org.polaris2023.gtu.physics.world.DimensionPhysics;

public class DataAttachments {
    /**
     * 维度物理配置附件类型
     * <p>
     * 作为 Level 的数据附件，存储每个维度的物理参数
     * 自动根据维度 key 生成默认值，持久化到存档
     */
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GregtechUniversePhysics.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DimensionPhysics>> DIMENSION_PHYSICS =
            REGISTER.register("dimension_physics", () ->
                    AttachmentType.builder(DimensionPhysics::getDefaultFor)
                            .serialize(DimensionPhysics.SERIALIZER)
                            .build()
            );

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
