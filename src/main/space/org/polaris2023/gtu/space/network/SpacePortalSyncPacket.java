package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.client.portal.PortalSyncData;
import org.polaris2023.gtu.space.portal.PortalType;

import java.util.UUID;

public record SpacePortalSyncPacket(
        UUID portalId,
        String sourceDimension,
        String targetDimension,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        PortalType portalType,
        int faceIndex,
        boolean active
) implements CustomPacketPayload {
    public static final Type<SpacePortalSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "space_portal_sync"));

    public static final StreamCodec<FriendlyByteBuf, SpacePortalSyncPacket> STREAM_CODEC =
            StreamCodec.of(SpacePortalSyncPacket::encode, SpacePortalSyncPacket::decode);

    public PortalSyncData toSyncData() {
        return new PortalSyncData(portalId, sourceDimension, targetDimension,
                minX, minY, minZ, maxX, maxY, maxZ, portalType, faceIndex, active);
    }

    private static void encode(FriendlyByteBuf buf, SpacePortalSyncPacket packet) {
        buf.writeUUID(packet.portalId);
        buf.writeUtf(packet.sourceDimension);
        buf.writeUtf(packet.targetDimension);
        buf.writeDouble(packet.minX);
        buf.writeDouble(packet.minY);
        buf.writeDouble(packet.minZ);
        buf.writeDouble(packet.maxX);
        buf.writeDouble(packet.maxY);
        buf.writeDouble(packet.maxZ);
        buf.writeEnum(packet.portalType);
        buf.writeVarInt(packet.faceIndex);
        buf.writeBoolean(packet.active);
    }

    private static SpacePortalSyncPacket decode(FriendlyByteBuf buf) {
        return new SpacePortalSyncPacket(
                buf.readUUID(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readEnum(PortalType.class),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
