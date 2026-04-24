package org.polaris2023.gtu.modpacks.dam;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;

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

    public static MultiblockShapeInfo createMainShape(MultiblockMachineDefinition definition) {
        Block treatedPlanks = DamStructureBlocks.treatedWoodPlanks();
        Block treatedStairs = DamStructureBlocks.treatedWoodStairs();
        Block treatedFrame = DamStructureBlocks.treatedWoodFrame();

        return MultiblockShapeInfo.builder()
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
                .where('X', Blocks.AIR.defaultBlockState())
                .where('S', Blocks.STONE_BRICKS.defaultBlockState())
                .where('W', Blocks.ANDESITE_WALL.defaultBlockState())
                .where('L', Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z))
                .where('I', Blocks.IRON_BLOCK.defaultBlockState())
                .where('P', treatedPlanks.defaultBlockState())
                .where('T', treatedStairs.defaultBlockState())
                .where('F', treatedFrame.defaultBlockState())
                .where('D', BlockRegistries.DAM_SHAFT.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z))
                .where('H', BlockRegistries.STRESS_OUTPUT_HATCH_PRIMITIVE.get().defaultBlockState()
                        .setValue(DirectionalBlock.FACING, Direction.NORTH))
                .where('C', (IMachineBlock) definition.getBlock(), Direction.NORTH)
                .build();
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
