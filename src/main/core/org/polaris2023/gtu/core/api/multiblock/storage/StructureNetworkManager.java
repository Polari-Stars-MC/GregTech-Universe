package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.server.level.ServerLevel;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberType;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;
import org.polaris2023.gtu.core.api.multiblock.runtime.execution.StructureDelta;
import org.polaris2023.gtu.core.api.multiblock.runtime.execution.StructureIndex;
import org.polaris2023.gtu.core.api.multiblock.runtime.execution.StructureMainThreadApplier;
import org.polaris2023.gtu.core.api.multiblock.runtime.execution.StructureRuntime;
import org.polaris2023.gtu.core.api.multiblock.runtime.execution.StructureRuntimeThread;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

public class StructureNetworkManager {
    private static final Map<ServerLevel, StructureNetworkManager> INSTANCES = new IdentityHashMap<>();

    private final StructureNetworkFileStorage storage;
    private final StructureSavedData savedData;
    private final StructureIndex index;
    private final StructureRuntime runtime;
    private final StructureRuntimeThread runtimeThread;

    private StructureNetworkManager(StructureNetworkFileStorage storage) {
        this.storage = storage;
        this.savedData = storage.data();
        this.index = new StructureIndex();
        rebuildIndex();
        this.runtime = new StructureRuntime(savedData.networks(), index);
        this.runtimeThread = new StructureRuntimeThread(runtime, 5L);
    }

    public static synchronized StructureNetworkManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, serverLevel -> {
            return new StructureNetworkManager(StructureNetworkFileStorage.load(serverLevel));
        });
    }

    public static synchronized void remove(ServerLevel level) {
        StructureNetworkManager manager = INSTANCES.remove(level);
        if (manager != null) {
            manager.stopRuntime();
        }
    }

    public static synchronized void clearAll() {
        for (StructureNetworkManager manager : INSTANCES.values()) {
            manager.stopRuntime();
        }
        INSTANCES.clear();
    }

    public Map<UUID, StructureNetwork> networks() {
        return savedData.networks();
    }

    public StructureRuntime runtime() {
        return runtime;
    }

    public StructureRuntimeThread runtimeThread() {
        return runtimeThread;
    }

    public StructureNetwork createNetwork(UUID id, net.minecraft.resources.ResourceLocation machineId, long controllerPos) {
        StructureNetwork network = new StructureNetwork(id, machineId, controllerPos);
        savedData.put(network);
        index.put(controllerPos, id);
        storage.saveIfDirty();
        return network;
    }

    public void registerMember(UUID networkId, long pos, StructureMemberType type) {
        StructureNetwork network = savedData.networks().get(networkId);
        if (network == null) {
            return;
        }
        network.addMember(pos, type);
        savedData.setDirty();
        index.put(pos, networkId);
        storage.saveIfDirty();
    }

    public void removeMember(long pos) {
        UUID networkId = index.getNetworkId(pos);
        if (networkId == null) {
            return;
        }
        StructureNetwork network = savedData.networks().get(networkId);
        if (network == null) {
            return;
        }
        network.removeMember(pos);
        index.remove(pos);
        savedData.setDirty();
        storage.saveIfDirty();
    }

    public UUID findNetworkId(long pos) {
        return index.getNetworkId(pos);
    }

    public StructureNetwork findNetwork(long pos) {
        UUID id = findNetworkId(pos);
        return id == null ? null : savedData.networks().get(id);
    }

    public void flushRuntimeDelta(StructureDelta delta) {
        StructureMainThreadApplier.apply(savedData, index, delta);
    }

    public void startRuntime(String threadName) {
        runtimeThread.start(threadName);
    }

    public void stopRuntime() {
        storage.saveIfDirty();
        runtimeThread.stop();
    }

    public void saveIfDirty() {
        storage.saveIfDirty();
    }

    private void rebuildIndex() {
        index.clear();
        for (StructureNetwork network : savedData.networks().values()) {
            index.put(network.controllerPos(), network.id());
            for (Long member : network.members()) {
                index.put(member, network.id());
            }
        }
    }
}
