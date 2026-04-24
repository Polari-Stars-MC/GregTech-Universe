package org.polaris2023.gtu.modpacks.client;

import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamMachineBlockEntity;
import org.polaris2023.gtu.modpacks.client.renderer.DamWheelRenderer;
import org.polaris2023.gtu.modpacks.client.screen.WaterDamScreen;
import org.polaris2023.gtu.modpacks.init.BlockEntityRegistries;
import org.polaris2023.gtu.modpacks.init.MachineRegistries;
import org.polaris2023.gtu.modpacks.init.MenuRegistries;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientRegistries {
    private ClientRegistries() {}

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistries.WATER_DAM_MENU.get(), WaterDamScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(waterDamControllerType(), DamWheelRenderer::new);
        event.registerBlockEntityRenderer(
                BlockEntityRegistries.DAM_SHAFT_BE.get(),
                BracketedKineticBlockEntityRenderer::new
        );
    }

    @SuppressWarnings("unchecked")
    private static BlockEntityType<WaterDamMachineBlockEntity> waterDamControllerType() {
        return (BlockEntityType<WaterDamMachineBlockEntity>) MachineRegistries.WATER_DAM_CONTROLLER.getBlockEntityType();
    }
}
