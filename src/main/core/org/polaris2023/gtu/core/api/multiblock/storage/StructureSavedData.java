package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StructureSavedData {
    private static final int FORMAT_VERSION = 1;

    private final Map<UUID, StructureNetwork> networks = new HashMap<>();
    private boolean dirty;

    public static StructureSavedData load(DataInput input) throws IOException {
        StructureSavedData data = new StructureSavedData();
        int version = input.readInt();
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported structure data version: " + version);
        }
        int networkCount = input.readInt();
        for (int i = 0; i < networkCount; i++) {
            UUID id = new UUID(input.readLong(), input.readLong());
            ResourceLocation machineId = ResourceLocation.parse(input.readUTF());
            long controllerPos = input.readLong();
            StructureNetwork network = new StructureNetwork(id, machineId, controllerPos);
            network.setFormed(input.readBoolean());
            network.setDirty(input.readBoolean());
            network.setRemoved(input.readBoolean());
            network.setFacing(Direction.from3DDataValue(input.readInt()));
            readLongSet(input, network.members());
            readLongSet(input, network.inputParts());
            readLongSet(input, network.outputParts());
            readLongSet(input, network.hatchParts());
            readLongSet(input, network.brokenPositions());
            data.track(network);
            data.networks.put(network.id(), network);
        }
        return data;
    }

    public Map<UUID, StructureNetwork> networks() {
        return networks;
    }

    public void put(StructureNetwork network) {
        track(network);
        networks.put(network.id(), network);
        setDirty();
    }

    public void remove(UUID id) {
        networks.remove(id);
        setDirty();
    }

    public void save(DataOutput output) throws IOException {
        output.writeInt(FORMAT_VERSION);
        output.writeInt(networks.size());
        for (StructureNetwork network : networks.values()) {
            output.writeLong(network.id().getMostSignificantBits());
            output.writeLong(network.id().getLeastSignificantBits());
            output.writeUTF(network.machineId().toString());
            output.writeLong(network.controllerPos());
            output.writeBoolean(network.formed());
            output.writeBoolean(network.dirty());
            output.writeBoolean(network.removed());
            output.writeInt(network.facing().get3DDataValue());
            writeLongSet(output, network.members());
            writeLongSet(output, network.inputParts());
            writeLongSet(output, network.outputParts());
            writeLongSet(output, network.hatchParts());
            writeLongSet(output, network.brokenPositions());
        }
    }

    public void setDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    private void track(StructureNetwork network) {
        network.setMutationListener(this::setDirty);
    }

    private static void writeLongSet(DataOutput output, Set<Long> values) throws IOException {
        output.writeInt(values.size());
        for (Long value : values) {
            output.writeLong(value);
        }
    }

    private static void readLongSet(DataInput input, Set<Long> target) throws IOException {
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            target.add(input.readLong());
        }
    }
}
