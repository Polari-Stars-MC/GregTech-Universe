package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import net.minecraft.world.level.block.Blocks;
import org.polaris2023.gtu.core.init.BlockRegistries;

import java.util.ArrayList;
import java.util.List;

public final class StructureDefinitionBootstrap {
    private StructureDefinitionBootstrap() {
    }

    public static void bootstrapDefaults(StructureTemplateRegistry registry) {
        registerTestMultiblock(registry);
    }

    public static void registerHandwritten(
            StructureTemplateRegistry registry,
            ResourceLocation machineId,
            List<StructureNodeDefinition> definitions
    ) {
        registry.register(machineId, definitions);
    }

    public static StructureNodeDefinition node(BlockPos relativePos, int expectedStateId, byte flags, int priority) {
        return new StructureNodeDefinition(relativePos.asLong(), expectedStateId, flags, priority);
    }

    public static List<StructureNodeDefinition> verticalColumn(int minY, int maxY, int expectedStateId, byte flags, int priority) {
        List<StructureNodeDefinition> definitions = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            definitions.add(node(new BlockPos(0, y, 0), expectedStateId, flags, priority));
        }
        return definitions;
    }

    public static void registerTestMultiblock(StructureTemplateRegistry registry) {
        ResourceLocation machineId = BlockRegistries.TEST_MULTIBLOCK_CONTROLLER.getId();
        int stoneId = net.minecraft.world.level.block.Block.getId(Blocks.STONE.defaultBlockState());
        byte criticalSameChunk = (byte) (CheckNodeFlags.CRITICAL | CheckNodeFlags.SAME_CHUNK);
        byte frameSameChunk = (byte) (CheckNodeFlags.FRAME | CheckNodeFlags.SAME_CHUNK);

        List<StructureNodeDefinition> definitions = new ArrayList<>();
        definitions.add(node(new BlockPos(1, 0, 0), stoneId, criticalSameChunk, 90));
        definitions.add(node(new BlockPos(-1, 0, 0), stoneId, criticalSameChunk, 90));
        definitions.add(node(new BlockPos(0, 0, 1), stoneId, criticalSameChunk, 90));
        definitions.add(node(new BlockPos(0, 0, -1), stoneId, criticalSameChunk, 90));
        definitions.add(node(new BlockPos(0, 1, 0), stoneId, frameSameChunk, 80));
        definitions.add(node(new BlockPos(0, 2, 0), stoneId, CheckNodeFlags.FRAME, 70));

        registerHandwritten(registry, machineId, definitions);
    }
}
