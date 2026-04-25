package org.polaris2023.gtu.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.block.entity.TestMultiblockControllerBlockEntity;

public final class BlockEntityRegistries {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GregtechUniverseCore.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestMultiblockControllerBlockEntity>> TEST_MULTIBLOCK_CONTROLLER =
            REGISTER.register(
                    "test_multiblock_controller",
                    () -> BlockEntityType.Builder.of(
                            TestMultiblockControllerBlockEntity::new,
                            BlockRegistries.TEST_MULTIBLOCK_CONTROLLER.get()
                    ).build(null)
            );

    private BlockEntityRegistries() {
    }

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
