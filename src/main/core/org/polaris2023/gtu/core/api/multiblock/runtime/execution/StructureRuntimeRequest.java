package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

import java.util.UUID;

public record StructureRuntimeRequest(
        UUID networkId,
        long triggerPos,
        StructureRequestType type,
        StructureRequestReason reason,
        int priority
) {
}
