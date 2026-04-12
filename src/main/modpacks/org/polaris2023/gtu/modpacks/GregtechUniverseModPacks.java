package org.polaris2023.gtu.modpacks;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.polaris2023.gtu.modpacks.data.DataAttachmentTypes;
import org.polaris2023.gtu.modpacks.init.BiomeModifiers;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

@Mod(GregtechUniverseModPacks.MOD_ID)
public class GregtechUniverseModPacks {
    public static final String MOD_ID = "gtu_modpacks";

    public GregtechUniverseModPacks(IEventBus modEventBus) {
        BlockRegistries.register(modEventBus);
        ItemRegistries.register(modEventBus);
        BiomeModifiers.register(modEventBus);
        DataAttachmentTypes.register(modEventBus);

    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
