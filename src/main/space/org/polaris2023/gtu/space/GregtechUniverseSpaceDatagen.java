package org.polaris2023.gtu.space;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID)
public class GregtechUniverseSpaceDatagen {
    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        // TODO: Add datagen providers here
    }
}
