package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record StructureTemplateDefinition(List<StructureNodeDefinition> nodes) {
    public static final Codec<StructureTemplateDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StructureNodeDefinition.CODEC.listOf().fieldOf("nodes").forGetter(StructureTemplateDefinition::nodes)
    ).apply(instance, StructureTemplateDefinition::new));

    public StructureTemplateDefinition {
        nodes = List.copyOf(nodes);
    }
}
