package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StructureValidator {
    private StructureValidator() {
    }

    public static StructureValidationResult validate(LevelReader level, BlockPos controllerPos, CompiledStructureTemplate template) {
        int matchedProbeCount = countMatches(level, controllerPos, template.quickProbeNodes(), null);
        boolean quickProbePassed = StructureQuickProbe.shouldRunFullCheck(matchedProbeCount, template.quickProbeNodes().size());
        if (!quickProbePassed) {
            return new StructureValidationResult(false, false, false, matchedProbeCount, template.totalRequired(), Map.of());
        }

        Map<Long, CheckNode> mismatches = new LinkedHashMap<>();
        int matchedCount = 0;
        for (LayerPlan layer : template.layers()) {
            matchedCount += countMatches(level, controllerPos, layer.orderedNodes(), mismatches);
        }

        boolean formed = mismatches.isEmpty() && matchedCount >= template.totalRequired();
        return new StructureValidationResult(true, true, formed, matchedCount, template.totalRequired(), mismatches);
    }

    private static int countMatches(LevelReader level, BlockPos controllerPos, Iterable<CheckNode> nodes, Map<Long, CheckNode> mismatches) {
        int matched = 0;
        for (CheckNode node : nodes) {
            BlockPos targetPos = controllerPos.offset(BlockPos.of(node.relativePos()));
            BlockState state = level.getBlockState(targetPos);
            if (Block.getId(state) == node.expectedStateId()) {
                matched++;
                continue;
            }
            if (mismatches != null) {
                mismatches.put(node.relativePos(), node);
            }
        }
        return matched;
    }
}
