package org.polaris2023.gtu.core.api.multiblock.runtime.check;

public record CheckNode(
        long relativePos,
        int expectedStateId,
        byte flags,
        int horizontalDistanceSq,
        int priority
) {
    public boolean isCritical() {
        return CheckNodeFlags.hasFlag(flags, CheckNodeFlags.CRITICAL);
    }

    public boolean isSameChunkPreferred() {
        return CheckNodeFlags.hasFlag(flags, CheckNodeFlags.SAME_CHUNK);
    }
}
