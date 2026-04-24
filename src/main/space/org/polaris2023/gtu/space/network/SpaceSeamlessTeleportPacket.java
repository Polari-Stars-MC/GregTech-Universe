package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

import java.util.UUID;

public record SpaceSeamlessTeleportPacket(
        UUID transitionId,
        String targetDimension,
        double x,
        double y,
        double z,
        float yRot,
        float xRot,
        long expireServerTick
) implements CustomPacketPayload {
    public static final Type<SpaceSeamlessTeleportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "space_seamless_teleport"));

    public static final StreamCodec<FriendlyByteBuf, SpaceSeamlessTeleportPacket> STREAM_CODEC =
            StreamCodec.of(SpaceSeamlessTeleportPacket::encode, SpaceSeamlessTeleportPacket::decode);

    private static void encode(FriendlyByteBuf buf, SpaceSeamlessTeleportPacket packet) {
        SpaceNetworkCodecs.writeNullableUuid(buf, packet.transitionId);
        buf.writeUtf(packet.targetDimension);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.yRot);
        buf.writeFloat(packet.xRot);
        buf.writeLong(packet.expireServerTick);
    }

    private static SpaceSeamlessTeleportPacket decode(FriendlyByteBuf buf) {
        return new SpaceSeamlessTeleportPacket(
                SpaceNetworkCodecs.readNullableUuid(buf),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readLong()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
