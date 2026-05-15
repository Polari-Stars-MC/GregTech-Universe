package org.polaris2023.gtu.modpacks.compat;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.MachineRegistries;

import java.util.List;

@JeiPlugin
public final class WaterDamJeiPlugin implements IModPlugin {
    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return GregtechUniverseModPacks.id("water_dam_jei_plugin");
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        MultiblockMachineDefinition waterDam = MachineRegistries.WATER_DAM_CONTROLLER;
        boolean alreadyRegistered = GTRegistries.MACHINES.stream().anyMatch(waterDam::equals);
        if (!alreadyRegistered) {
            registration.addRecipes(MultiblockInfoCategory.RECIPE_TYPE, List.of(waterDam));
        }
    }
}
