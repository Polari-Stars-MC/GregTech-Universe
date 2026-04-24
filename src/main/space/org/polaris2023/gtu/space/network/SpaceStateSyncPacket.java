package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.simulation.SpacePlayerState;

import java.util.UUID;

public record SpaceStateSyncPacket(
        SpacePlayerState.Mode mode,
        String bodyId,
        String planetDimension,
        String stablePlanetDimension,
        String sdSlotDimension,
        double stableX,
        double stableY,
        double stableZ,
        float stableYRot,
        float stableXRot,
        UUID vesselId,
        String authorityVesselId,
        double vesselAltitudeMeters,
        UUID transitionId
) implements CustomPacketPayload {
    public static final Type<SpaceStateSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "space_state_sync"));

    public static final StreamCodec<FriendlyByteBuf, SpaceStateSyncPacket> STREAM_CODEC =
            StreamCodec.of(SpaceStateSyncPacket::encode, SpaceStateSyncPacket::decode);

    private static void encode(FriendlyByteBuf buf, SpaceStateSyncPacket packet) {
        buf.writeEnum(packet.mode);
        buf.writeUtf(packet.bodyId);
        buf.writeUtf(packet.planetDimension);
        buf.writeUtf(packet.stablePlanetDimension);
        SpaceNetworkCodecs.writeNullableString(buf, packet.sdSlotDimension);
        buf.writeDouble(packet.stableX);
        buf.writeDouble(packet.stableY);
        buf.writeDouble(packet.stableZ);
        buf.writeFloat(packet.stableYRot);
        buf.writeFloat(packet.stableXRot);
        SpaceNetworkCodecs.writeNullableUuid(buf, packet.vesselId);
        SpaceNetworkCodecs.writeNullableString(buf, packet.authorityVesselId);
        buf.writeDouble(packet.vesselAltitudeMeters);
        SpaceNetworkCodecs.writeNullableUuid(buf, packet.transitionId);
    }

    private static SpaceStateSyncPacket decode(FriendlyByteBuf buf) {
        return new SpaceStateSyncPacket(
                buf.readEnum(SpacePlayerState.Mode.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                SpaceNetworkCodecs.readNullableString(buf),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                SpaceNetworkCodecs.readNullableUuid(buf),
                SpaceNetworkCodecs.readNullableString(buf),
                buf.readDouble(),
                SpaceNetworkCodecs.readNullableUuid(buf)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
