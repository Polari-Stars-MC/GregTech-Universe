package org.polaris2023.gtu.space.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

public record KspDebugSnapshotRequestPacket() implements CustomPacketPayload {
    public static final KspDebugSnapshotRequestPacket INSTANCE = new KspDebugSnapshotRequestPacket();

    public static final Type<KspDebugSnapshotRequestPacket> TYPE =
            new Type<>(GregtechUniverseSpace.id("ksp_debug_snapshot_request"));

    public static final StreamCodec<ByteBuf, KspDebugSnapshotRequestPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
