package org.polaris2023.gtu.core.api.multiblock.event;

public sealed interface StructureEvent permits
        StructureBlockBrokenEvent,
        StructureBlockPlacedEvent,
        StructureInteractEvent {
    long position();
}
