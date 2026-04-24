package org.polaris2023.gtu.modpacks.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.blockentity.DamShaftBlockEntity;
import org.polaris2023.gtu.modpacks.blockentity.StressOutputHatchBlockEntity;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamControllerBlockEntity;

/**
 * 方块实体类型注册。
 */
public final class BlockEntityRegistries {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GregtechUniverseModPacks.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterDamControllerBlockEntity>>
            WATER_DAM_CONTROLLER_BE = REGISTER.register(
            "water_dam_controller_legacy",
            () -> {
                var type = new BlockEntityType[1]; // 用于捕获类型引用
                var built = BlockEntityType.Builder.of(
                        (pos, state) -> new WaterDamControllerBlockEntity(type[0], pos, state),
                        BlockRegistries.WATER_DAM_CONTROLLER_LEGACY.get()
                ).build(null);
                type[0] = built;
                return built;
            }
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StressOutputHatchBlockEntity>>
            STRESS_OUTPUT_HATCH_BE = REGISTER.register(
            "stress_output_hatch",
            () -> {
                var type = new BlockEntityType[1];
                var built = BlockEntityType.Builder.of(
                        (pos, state) -> new StressOutputHatchBlockEntity(type[0], pos, state),
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
                ).build(null);
                type[0] = built;
                return built;
            }
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DamShaftBlockEntity>>
            DAM_SHAFT_BE = REGISTER.register(
            "dam_shaft",
            () -> {
                var type = new BlockEntityType[1];
                var built = BlockEntityType.Builder.of(
                        (pos, state) -> new DamShaftBlockEntity(type[0], pos, state),
                        BlockRegistries.DAM_SHAFT.get()
                ).build(null);
                type[0] = built;
                return built;
            }
    );

    private BlockEntityRegistries() {}

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
