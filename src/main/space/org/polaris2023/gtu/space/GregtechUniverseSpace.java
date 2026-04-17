package org.polaris2023.gtu.space;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.polaris2023.gtu.space.init.SpaceBootstrap;

@Mod(GregtechUniverseSpace.MOD_ID)
public class GregtechUniverseSpace {
    public static final String MOD_ID = "gtu_space";

    public GregtechUniverseSpace(IEventBus modEventBus) {
        SpaceBootstrap.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
