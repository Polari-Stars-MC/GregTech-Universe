package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

public record SpaceLandingRequestPacket(boolean descending) implements CustomPacketPayload {
    public static final Type<SpaceLandingRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "space_landing_request"));

    public static final StreamCodec<FriendlyByteBuf, SpaceLandingRequestPacket> STREAM_CODEC =
            StreamCodec.of(SpaceLandingRequestPacket::encode, SpaceLandingRequestPacket::decode);

    private static void encode(FriendlyByteBuf buf, SpaceLandingRequestPacket packet) {
        buf.writeBoolean(packet.descending);
    }

    private static SpaceLandingRequestPacket decode(FriendlyByteBuf buf) {
        return new SpaceLandingRequestPacket(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
