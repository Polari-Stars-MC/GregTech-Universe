package org.polaris2023.gtu.core.api.multiblock.runtime.execution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StructureDelta {
    private final Set<UUID> dirtyNetworks = new HashSet<>();
    private final Map<UUID, Set<Long>> brokenPositions = new HashMap<>();
    private final Map<UUID, Set<Long>> removedMembers = new HashMap<>();
    private final Map<UUID, Boolean> formedStates = new HashMap<>();
    private final Set<StructureRuntimeRequest> runtimeRequests = new HashSet<>();

    public Set<UUID> dirtyNetworks() {
        return dirtyNetworks;
    }

    public Map<UUID, Set<Long>> brokenPositions() {
        return brokenPositions;
    }

    public Map<UUID, Set<Long>> removedMembers() {
        return removedMembers;
    }

    public Map<UUID, Boolean> formedStates() {
        return formedStates;
    }

    public Set<StructureRuntimeRequest> runtimeRequests() {
        return runtimeRequests;
    }

    public void markDirty(UUID networkId) {
        dirtyNetworks.add(networkId);
    }

    public void markBroken(UUID networkId, long pos) {
        dirtyNetworks.add(networkId);
        brokenPositions.computeIfAbsent(networkId, ignored -> new HashSet<>()).add(pos);
    }

    public void removeMember(UUID networkId, long pos) {
        dirtyNetworks.add(networkId);
        removedMembers.computeIfAbsent(networkId, ignored -> new HashSet<>()).add(pos);
    }

    public void setFormed(UUID networkId, boolean formed) {
        dirtyNetworks.add(networkId);
        formedStates.put(networkId, formed);
    }

    public void addRequest(StructureRuntimeRequest request) {
        dirtyNetworks.add(request.networkId());
        runtimeRequests.add(request);
    }

    public void requestValidate(UUID networkId) {
        dirtyNetworks.add(networkId);
        runtimeRequests.add(new StructureRuntimeRequest(
                networkId,
                Long.MIN_VALUE,
                StructureRequestType.VALIDATE,
                StructureRequestReason.MANUAL_TRIGGER,
                0
        ));
    }

    public void merge(StructureDelta other) {
        dirtyNetworks.addAll(other.dirtyNetworks);
        other.brokenPositions.forEach((networkId, positions) ->
                brokenPositions.computeIfAbsent(networkId, ignored -> new HashSet<>()).addAll(positions));
        other.removedMembers.forEach((networkId, positions) ->
                removedMembers.computeIfAbsent(networkId, ignored -> new HashSet<>()).addAll(positions));
        formedStates.putAll(other.formedStates);
        runtimeRequests.addAll(other.runtimeRequests);
    }

    public boolean isEmpty() {
        return dirtyNetworks.isEmpty()
                && brokenPositions.isEmpty()
                && removedMembers.isEmpty()
                && formedStates.isEmpty()
                && runtimeRequests.isEmpty();
    }
}
