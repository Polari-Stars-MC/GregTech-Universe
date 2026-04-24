package org.polaris2023.gtu.space.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

final class SpaceNetworkCodecs {
    private SpaceNetworkCodecs() {
    }

    static void writeNullableString(FriendlyByteBuf buf, String value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeUtf(value);
        }
    }

    static String readNullableString(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf() : null;
    }

    static void writeNullableUuid(FriendlyByteBuf buf, UUID value) {
        writeNullableString(buf, value == null ? null : value.toString());
    }

    static UUID readNullableUuid(FriendlyByteBuf buf) {
        String value = readNullableString(buf);
        return value == null ? null : UUID.fromString(value);
    }
}
