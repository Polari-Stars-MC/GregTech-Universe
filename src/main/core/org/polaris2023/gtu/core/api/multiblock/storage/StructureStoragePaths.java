package org.polaris2023.gtu.core.api.multiblock.storage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.polaris2023.gtu.core.GregtechUniverseCore;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureStoragePaths {
    public static final LevelResource GTU_CORE = new LevelResource(GregtechUniverseCore.MOD_ID);

    private static final Map<ServerLevel, LevelResource> LEVEL_RESOURCES = new ConcurrentHashMap<>();

    private StructureStoragePaths() {
    }

    public static LevelResource getLevelResource(ServerLevel level) {
        return LEVEL_RESOURCES.computeIfAbsent(level, ignored -> new LevelResource(GTU_CORE.getId() + "/" + level.dimension().location().toDebugFileName()));
    }

    public static Path getLevelDirectory(ServerLevel level) {
        return level.getServer().getWorldPath(getLevelResource(level));
    }

    public static Path getNetworkDataFile(ServerLevel level) {
        return getLevelDirectory(level).resolve("structure_networks.bin");
    }
}
