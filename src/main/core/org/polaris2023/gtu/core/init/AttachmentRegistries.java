package org.polaris2023.gtu.core.init;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberRef;

public class AttachmentRegistries {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GregtechUniverseCore.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> STAGE_LEVEL = REGISTER.register(
            "stage_level",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.INT)
                    .build()
    );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PLACE = REGISTER.register(
            "place",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<StructureMemberRef>> STRUCTURE_MEMBER =
            REGISTER.register("structure_member", () -> AttachmentType.<StructureMemberRef>builder(() -> null)
                    .serialize(StructureMemberRef.CODEC)
                    .build());

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
