package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureIndex {
    private final Map<Long, UUID> posToNetwork = new HashMap<>();

    public UUID getNetworkId(long pos) {
        return posToNetwork.get(pos);
    }

    public void put(long pos, UUID id) {
        posToNetwork.put(pos, id);
    }

    public void remove(long pos) {
        posToNetwork.remove(pos);
    }

    public void clear() {
        posToNetwork.clear();
    }
}
