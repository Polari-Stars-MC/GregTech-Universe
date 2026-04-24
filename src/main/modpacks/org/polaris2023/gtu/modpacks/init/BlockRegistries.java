package org.polaris2023.gtu.modpacks.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.block.DamShaftBlock;
import org.polaris2023.gtu.modpacks.block.StressOutputHatchBlock;
import org.polaris2023.gtu.modpacks.block.WaterDamControllerBlock;
import org.polaris2023.gtu.modpacks.dam.DamTier;

import java.util.function.Supplier;

public final class BlockRegistries {
    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(GregtechUniverseModPacks.MOD_ID);

    public static final Supplier<Block> WATER_DAM_CONTROLLER =
            () -> MachineRegistries.WATER_DAM_CONTROLLER.getBlock();

    public static final DeferredBlock<WaterDamControllerBlock> WATER_DAM_CONTROLLER_LEGACY =
            REGISTER.registerBlock(
                    "water_dam_controller_legacy",
                    WaterDamControllerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<DamShaftBlock> DAM_SHAFT =
            REGISTER.registerBlock(
                    "dam_shaft",
                    DamShaftBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            );

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_PRIMITIVE =
            registerStressHatch("stress_output_hatch_primitive", DamTier.PRIMITIVE);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_ULV =
            registerStressHatch("stress_output_hatch_ulv", DamTier.ULV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_LV =
            registerStressHatch("stress_output_hatch_lv", DamTier.LV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_MV =
            registerStressHatch("stress_output_hatch_mv", DamTier.MV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_HV =
            registerStressHatch("stress_output_hatch_hv", DamTier.HV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_EV =
            registerStressHatch("stress_output_hatch_ev", DamTier.EV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_IV =
            registerStressHatch("stress_output_hatch_iv", DamTier.IV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_LUV =
            registerStressHatch("stress_output_hatch_luv", DamTier.LuV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_ZPM =
            registerStressHatch("stress_output_hatch_zpm", DamTier.ZPM);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_UV =
            registerStressHatch("stress_output_hatch_uv", DamTier.UV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_UHV =
            registerStressHatch("stress_output_hatch_uhv", DamTier.UHV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_UEV =
            registerStressHatch("stress_output_hatch_uev", DamTier.UEV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_UIV =
            registerStressHatch("stress_output_hatch_uiv", DamTier.UIV);

    public static final DeferredBlock<StressOutputHatchBlock> STRESS_OUTPUT_HATCH_UXV =
            registerStressHatch("stress_output_hatch_uxv", DamTier.UXV);

    private static DeferredBlock<StressOutputHatchBlock> registerStressHatch(String name, DamTier tier) {
        return REGISTER.register(name,
                () -> new StressOutputHatchBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(3.0F, 6.0F)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops()
                                .noOcclusion(),
                        tier
                ));
    }

    public static DeferredBlock<StressOutputHatchBlock> getStressHatchByTier(DamTier tier) {
        return switch (tier) {
            case PRIMITIVE -> STRESS_OUTPUT_HATCH_PRIMITIVE;
            case ULV -> STRESS_OUTPUT_HATCH_ULV;
            case LV -> STRESS_OUTPUT_HATCH_LV;
            case MV -> STRESS_OUTPUT_HATCH_MV;
            case HV -> STRESS_OUTPUT_HATCH_HV;
            case EV -> STRESS_OUTPUT_HATCH_EV;
            case IV -> STRESS_OUTPUT_HATCH_IV;
            case LuV -> STRESS_OUTPUT_HATCH_LUV;
            case ZPM -> STRESS_OUTPUT_HATCH_ZPM;
            case UV -> STRESS_OUTPUT_HATCH_UV;
            case UHV -> STRESS_OUTPUT_HATCH_UHV;
            case UEV -> STRESS_OUTPUT_HATCH_UEV;
            case UIV -> STRESS_OUTPUT_HATCH_UIV;
            case UXV -> STRESS_OUTPUT_HATCH_UXV;
        };
    }

    private BlockRegistries() {
    }

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
