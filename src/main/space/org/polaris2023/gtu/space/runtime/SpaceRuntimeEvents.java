package org.polaris2023.gtu.space.runtime;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID)
public final class SpaceRuntimeEvents {
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        SpaceManager.get(event.getServer()).startSystems();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SpaceManager.get(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        SpaceManager.get(event.getServer()).tick();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SpaceManager.shutdown(event.getServer());
    }
}
