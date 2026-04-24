package org.polaris2023.gtu.space.network.payload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;

public record KspDebugSnapshotPacket(String snapshotJson) implements CustomPacketPayload {
    private static final Gson GSON = new GsonBuilder().create();
    private static final int MAX_JSON_LENGTH = 1_048_576;

    public static final Type<KspDebugSnapshotPacket> TYPE =
            new Type<>(GregtechUniverseSpace.id("ksp_debug_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, KspDebugSnapshotPacket> STREAM_CODEC =
            StreamCodec.of(KspDebugSnapshotPacket::encode, KspDebugSnapshotPacket::decode);

    public static KspDebugSnapshotPacket fromSnapshot(KspSnapshot snapshot) {
        return new KspDebugSnapshotPacket(GSON.toJson(snapshot));
    }

    public KspSnapshot snapshot() {
        return GSON.fromJson(snapshotJson, KspSnapshot.class);
    }

    private static void encode(FriendlyByteBuf buf, KspDebugSnapshotPacket packet) {
        buf.writeUtf(packet.snapshotJson, MAX_JSON_LENGTH);
    }

    private static KspDebugSnapshotPacket decode(FriendlyByteBuf buf) {
        return new KspDebugSnapshotPacket(buf.readUtf(MAX_JSON_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

