package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.Registrykeys;

import java.util.ArrayList;
import java.util.List;

public final class StructureDefinitionBootstrap {
    public static final ResourceKey<StructureTemplateDefinition> TEST_MULTIBLOCK_CONTROLLER =
            ResourceKey.create(Registrykeys.DYNAMIC_STRUCTURE_TEMPLATE_REGISTRY_KEY, GregtechUniverseCore.id("test_multiblock_controller"));

    private StructureDefinitionBootstrap() {
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

    public static StructureTemplateDefinition testMultiblockDefinition() {
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

        return new StructureTemplateDefinition(definitions);
    }

    public static void bootstrap(BootstrapContext<StructureTemplateDefinition> context) {
        context.register(TEST_MULTIBLOCK_CONTROLLER, testMultiblockDefinition());
    }
}
