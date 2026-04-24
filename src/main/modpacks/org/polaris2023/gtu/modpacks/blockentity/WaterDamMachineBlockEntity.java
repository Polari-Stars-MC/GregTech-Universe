package org.polaris2023.gtu.modpacks.blockentity;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.modpacks.machine.multiblock.WaterDamMachine;

import java.util.List;

public class WaterDamMachineBlockEntity extends MetaMachineBlockEntity
        implements IControlContraption, IHaveGoggleInformation {
    private static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(WaterDamMachineBlockEntity.class);

    public WaterDamMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Nullable
    public WaterDamMachine getWaterDamMachine() {
        return metaMachine instanceof WaterDamMachine waterDamMachine ? waterDamMachine : null;
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        WaterDamMachine machine = getWaterDamMachine();
        return machine != null && machine.isAttachedTo(contraption);
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
        WaterDamMachine machine = getWaterDamMachine();
        if (machine != null) {
            machine.attach(contraption);
        }
    }

    @Override
    public void onStall() {
        WaterDamMachine machine = getWaterDamMachine();
        if (machine != null) {
            machine.onStall();
        }
    }

    @Override
    public boolean isValid() {
        WaterDamMachine machine = getWaterDamMachine();
        return machine != null && machine.isControllerValid();
    }

    @Override
    public BlockPos getBlockPosition() {
        return getBlockPos();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        WaterDamMachine machine = getWaterDamMachine();
        return machine != null && machine.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }
}
