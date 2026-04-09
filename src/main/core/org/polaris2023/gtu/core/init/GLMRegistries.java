package org.polaris2023.gtu.core.init;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.loot.GrassFiberModifier;
import org.polaris2023.gtu.core.loot.GravelFlintModifier;

public class GLMRegistries {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, GregtechUniverseCore.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<GrassFiberModifier>> GRASS_FIBER =
            REGISTER.register("grass_fiber", () -> GrassFiberModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<GravelFlintModifier>> GRAVEL_FLINT =
            REGISTER.register("gravel_flint", () -> GravelFlintModifier.CODEC);
    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
