package org.polaris2023.gtu.modpacks.init;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.block.WaterDamControllerBlock;

public final class BlockRegistries {
    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(GregtechUniverseModPacks.MOD_ID);

    public static final DeferredBlock<WaterDamControllerBlock> WATER_DAM_CONTROLLER =
            REGISTER.registerBlock(
                    "water_dam_controller",
                    WaterDamControllerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );

    private BlockRegistries() {
    }

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
