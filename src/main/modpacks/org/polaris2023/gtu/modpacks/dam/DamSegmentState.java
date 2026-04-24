package org.polaris2023.gtu.modpacks.dam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DamSegmentState {
    private final int index;
    private final BlockPos controllerPos;
    private final BlockPos axisPos;
    private final BlockPos shaftPos;
    private final BlockPos hatchPos;
    private double flowSpeed;
    private double stressShare;
    private boolean hatchValid;
    private boolean assembled;
    private UUID contraptionId;

    public DamSegmentState(int index, BlockPos controllerPos, BlockPos axisPos, BlockPos shaftPos, BlockPos hatchPos) {
        this.index = index;
        this.controllerPos = controllerPos;
        this.axisPos = axisPos;
        this.shaftPos = shaftPos;
        this.hatchPos = hatchPos;
    }

    public int index() {
        return index;
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    public BlockPos axisPos() {
        return axisPos;
    }

    public BlockPos shaftPos() {
        return shaftPos;
    }

    public BlockPos hatchPos() {
        return hatchPos;
    }

    public double flowSpeed() {
        return flowSpeed;
    }

    public void setFlowSpeed(double flowSpeed) {
        this.flowSpeed = flowSpeed;
    }

    public double stressShare() {
        return stressShare;
    }

    public void setStressShare(double stressShare) {
        this.stressShare = stressShare;
    }

    public boolean hatchValid() {
        return hatchValid;
    }

    public void setHatchValid(boolean hatchValid) {
        this.hatchValid = hatchValid;
    }

    public boolean assembled() {
        return assembled;
    }

    public void setAssembled(boolean assembled) {
        this.assembled = assembled;
    }

    public UUID contraptionId() {
        return contraptionId;
    }

    public void setContraptionId(UUID contraptionId) {
        this.contraptionId = contraptionId;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Index", index);
        tag.put("ControllerPos", BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, controllerPos).getOrThrow());
        tag.put("AxisPos", BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, axisPos).getOrThrow());
        tag.put("ShaftPos", BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, shaftPos).getOrThrow());
        tag.put("HatchPos", BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, hatchPos).getOrThrow());
        tag.putDouble("FlowSpeed", flowSpeed);
        tag.putDouble("StressShare", stressShare);
        tag.putBoolean("HatchValid", hatchValid);
        tag.putBoolean("Assembled", assembled);
        if (contraptionId != null) {
            tag.putUUID("ContraptionId", contraptionId);
        }
        return tag;
    }

    public static DamSegmentState load(CompoundTag tag) {
        BlockPos controllerPos = BlockPos.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("ControllerPos")).getOrThrow();
        BlockPos axisPos = BlockPos.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("AxisPos")).getOrThrow();
        BlockPos shaftPos = BlockPos.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("ShaftPos")).getOrThrow();
        BlockPos hatchPos = BlockPos.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("HatchPos")).getOrThrow();
        DamSegmentState state = new DamSegmentState(tag.getInt("Index"), controllerPos, axisPos, shaftPos, hatchPos);
        state.flowSpeed = tag.getDouble("FlowSpeed");
        state.stressShare = tag.getDouble("StressShare");
        state.hatchValid = tag.getBoolean("HatchValid");
        state.assembled = tag.getBoolean("Assembled");
        if (tag.hasUUID("ContraptionId")) {
            state.contraptionId = tag.getUUID("ContraptionId");
        }
        return state;
    }

    public static ListTag saveAll(List<DamSegmentState> segments, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DamSegmentState segment : segments) {
            list.add(segment.save(registries));
        }
        return list;
    }

    public static List<DamSegmentState> loadAll(ListTag list) {
        List<DamSegmentState> segments = new ArrayList<>();
        for (Tag tag : list) {
            if (tag instanceof CompoundTag compoundTag) {
                segments.add(load(compoundTag));
            }
        }
        return segments;
    }
}
