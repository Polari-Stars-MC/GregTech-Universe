package org.polaris2023.gtu.modpacks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CopperToolVeinMiningStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<CopperToolVeinMiningStatePayload> TYPE =
            new Type<>(GregtechUniverseModPacks.id("copper_tool_vein_mining_state"));

    public static final StreamCodec<FriendlyByteBuf, CopperToolVeinMiningStatePayload> STREAM_CODEC =
            StreamCodec.of(CopperToolVeinMiningStatePayload::encode, CopperToolVeinMiningStatePayload::decode);

    private static void encode(FriendlyByteBuf buf, CopperToolVeinMiningStatePayload payload) {
        buf.writeBoolean(payload.enabled());
    }

    private static CopperToolVeinMiningStatePayload decode(FriendlyByteBuf buf) {
        return new CopperToolVeinMiningStatePayload(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
