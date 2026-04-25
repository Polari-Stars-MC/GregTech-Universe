package org.polaris2023.gtu.core.api.multiblock.runtime.access;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;
import org.polaris2023.gtu.core.api.multiblock.storage.StructureNetworkManager;

import java.util.UUID;

public final class StructureAccess {
    private StructureAccess() {
    }

    public static StructureNetworkManager manager(ServerLevel level) {
        return StructureNetworkManager.get(level);
    }

    @Nullable
    public static UUID findNetworkId(ServerLevel level, long pos) {
        return manager(level).findNetworkId(pos);
    }

    @Nullable
    public static StructureNetwork findNetwork(ServerLevel level, long pos) {
        return manager(level).findNetwork(pos);
    }
}
