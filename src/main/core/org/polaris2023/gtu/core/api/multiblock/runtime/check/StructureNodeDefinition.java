package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StructureNodeDefinition(
        long relativePos,
        int expectedStateId,
        byte flags,
        int priority
) {
    public static final Codec<StructureNodeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("relative_pos").forGetter(StructureNodeDefinition::relativePos),
            Codec.INT.fieldOf("expected_state_id").forGetter(StructureNodeDefinition::expectedStateId),
            Codec.BYTE.fieldOf("flags").forGetter(StructureNodeDefinition::flags),
            Codec.INT.fieldOf("priority").forGetter(StructureNodeDefinition::priority)
    ).apply(instance, StructureNodeDefinition::new));
}
