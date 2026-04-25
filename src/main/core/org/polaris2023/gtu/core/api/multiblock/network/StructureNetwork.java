package org.polaris2023.gtu.core.api.multiblock.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StructureNetwork {
    private final UUID id;
    private final ResourceLocation machineId;
    private long controllerPos;
    private boolean formed;
    private boolean dirty;
    private boolean removed;
    private Direction facing;
    private final Set<Long> members;
    private final Set<Long> inputParts;
    private final Set<Long> outputParts;
    private final Set<Long> hatchParts;
    private final Set<Long> brokenPositions;

    public StructureNetwork(UUID id, ResourceLocation machineId, long controllerPos) {
        this.id = id;
        this.machineId = machineId;
        this.controllerPos = controllerPos;
        this.formed = false;
        this.dirty = false;
        this.removed = false;
        this.facing = Direction.NORTH;
        this.members = new HashSet<>();
        this.inputParts = new HashSet<>();
        this.outputParts = new HashSet<>();
        this.hatchParts = new HashSet<>();
        this.brokenPositions = new HashSet<>();
    }

    public UUID id() {
        return id;
    }

    public ResourceLocation machineId() {
        return machineId;
    }

    public long controllerPos() {
        return controllerPos;
    }

    public BlockPos controllerBlockPos() {
        return BlockPos.of(controllerPos);
    }

    public void setControllerPos(long controllerPos) {
        this.controllerPos = controllerPos;
    }

    public boolean formed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
    }

    public boolean dirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean removed() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public Direction facing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public Set<Long> members() {
        return members;
    }

    public Set<Long> inputParts() {
        return inputParts;
    }

    public Set<Long> outputParts() {
        return outputParts;
    }

    public Set<Long> hatchParts() {
        return hatchParts;
    }

    public Set<Long> brokenPositions() {
        return brokenPositions;
    }

    public boolean contains(long pos) {
        return members.contains(pos);
    }

    public void addMember(long pos, StructureMemberType type) {
        members.add(pos);
        switch (type) {
            case INPUT -> inputParts.add(pos);
            case OUTPUT -> outputParts.add(pos);
            case HATCH -> hatchParts.add(pos);
            default -> {
            }
        }
    }

    public void removeMember(long pos) {
        members.remove(pos);
        inputParts.remove(pos);
        outputParts.remove(pos);
        hatchParts.remove(pos);
        brokenPositions.remove(pos);
    }

    public void markBroken(long pos) {
        brokenPositions.add(pos);
        dirty = true;
        formed = false;
    }

    public boolean isInput(long pos) {
        return inputParts.contains(pos);
    }

    public boolean isOutput(long pos) {
        return outputParts.contains(pos);
    }
}
