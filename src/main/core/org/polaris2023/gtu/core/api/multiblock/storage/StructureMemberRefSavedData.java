package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.polaris2023.gtu.core.api.multiblock.StructureIds;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberRef;

import java.util.HashMap;
import java.util.Map;

public class StructureMemberRefSavedData extends SavedData {
    public static final Factory<StructureMemberRefSavedData> FACTORY = new Factory<>(StructureMemberRefSavedData::new, StructureMemberRefSavedData::load);

    private final Map<Long, StructureMemberRef> refs = new HashMap<>();

    public static StructureMemberRefSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        StructureMemberRefSavedData data = new StructureMemberRefSavedData();
        ListTag refsTag = tag.getList("member_refs", Tag.TAG_COMPOUND);
        for (Tag entry : refsTag) {
            if (!(entry instanceof CompoundTag compound)) {
                continue;
            }
            long pos = compound.getLong("pos");
            StructureMemberRef.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, compound.get("ref"))
                    .result()
                    .ifPresent(ref -> data.refs.put(pos, ref));
        }
        return data;
    }

    public Map<Long, StructureMemberRef> refs() {
        return refs;
    }

    public void put(long pos, StructureMemberRef ref) {
        refs.put(pos, ref);
        setDirty();
    }

    public StructureMemberRef get(long pos) {
        return refs.get(pos);
    }

    public void remove(long pos) {
        if (refs.remove(pos) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag refsTag = new ListTag();
        for (Map.Entry<Long, StructureMemberRef> entry : refs.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putLong("pos", entry.getKey());
            StructureMemberRef.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, entry.getValue())
                    .result()
                    .ifPresent(encoded -> compound.put("ref", encoded));
            refsTag.add(compound);
        }
        tag.put("member_refs", refsTag);
        return tag;
    }
}
