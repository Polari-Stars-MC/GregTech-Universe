package org.polaris2023.gtu.core.init;

import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

public final class ClayCauldronFluidRegistries {
    private ClayCauldronFluidRegistries() {
    }

    public static void register(RegisterCauldronFluidContentEvent event) {
        if (CauldronFluidContent.getForFluid(Fluids.WATER) != null) {
            return;
        }
        event.register(BlockRegistries.WATER_CLAY_CAULDRON.get(), Fluids.WATER, 1000, LayeredCauldronBlock.LEVEL);
    }
}
