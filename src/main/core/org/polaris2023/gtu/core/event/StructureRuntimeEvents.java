package org.polaris2023.gtu.core.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.api.multiblock.storage.StructureNetworkManager;

@EventBusSubscriber(modid = GregtechUniverseCore.MOD_ID)
public class StructureRuntimeEvents {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            StructureNetworkManager manager = StructureNetworkManager.get(level);
            String threadName = "gtu-structure-runtime-" + level.dimension().location();
            manager.startRuntime(threadName.replace(':', '_'));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            StructureNetworkManager manager = StructureNetworkManager.get(level);
            manager.flushRuntimeDelta(manager.runtime().drainOutput());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StructureNetworkManager.clearAll();
    }
}
