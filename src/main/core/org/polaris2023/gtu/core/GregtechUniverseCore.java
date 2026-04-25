package org.polaris2023.gtu.core;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.polaris2023.gtu.core.block.ClayCauldronInteractions;
import org.polaris2023.gtu.core.api.multiblock.runtime.cache.StructureTemplateServices;
import org.polaris2023.gtu.core.init.AttachmentRegistries;
import org.polaris2023.gtu.core.init.BlockEntityRegistries;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ClayCauldronFluidRegistries;
import org.polaris2023.gtu.core.init.CreativeTabRegistries;
import org.polaris2023.gtu.core.init.GLMRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;
import org.polaris2023.gtu.core.init.MenuRegistries;

@Mod(GregtechUniverseCore.MOD_ID)
public class GregtechUniverseCore {
    public static final String MOD_ID = "gtu_core";



    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation cid(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }

    public static ResourceLocation mid(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    public GregtechUniverseCore(IEventBus modBus) {
        AttachmentRegistries.register(modBus);
        BlockEntityRegistries.register(modBus);
        BlockRegistries.register(modBus);
        ItemRegistries.register(modBus);
        modBus.addListener(GregtechUniverseCore::commonSetup);
        modBus.addListener(ClayCauldronFluidRegistries::register);
        CreativeTabRegistries.register(modBus);
        MenuRegistries.register(modBus);
        GLMRegistries.register(modBus);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ClayCauldronInteractions.bootstrap();
            StructureTemplateServices.bootstrapDefaults();
        });
    }
}
