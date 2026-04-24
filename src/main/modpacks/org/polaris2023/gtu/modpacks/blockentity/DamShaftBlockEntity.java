package org.polaris2023.gtu.modpacks.blockentity;

import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DamShaftBlockEntity extends BracketedKineticBlockEntity {
    public DamShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
