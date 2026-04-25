package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;
import org.polaris2023.gtu.core.api.multiblock.storage.StructureSavedData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StructureMainThreadApplier {
    private StructureMainThreadApplier() {
    }

    public static boolean apply(StructureSavedData savedData, StructureIndex index, StructureDelta delta) {
        if (delta.isEmpty()) {
            return false;
        }

        boolean applied = false;
        Map<UUID, StructureNetwork> networks = savedData.networks();

        for (Map.Entry<UUID, Set<Long>> entry : delta.removedMembers().entrySet()) {
            StructureNetwork network = networks.get(entry.getKey());
            if (network == null || network.removed()) {
                continue;
            }
            for (Long pos : entry.getValue()) {
                if (pos == network.controllerPos()) {
                    continue;
                }
                if (network.contains(pos) || network.isInput(pos) || network.isOutput(pos) || network.hatchParts().contains(pos)) {
                    network.removeMember(pos);
                    index.remove(pos);
                    applied = true;
                }
            }
        }

        for (Map.Entry<UUID, Set<Long>> entry : delta.brokenPositions().entrySet()) {
            StructureNetwork network = networks.get(entry.getKey());
            if (network == null || network.removed()) {
                continue;
            }
            for (Long pos : entry.getValue()) {
                network.markBroken(pos);
                applied = true;
            }
        }

        for (Map.Entry<UUID, Boolean> entry : delta.formedStates().entrySet()) {
            StructureNetwork network = networks.get(entry.getKey());
            if (network == null || network.removed()) {
                continue;
            }
            if (network.formed() != entry.getValue()) {
                network.setFormed(entry.getValue());
                applied = true;
            }
        }

        for (StructureRuntimeRequest request : delta.runtimeRequests()) {
            StructureNetwork network = networks.get(request.networkId());
            if (network == null || network.removed()) {
                continue;
            }
            switch (request.type()) {
                case REBUILD -> {
                    network.setDirty(true);
                    network.setFormed(false);
                    applied = true;
                }
                case VALIDATE -> {
                    network.setDirty(true);
                    applied = true;
                }
            }
        }

        for (UUID networkId : delta.dirtyNetworks()) {
            StructureNetwork network = networks.get(networkId);
            if (network == null || network.removed()) {
                continue;
            }
            if (!network.dirty()) {
                network.setDirty(true);
                applied = true;
            }
        }

        if (applied) {
            savedData.setDirty();
        }
        return applied;
    }
}
