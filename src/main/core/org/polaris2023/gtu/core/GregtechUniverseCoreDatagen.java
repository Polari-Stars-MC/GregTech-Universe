package org.polaris2023.gtu.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.polaris2023.gtu.core.datagen.glm.GregtechUniverseCoreLootModifierProvider;
import org.polaris2023.gtu.core.datagen.lang.EnUs;
import org.polaris2023.gtu.core.datagen.BlockStateProvider;

@EventBusSubscriber(modid = GregtechUniverseCore.MOD_ID)
public class GregtechUniverseCoreDatagen {
    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        // GLM 数据生成
        event.createProvider(EnUs::new);
        event.createProvider(GregtechUniverseCoreLootModifierProvider::new);
        event.createProvider(output -> new BlockStateProvider(output, event.getExistingFileHelper()));
    }
}
