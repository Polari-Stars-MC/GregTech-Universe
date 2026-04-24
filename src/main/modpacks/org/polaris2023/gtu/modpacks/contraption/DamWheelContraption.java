package org.polaris2023.gtu.modpacks.contraption;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;
import org.polaris2023.gtu.modpacks.dam.DamBlueprint;
import org.polaris2023.gtu.modpacks.dam.DamSegmentState;
import org.polaris2023.gtu.modpacks.dam.DamStructureBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamWheelContraption extends BearingContraption {
    private static final Logger LOGGER = LoggerFactory.getLogger("GTU Water Dam");
    private final DamSegmentState segment;

    public DamWheelContraption(DamSegmentState segment, Direction facing) {
        super(false, facing);
        this.segment = segment;
    }

    @Override
    public boolean assemble(Level level, BlockPos ignored) throws AssemblyException {
        this.anchor = segment.axisPos();
        // Create's manual capture path expects bounds to be initialized before addBlock().
        this.bounds = new AABB(BlockPos.ZERO);
        int bladeCandidates = 0;
        int nullCaptures = 0;
        LOGGER.debug("Dam wheel assemble start for segment {} at {} using facing {}",
                segment.index() + 1, segment.axisPos(), getFacing());

        for (BlockPos axisRelativeOffset : DamBlueprint.bladeOffsets()) {
            BlockPos worldPos = DamBlueprint.rotateAxisOffset(segment.axisPos(), axisRelativeOffset, getFacing());
            BlockState state = level.getBlockState(worldPos);
            if (!isDamBladeBlock(state)) {
                continue;
            }
            bladeCandidates++;

            Pair<?, ?> capture = capture(level, worldPos);
            if (capture != null) {
                addBlock(level, worldPos, (Pair) capture);
            } else {
                nullCaptures++;
            }
        }
        LOGGER.debug("Dam wheel assemble captured {}/{} blade blocks for segment {} at {} (nullCaptures={})",
                blocks.size(), bladeCandidates, segment.index() + 1, segment.axisPos(), nullCaptures);

        if (blocks.isEmpty()) {
            return false;
        }

        LOGGER.debug("Dam wheel startMoving for segment {} at {} (capturedBlocks={})",
                segment.index() + 1, segment.axisPos(), blocks.size());
        startMoving(level);
        expandBoundsAroundAxis(getFacing().getAxis());
        LOGGER.debug("Dam wheel assemble finished for segment {} at {} (capturedBlocks={})",
                segment.index() + 1, segment.axisPos(), blocks.size());
        return true;
    }

    @Override
    public boolean canBeStabilized(Direction direction, BlockPos blockPos) {
        return false;
    }

    private static boolean isDamBladeBlock(BlockState state) {
        return DamStructureBlocks.isBladeBlock(state);
    }
}
