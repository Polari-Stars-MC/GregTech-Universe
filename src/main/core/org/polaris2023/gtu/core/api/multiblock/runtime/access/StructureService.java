package org.polaris2023.gtu.core.api.multiblock.runtime.access;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.api.multiblock.event.StructureBlockBrokenEvent;
import org.polaris2023.gtu.core.api.multiblock.event.StructureBlockPlacedEvent;
import org.polaris2023.gtu.core.api.multiblock.event.StructureInteractEvent;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberType;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;
import org.polaris2023.gtu.core.api.multiblock.runtime.execution.StructureDelta;
import org.polaris2023.gtu.core.api.multiblock.storage.StructureNetworkManager;

import java.util.UUID;

public final class StructureService {
    private StructureService() {
    }

    public static StructureNetwork createNetwork(ServerLevel level, ResourceLocation machineId, long controllerPos) {
        StructureNetworkManager manager = StructureAccess.manager(level);
        return manager.createNetwork(UUID.randomUUID(), machineId, controllerPos);
    }

    public static void registerMember(ServerLevel level, UUID networkId, long pos, StructureMemberType type) {
        StructureAccess.manager(level).registerMember(networkId, pos, type);
    }

    public static void removeMember(ServerLevel level, long pos) {
        StructureAccess.manager(level).removeMember(pos);
    }

    public static void submitBroken(ServerLevel level, long pos) {
        StructureAccess.manager(level).runtime().submit(new StructureBlockBrokenEvent(pos));
    }

    public static void submitPlaced(ServerLevel level, long pos, int blockStateId) {
        StructureAccess.manager(level).runtime().submit(new StructureBlockPlacedEvent(pos, blockStateId));
    }

    public static void submitInteract(ServerLevel level, long pos, UUID playerId) {
        StructureAccess.manager(level).runtime().submit(new StructureInteractEvent(pos, playerId));
    }

    public static void flush(ServerLevel level) {
        StructureNetworkManager manager = StructureAccess.manager(level);
        StructureDelta delta = manager.runtime().drainOutput();
        manager.flushRuntimeDelta(delta);
    }

    @Nullable
    public static StructureNetwork findNetwork(ServerLevel level, long pos) {
        return StructureAccess.findNetwork(level, pos);
    }
}
