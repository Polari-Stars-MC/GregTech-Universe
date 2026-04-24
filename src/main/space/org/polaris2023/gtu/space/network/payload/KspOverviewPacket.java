package org.polaris2023.gtu.space.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;

public record KspOverviewPacket(
        long simulationTick,
        double simulationSeconds,
        String universeId,
        String galaxyId,
        String systemUniversePosition,
        int bodyCount,
        int vesselCount,
        int rocheViolationCount
) implements CustomPacketPayload {

    public static final Type<KspOverviewPacket> TYPE =
            new Type<>(GregtechUniverseSpace.id("ksp_overview"));

    public static final StreamCodec<FriendlyByteBuf, KspOverviewPacket> STREAM_CODEC =
            StreamCodec.of(KspOverviewPacket::encode, KspOverviewPacket::decode);

    public static KspOverviewPacket fromSnapshot(KspSnapshot snapshot) {
        return new KspOverviewPacket(
                snapshot.simulationTick(),
                snapshot.simulationSeconds(),
                snapshot.universe().universeId(),
                snapshot.universe().galaxy().galaxyId(),
                snapshot.universe().systemUniversePosition().toString(),
                snapshot.bodies().size(),
                snapshot.vessels().size(),
                snapshot.rocheViolations().size()
        );
    }

    private static void encode(FriendlyByteBuf buf, KspOverviewPacket packet) {
        buf.writeVarLong(packet.simulationTick());
        buf.writeDouble(packet.simulationSeconds());
        buf.writeUtf(packet.universeId());
        buf.writeUtf(packet.galaxyId());
        buf.writeUtf(packet.systemUniversePosition());
        buf.writeVarInt(packet.bodyCount());
        buf.writeVarInt(packet.vesselCount());
        buf.writeVarInt(packet.rocheViolationCount());
    }

    private static KspOverviewPacket decode(FriendlyByteBuf buf) {
        return new KspOverviewPacket(
                buf.readVarLong(),
                buf.readDouble(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

