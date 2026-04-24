package org.polaris2023.gtu.space.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.simulation.SpaceManager;
import org.polaris2023.gtu.space.simulation.earth.EarthSeamlessTravelService;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID)
public final class SpaceSimulationEvents {
    private static final int AUTO_SAVE_INTERVAL = 6000;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        SpaceManager.get(event.getServer()).load();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SpaceManager.get(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(ServerTickEvent.Pre event) {
        SpaceManager manager = SpaceManager.get(event.getServer());
        manager.tick();
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            EarthSeamlessTravelService.wrapPlayerIfNeeded(player);
        }
        tickCounter++;
        if (tickCounter >= AUTO_SAVE_INTERVAL) {
            tickCounter = 0;
            manager.save();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SpaceManager.get(event.getServer()).save();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SpaceManager.shutdown(event.getServer());
    }
}
