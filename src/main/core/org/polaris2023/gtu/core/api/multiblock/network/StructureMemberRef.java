package org.polaris2023.gtu.core.api.multiblock.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public record StructureMemberRef(UUID networkId, StructureMemberType type) {
    public static final Codec<StructureMemberRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("network_id").forGetter(StructureMemberRef::networkId),
            StructureMemberType.CODEC.fieldOf("type").forGetter(StructureMemberRef::type)
    ).apply(instance, StructureMemberRef::new));
}
