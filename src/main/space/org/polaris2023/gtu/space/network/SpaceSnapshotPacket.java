package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyKind;

import java.util.ArrayList;
import java.util.List;

public record SpaceSnapshotPacket(
        long simulationTick,
        double simulationSeconds,
        List<BodyData> bodies,
        List<VesselData> vessels
) implements CustomPacketPayload {
    public static final Type<SpaceSnapshotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "space_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, SpaceSnapshotPacket> STREAM_CODEC =
            StreamCodec.of(SpaceSnapshotPacket::encode, SpaceSnapshotPacket::decode);

    public record BodyData(
            String id,
            String referenceBodyId,
            KspBodyKind kind,
            double radius,
            double posX,
            double posY,
            double posZ,
            double velX,
            double velY,
            double velZ,
            double axisX,
            double axisY,
            double axisZ,
            double rotationPhaseRadians
    ) {
    }

    public record VesselData(
            String id,
            String primaryBodyId,
            double posX,
            double posY,
            double posZ,
            double velX,
            double velY,
            double velZ
    ) {
    }

    private static void encode(FriendlyByteBuf buf, SpaceSnapshotPacket packet) {
        buf.writeLong(packet.simulationTick);
        buf.writeDouble(packet.simulationSeconds);
        buf.writeVarInt(packet.bodies.size());
        for (BodyData body : packet.bodies) {
            buf.writeUtf(body.id);
            SpaceNetworkCodecs.writeNullableString(buf, body.referenceBodyId);
            buf.writeEnum(body.kind);
            buf.writeDouble(body.radius);
            buf.writeDouble(body.posX);
            buf.writeDouble(body.posY);
            buf.writeDouble(body.posZ);
            buf.writeDouble(body.velX);
            buf.writeDouble(body.velY);
            buf.writeDouble(body.velZ);
            buf.writeDouble(body.axisX);
            buf.writeDouble(body.axisY);
            buf.writeDouble(body.axisZ);
            buf.writeDouble(body.rotationPhaseRadians);
        }
        buf.writeVarInt(packet.vessels.size());
        for (VesselData vessel : packet.vessels) {
            buf.writeUtf(vessel.id);
            buf.writeUtf(vessel.primaryBodyId);
            buf.writeDouble(vessel.posX);
            buf.writeDouble(vessel.posY);
            buf.writeDouble(vessel.posZ);
            buf.writeDouble(vessel.velX);
            buf.writeDouble(vessel.velY);
            buf.writeDouble(vessel.velZ);
        }
    }

    private static SpaceSnapshotPacket decode(FriendlyByteBuf buf) {
        long simulationTick = buf.readLong();
        double simulationSeconds = buf.readDouble();
        int bodyCount = buf.readVarInt();
        List<BodyData> bodies = new ArrayList<>(bodyCount);
        for (int i = 0; i < bodyCount; i++) {
            bodies.add(new BodyData(
                    buf.readUtf(),
                    SpaceNetworkCodecs.readNullableString(buf),
                    buf.readEnum(KspBodyKind.class),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }
        int vesselCount = buf.readVarInt();
        List<VesselData> vessels = new ArrayList<>(vesselCount);
        for (int i = 0; i < vesselCount; i++) {
            vessels.add(new VesselData(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }
        return new SpaceSnapshotPacket(simulationTick, simulationSeconds, List.copyOf(bodies), List.copyOf(vessels));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
