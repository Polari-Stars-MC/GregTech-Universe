package org.polaris2023.gtu.core.api.multiblock.runtime.check;

public record StructureNodeDefinition(
        long relativePos,
        int expectedStateId,
        byte flags,
        int priority
) {
}
