package org.polaris2023.gtu.modpacks.init;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.RotationState;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamMachineBlockEntity;
import org.polaris2023.gtu.modpacks.dam.DamMultiblockPatterns;
import org.polaris2023.gtu.modpacks.machine.multiblock.WaterDamMachine;

public final class MachineRegistries {
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(GregtechUniverseModPacks.MOD_ID);

    public static final MultiblockMachineDefinition WATER_DAM_CONTROLLER = REGISTRATE
            .multiblock(
                    "water_dam_controller",
                    WaterDamMachine::new,
                    MetaMachineBlock::new,
                    MetaMachineItem::new,
                    WaterDamMachineBlockEntity::new
            )
            .rotationState(RotationState.NON_Y_AXIS)
            .allowExtendedFacing(false)
            .langValue("Water Dam Controller")
            .blockProp(properties -> properties.strength(3.5F, 6.0F).requiresCorrectToolForDrops())
            .appearance(Blocks.STONE_BRICKS::defaultBlockState)
            .pattern(definition -> DamMultiblockPatterns.createMainPattern(definition.getBlock()))
            .shapeInfo(DamMultiblockPatterns::createMainShape)
            .blockModel((ctx, prov) -> {
                VariantBlockStateBuilder builder = prov.getVariantBuilder(ctx.getEntry());
                ModelFile.ExistingModelFile model = prov.models()
                        .getExistingFile(GregtechUniverseModPacks.id("block/machine/water_dam_controller"));

                builder.partialState()
                        .with(RotationState.NON_Y_AXIS.property, Direction.NORTH)
                        .modelForState()
                        .modelFile(model)
                        .addModel();

                builder.partialState()
                        .with(RotationState.NON_Y_AXIS.property, Direction.EAST)
                        .modelForState()
                        .modelFile(model)
                        .rotationY(90)
                        .addModel();

                builder.partialState()
                        .with(RotationState.NON_Y_AXIS.property, Direction.SOUTH)
                        .modelForState()
                        .modelFile(model)
                        .rotationY(180)
                        .addModel();

                builder.partialState()
                        .with(RotationState.NON_Y_AXIS.property, Direction.WEST)
                        .modelForState()
                        .modelFile(model)
                        .rotationY(270)
                        .addModel();
            })
            .tooltips(
                    Component.literal("Use a GT machine casing on the controller to lock the dam tier."),
                    Component.literal("If left unlocked, the controller follows the first stress hatch tier."),
                    Component.literal("Shift-right-click with an empty hand to preview the structure.")
            )
            .register();

    private MachineRegistries() {
    }

    public static void init() {
    }
}
