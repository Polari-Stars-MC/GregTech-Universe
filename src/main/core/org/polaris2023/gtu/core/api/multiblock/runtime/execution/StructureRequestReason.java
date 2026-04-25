package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

public enum StructureRequestReason {
    BLOCK_BROKEN,
    BLOCK_PLACED,
    CHUNK_LOADED,
    MANUAL_TRIGGER,
    CONTROLLER_DIRTY,
    MEMBER_CHANGED
}
