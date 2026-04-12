package org.polaris2023.gtu.physics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = GregtechUniversePhysics.MOD_ID)
public class GregtechUniversePhysicsDatagen {
    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        // TODO: Add datagen providers here
    }
}
