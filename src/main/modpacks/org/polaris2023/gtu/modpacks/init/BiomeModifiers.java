package org.polaris2023.gtu.modpacks.init;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.core.GregtechUniverseCore;

public class BiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, GregtechUniverseCore.MOD_ID);

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
