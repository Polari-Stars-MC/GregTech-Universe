package org.polaris2023.gtu.modpacks.dam;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.block.StressOutputHatchBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DamMultiblockPatterns {
    private static final BlockPattern STACK_PATTERN = createStackPattern();
    private static final BlockPattern STACK_PATTERN_ENTITYIZED = createStackPattern(true);

    private DamMultiblockPatterns() {
    }

    public static boolean matchesMain(Level level, BlockPos controllerPos, Direction facing) {
        return match(createMainPattern(BlockRegistries.WATER_DAM_CONTROLLER.get()), level, controllerPos, facing);
    }

    public static boolean matchesStack(Level level, BlockPos controllerPos, Direction facing) {
        return match(STACK_PATTERN, level, controllerPos, facing);
    }

    public static boolean matchesMainEntityized(Level level, BlockPos controllerPos, Direction facing) {
        return match(createMainPattern(BlockRegistries.WATER_DAM_CONTROLLER.get(), true), level, controllerPos, facing);
    }

    public static boolean matchesStackEntityized(Level level, BlockPos controllerPos, Direction facing) {
        return match(STACK_PATTERN_ENTITYIZED, level, controllerPos, facing);
    }

    public static BlockPattern createMainPattern(Block controllerBlock) {
        return createMainPattern(controllerBlock, false);
    }

    public static BlockPattern createMainPattern(Block controllerBlock, boolean allowEntityizedBlades) {
        return basePattern(allowEntityizedBlades)
                .where('C', Predicates.controller(Predicates.blocks(controllerBlock)))
                .build();
    }

    public static BlockPattern createStackPattern() {
        return createStackPattern(false);
    }

    public static BlockPattern createStackPattern(boolean allowEntityizedBlades) {
        return basePattern(allowEntityizedBlades)
                .where('C', Predicates.any())
                .build();
    }

    public static List<MultiblockShapeInfo> createMainShapes(MultiblockMachineDefinition definition) {
        BlockPattern pattern = createMainPattern(definition.getBlock());
        int[] repetitions = Arrays.stream(pattern.aisleRepetitions)
                .mapToInt(repetition -> repetition[0])
                .toArray();
        BlockInfo[][][] basePreview = pattern.getPreview(repetitions);
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (DamTier tier : DamTier.values()) {
            shapes.add(new MultiblockShapeInfo(replaceStressHatch(basePreview, tier)));
        }
        return shapes;
    }

    public static MultiblockShapeInfo createMainShape(MultiblockMachineDefinition definition) {
        return createMainShapes(definition).get(0);
    }

    private static BlockInfo[][][] replaceStressHatch(BlockInfo[][][] source, DamTier tier) {
        BlockInfo[][][] copy = new BlockInfo[source.length][][];
        for (int x = 0; x < source.length; x++) {
            copy[x] = new BlockInfo[source[x].length][];
            for (int y = 0; y < source[x].length; y++) {
                copy[x][y] = Arrays.copyOf(source[x][y], source[x][y].length);
                for (int z = 0; z < copy[x][y].length; z++) {
                    BlockInfo info = copy[x][y][z];
                    if (info != null && info.getBlockState().getBlock() instanceof StressOutputHatchBlock) {
                        copy[x][y][z] = BlockInfo.fromBlockState(BlockRegistries.getStressHatchByTier(tier).get().defaultBlockState());
                    }
                }
            }
        }
        return copy;
    }

    private static boolean match(BlockPattern pattern, Level level, BlockPos controllerPos, Direction facing) {
        MultiblockState state = new MultiblockState(level, controllerPos);
        return pattern.checkPatternAt(state, controllerPos, facing, Direction.UP, false, false);
    }

    private static FactoryBlockPattern basePattern(boolean allowEntityizedBlades) {
        return FactoryBlockPattern.start()
                .aisle(
                        "XSSSSSX",
                        "SSXWXSS",
                        "SXXWXXS",
                        "SXXLXXS",
                        "SXXWXXS",
                        "SSXWXSS",
                        "XSSSSSX"
                )
                .aisle(
                        "SXXXXXC",
                        "XXPPPXX",
                        "XPTFTPX",
                        "XPFIFPX",
                        "XPTFTPX",
                        "XXPPPXX",
                        "XXXXXXX"
                )
                .aisle(
                        "XSSSSSX",
                        "SSXWXSS",
                        "SXXWXXS",
                        "SXXLXXS",
                        "SXXWXXS",
                        "SSXWXSS",
                        "XSSSSSX"
                )
                .aisle(
                        "XXXXXXX",
                        "XXXXXXX",
                        "XXXXXXX",
                        "XXXDXXX",
                        "XXXXXXX",
                        "XXXXXXX",
                        "XXXXXXX"
                )
                .aisle(
                        "XXXXXXX",
                        "XXXXXXX",
                        "XXXXXXX",
                        "XXXHXXX",
                        "XXXXXXX",
                        "XXXXXXX",
                        "XXXXXXX"
                )
                .where('X', Predicates.any())
                .where('S', Predicates.blocks(Blocks.STONE_BRICKS))
                .where('W', Predicates.blocks(Blocks.ANDESITE_WALL))
                .where('L', Predicates.blocks(Blocks.OAK_LOG))
                .where('I', Predicates.blocks(Blocks.IRON_BLOCK))
                .where('P', bladePredicate(DamStructureBlocks.treatedWoodPlanks(), allowEntityizedBlades))
                .where('T', bladePredicate(DamStructureBlocks.treatedWoodStairs(), allowEntityizedBlades))
                .where('F', bladePredicate(DamStructureBlocks.treatedWoodFrame(), allowEntityizedBlades))
                .where('D', Predicates.blocks(BlockRegistries.DAM_SHAFT.get()))
                .where('H', stressOutputHatches());
    }

    private static TraceabilityPredicate bladePredicate(Block bladeBlock, boolean allowEntityizedBlades) {
        if (!allowEntityizedBlades) {
            return Predicates.blocks(bladeBlock);
        }
        return Predicates.blocks(bladeBlock, Blocks.AIR);
    }

    private static TraceabilityPredicate stressOutputHatches() {
        return Predicates.blocks(
                BlockRegistries.STRESS_OUTPUT_HATCH_PRIMITIVE.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_ULV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_LV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_MV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_HV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_EV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_IV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_LUV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_ZPM.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_UV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_UHV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_UEV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_UIV.get(),
                BlockRegistries.STRESS_OUTPUT_HATCH_UXV.get()
        );
    }
}
