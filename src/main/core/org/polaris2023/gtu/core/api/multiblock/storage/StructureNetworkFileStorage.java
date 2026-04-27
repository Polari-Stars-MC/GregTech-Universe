package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.server.level.ServerLevel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class StructureNetworkFileStorage {
    private final Path file;
    private final StructureSavedData data;

    private StructureNetworkFileStorage(Path file, StructureSavedData data) {
        this.file = file;
        this.data = data;
    }

    public static StructureNetworkFileStorage load(ServerLevel level) {
        Path file = StructureStoragePaths.getNetworkDataFile(level);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                return new StructureNetworkFileStorage(file, new StructureSavedData());
            }
            StructureSavedData data;
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
                 DataInputStream input = new DataInputStream(new BufferedInputStream(Channels.newInputStream(channel)))) {
                data = StructureSavedData.load(input);
            }
            data.clearDirty();
            return new StructureNetworkFileStorage(file, data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load structure network data from " + file, e);
        }
    }

    public StructureSavedData data() {
        return data;
    }

    public void saveIfDirty() {
        if (!data.isDirty()) {
            return;
        }
        save();
    }

    public void save() {
        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.deleteIfExists(tempFile);
            try (FileChannel channel = FileChannel.open(tempFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
                 DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(channel)))) {
                data.save(output);
                output.flush();
                channel.force(true);
            }
            replaceFile(tempFile);
            data.clearDirty();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save structure network data to " + file, e);
        }
    }

    private void replaceFile(Path tempFile) throws IOException {
        try {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (IOException atomicMoveFailure) {
            try {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException replaceMoveFailure) {
                atomicMoveFailure.addSuppressed(replaceMoveFailure);
            }

            try {
                Files.copy(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(tempFile);
                return;
            } catch (IOException copyFailure) {
                atomicMoveFailure.addSuppressed(copyFailure);
            }

            throw atomicMoveFailure;
        }
    }
}
