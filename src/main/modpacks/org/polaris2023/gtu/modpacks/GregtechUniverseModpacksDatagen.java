package org.polaris2023.gtu.modpacks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.polaris2023.gtu.modpacks.datagen.ItemModelProvider;
import org.polaris2023.gtu.modpacks.datagen.RecipesProvider;
import org.polaris2023.gtu.modpacks.datagen.lang.EnUs;
import org.polaris2023.gtu.modpacks.datagen.lang.ZhCn;
import org.polaris2023.gtu.modpacks.datagen.tag.BlockTagProvider;
import org.polaris2023.gtu.modpacks.datagen.tag.ItemTagProvider;

import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class GregtechUniverseModpacksDatagen {
    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        event.createProvider(RecipesProvider::new);
        final AtomicReference<BlockTagProvider> blockTagProvider = new AtomicReference<>();
        event.createProvider((output, future) -> {
            blockTagProvider.set(new BlockTagProvider(output, future, event.getExistingFileHelper()));
            return blockTagProvider.get();
        });
        event.createProvider((packOutput, completableFuture) -> new ItemTagProvider(packOutput, completableFuture, blockTagProvider.get().contentsGetter()));
        event.createProvider(ZhCn::new);
        event.createProvider(EnUs::new);
        event.createProvider(output -> new ItemModelProvider(output, event.getExistingFileHelper()));
    }
}
