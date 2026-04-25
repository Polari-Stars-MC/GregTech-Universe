package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.server.level.ServerLevel;
import org.polaris2023.gtu.core.api.multiblock.StructureIds;

public final class StructureMemberRefStorage {
    private StructureMemberRefStorage() {
    }

    public static StructureMemberRefSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(StructureMemberRefSavedData.FACTORY, StructureIds.MEMBER_REF_DATA_NAME);
    }
}
