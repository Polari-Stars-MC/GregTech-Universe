package org.polaris2023.gtu.modpacks;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@Mod(GregtechUniverseModPacks.MOD_ID)
public class GregtechUniverseModPacks {
    public static final String MOD_ID = "gtu_modpacks";


    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
