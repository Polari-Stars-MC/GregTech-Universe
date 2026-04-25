package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import org.polaris2023.gtu.core.api.multiblock.StructureIds;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberType;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureSavedData extends SavedData {
    public static final Factory<StructureSavedData> FACTORY = new Factory<>(StructureSavedData::new, StructureSavedData::load);

    private final Map<UUID, StructureNetwork> networks = new HashMap<>();

    public static StructureSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        StructureSavedData data = new StructureSavedData();
        ListTag networksTag = tag.getList("networks", Tag.TAG_COMPOUND);
        for (Tag networkTag : networksTag) {
            if (!(networkTag instanceof CompoundTag compound)) {
                continue;
            }
            UUID id = compound.getUUID("id");
            ResourceLocation machineId = ResourceLocation.parse(compound.getString("machine_id"));
            long controllerPos = compound.getLong("controller_pos");
            StructureNetwork network = new StructureNetwork(id, machineId, controllerPos);
            network.setFormed(compound.getBoolean("formed"));
            network.setDirty(compound.getBoolean("dirty"));
            network.setRemoved(compound.getBoolean("removed"));
            network.setFacing(net.minecraft.core.Direction.byName(compound.getString("facing")));
            readLongSet(compound.getList("members", Tag.TAG_LONG), network.members());
            readLongSet(compound.getList("inputs", Tag.TAG_LONG), network.inputParts());
            readLongSet(compound.getList("outputs", Tag.TAG_LONG), network.outputParts());
            readLongSet(compound.getList("hatches", Tag.TAG_LONG), network.hatchParts());
            readLongSet(compound.getList("broken", Tag.TAG_LONG), network.brokenPositions());
            data.networks.put(id, network);
        }
        return data;
    }

    public Map<UUID, StructureNetwork> networks() {
        return networks;
    }

    public void put(StructureNetwork network) {
        networks.put(network.id(), network);
        setDirty();
    }

    public void remove(UUID id) {
        networks.remove(id);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag networksTag = new ListTag();
        for (StructureNetwork network : networks.values()) {
            CompoundTag networkTag = new CompoundTag();
            networkTag.putUUID("id", network.id());
            networkTag.putString("machine_id", network.machineId().toString());
            networkTag.putLong("controller_pos", network.controllerPos());
            networkTag.putBoolean("formed", network.formed());
            networkTag.putBoolean("dirty", network.dirty());
            networkTag.putBoolean("removed", network.removed());
            networkTag.putString("facing", network.facing().getSerializedName());
            networkTag.put("members", writeLongSet(network.members()));
            networkTag.put("inputs", writeLongSet(network.inputParts()));
            networkTag.put("outputs", writeLongSet(network.outputParts()));
            networkTag.put("hatches", writeLongSet(network.hatchParts()));
            networkTag.put("broken", writeLongSet(network.brokenPositions()));
            networksTag.add(networkTag);
        }
        tag.put("networks", networksTag);
        return tag;
    }

    private static ListTag writeLongSet(Iterable<Long> values) {
        ListTag listTag = new ListTag();
        for (Long value : values) {
            listTag.add(net.minecraft.nbt.LongTag.valueOf(value));
        }
        return listTag;
    }

    private static void readLongSet(ListTag listTag, java.util.Set<Long> target) {
        for (Tag tag : listTag) {
            if (tag instanceof net.minecraft.nbt.LongTag longTag) {
                target.add(longTag.getAsLong());
            }
        }
    }
}
