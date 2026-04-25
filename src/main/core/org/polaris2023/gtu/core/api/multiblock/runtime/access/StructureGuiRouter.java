package org.polaris2023.gtu.core.api.multiblock.runtime.access;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;

public final class StructureGuiRouter {
    private StructureGuiRouter() {
    }

    public static boolean shouldRouteToController(ServerLevel level, long clickedPos) {
        StructureNetwork network = StructureAccess.findNetwork(level, clickedPos);
        if (network == null) {
            return false;
        }
        return !network.isInput(clickedPos) && !network.isOutput(clickedPos);
    }

    @Nullable
    public static BlockPos resolveController(ServerLevel level, long clickedPos) {
        StructureNetwork network = StructureAccess.findNetwork(level, clickedPos);
        if (network == null) {
            return null;
        }
        return network.controllerBlockPos();
    }
}
