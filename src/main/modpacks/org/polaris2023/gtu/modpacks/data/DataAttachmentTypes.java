package org.polaris2023.gtu.modpacks.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

public class DataAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GregtechUniverseModPacks.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> WATER_SPEED = REGISTER.register("water_speed", () -> AttachmentType
                .builder(() -> 0)
                .serialize(Codec.INT)
                .sync(ByteBufCodecs.INT)
                .build());//水流速的一个动量



    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
