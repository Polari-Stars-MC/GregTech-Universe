package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

import org.polaris2023.gtu.core.api.multiblock.event.StructureBlockBrokenEvent;
import org.polaris2023.gtu.core.api.multiblock.event.StructureBlockPlacedEvent;
import org.polaris2023.gtu.core.api.multiblock.event.StructureEvent;
import org.polaris2023.gtu.core.api.multiblock.event.StructureInteractEvent;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StructureRuntime {
    private final Map<UUID, StructureNetwork> networks;
    private final StructureIndex index;
    private final Queue<StructureEvent> inputQueue;
    private final Queue<StructureDelta> outputQueue;

    public StructureRuntime(Map<UUID, StructureNetwork> networks, StructureIndex index) {
        this.networks = networks;
        this.index = index;
        this.inputQueue = new ConcurrentLinkedQueue<>();
        this.outputQueue = new ConcurrentLinkedQueue<>();
    }

    public void submit(StructureEvent event) {
        inputQueue.add(event);
    }

    public boolean processInputBatch() {
        StructureDelta delta = new StructureDelta();
        StructureEvent event;
        while ((event = inputQueue.poll()) != null) {
            process(event, delta);
        }
        if (!delta.isEmpty()) {
            outputQueue.add(delta);
            return true;
        }
        return false;
    }

    public StructureDelta drainOutput() {
        StructureDelta merged = new StructureDelta();
        StructureDelta delta;
        while ((delta = outputQueue.poll()) != null) {
            merged.merge(delta);
        }
        return merged;
    }

    private void process(StructureEvent event, StructureDelta delta) {
        UUID networkId = index.getNetworkId(event.position());
        if (networkId == null) {
            return;
        }
        StructureNetwork network = networks.get(networkId);
        if (network == null || network.removed()) {
            return;
        }

        switch (event) {
            case StructureBlockBrokenEvent brokenEvent -> {
                delta.markBroken(network.id(), brokenEvent.position());
                if (brokenEvent.position() != network.controllerPos()) {
                    delta.removeMember(network.id(), brokenEvent.position());
                }
                delta.setFormed(network.id(), false);
                delta.addRequest(new StructureRuntimeRequest(
                        network.id(),
                        brokenEvent.position(),
                        StructureRequestType.REBUILD,
                        StructureRequestReason.BLOCK_BROKEN,
                        100
                ));
            }
            case StructureBlockPlacedEvent placedEvent -> {
                delta.markDirty(network.id());
                delta.addRequest(new StructureRuntimeRequest(
                        network.id(),
                        placedEvent.position(),
                        StructureRequestType.VALIDATE,
                        StructureRequestReason.BLOCK_PLACED,
                        50
                ));
            }
            case StructureInteractEvent ignored -> {
            }
        }
    }
}
