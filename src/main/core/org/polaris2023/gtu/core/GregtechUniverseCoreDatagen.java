package org.polaris2023.gtu.core;

import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.polaris2023.gtu.core.datagen.DatapackBuiltinEntriesProvider;
import org.polaris2023.gtu.core.datagen.LootProvider;
import org.polaris2023.gtu.core.datagen.RecipesProvider;
import org.polaris2023.gtu.core.datagen.glm.GregtechUniverseCoreLootModifierProvider;
import org.polaris2023.gtu.core.datagen.lang.*;
import org.polaris2023.gtu.core.datagen.BlockStateProvider;
import org.polaris2023.gtu.core.datagen.tag.BlockTagProvider;
import org.polaris2023.gtu.core.datagen.tag.ItemTagProvider;

import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber(modid = GregtechUniverseCore.MOD_ID)
public class GregtechUniverseCoreDatagen {

    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        
        event.createProvider(EnUs::new);
        event.createProvider(ZhCnFixed::new);
        event.createProvider(GregtechUniverseCoreLootModifierProvider::new);
        event.createProvider(output -> new BlockStateProvider(output, event.getExistingFileHelper()));
        event.createProvider(RecipesProvider::new);
        AtomicReference<BlockTagProvider> blockTagProvider = new AtomicReference<>();
        event.createProvider((output, future) -> {
            blockTagProvider.set(new BlockTagProvider(output, future, event.getExistingFileHelper()));
            return blockTagProvider.get();
        });
        event.createProvider((output, future) -> new ItemTagProvider(output, future, blockTagProvider.get().contentsGetter()));
        event.createProvider(LootProvider::new);
        event.createProvider(DatapackBuiltinEntriesProvider::new);
    }
}
