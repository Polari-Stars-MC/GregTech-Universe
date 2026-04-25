package org.polaris2023.gtu.core.api.multiblock.event;

import java.util.UUID;

public record StructureInteractEvent(long position, UUID playerId) implements StructureEvent {
}
