package org.polaris2023.gtu.core.api.multiblock.event;

public record StructureBlockPlacedEvent(long position, int blockStateId) implements StructureEvent {
}
