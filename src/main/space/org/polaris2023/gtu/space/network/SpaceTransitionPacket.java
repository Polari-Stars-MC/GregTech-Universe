package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.runtime.SpaceDomain;
import org.polaris2023.gtu.space.runtime.SpaceTransitionDirection;

import java.util.UUID;

public record SpaceTransitionPacket(
        boolean active,
        long serverTick,
        UUID transitionId,
        SpaceTransitionDirection direction,
        SpaceDomain sourceDomain,
        SpaceDomain targetDomain,
        String bodyId,
        String sourceDimension,
        String targetDimension,
        long startTick,
        long cutoverTick,
        long completeTick,
        double startAltitudeMeters,
        double cutoverAltitudeMeters,
        double completeAltitudeMeters,
        boolean cutoverApplied
) implements CustomPacketPayload {
    public static final Type<SpaceTransitionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "space_transition"));

    public static final StreamCodec<FriendlyByteBuf, SpaceTransitionPacket> STREAM_CODEC =
            StreamCodec.of(SpaceTransitionPacket::encode, SpaceTransitionPacket::decode);

    public static SpaceTransitionPacket inactive(long serverTick) {
        return new SpaceTransitionPacket(false, serverTick, null, null, null, null,
                null, null, null, 0L, 0L, 0L, 0.0, 0.0, 0.0, false);
    }

    private static void encode(FriendlyByteBuf buf, SpaceTransitionPacket packet) {
        buf.writeBoolean(packet.active);
        buf.writeLong(packet.serverTick);
        if (!packet.active) {
            return;
        }
        SpaceNetworkCodecs.writeNullableUuid(buf, packet.transitionId);
        buf.writeEnum(packet.direction);
        buf.writeEnum(packet.sourceDomain);
        buf.writeEnum(packet.targetDomain);
        buf.writeUtf(packet.bodyId);
        buf.writeUtf(packet.sourceDimension);
        buf.writeUtf(packet.targetDimension);
        buf.writeLong(packet.startTick);
        buf.writeLong(packet.cutoverTick);
        buf.writeLong(packet.completeTick);
        buf.writeDouble(packet.startAltitudeMeters);
        buf.writeDouble(packet.cutoverAltitudeMeters);
        buf.writeDouble(packet.completeAltitudeMeters);
        buf.writeBoolean(packet.cutoverApplied);
    }

    private static SpaceTransitionPacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        long serverTick = buf.readLong();
        if (!active) {
            return inactive(serverTick);
        }
        return new SpaceTransitionPacket(
                true,
                serverTick,
                SpaceNetworkCodecs.readNullableUuid(buf),
                buf.readEnum(SpaceTransitionDirection.class),
                buf.readEnum(SpaceDomain.class),
                buf.readEnum(SpaceDomain.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
