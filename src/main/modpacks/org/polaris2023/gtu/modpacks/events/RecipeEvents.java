package org.polaris2023.gtu.modpacks.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class RecipeEvents {
    @SubscribeEvent
    public static void recipeReload(AddReloadListenerEvent event) {
        event.addListener((
                barrier,
                manager,
                filter,
                reload,
                background,
                game
                ) -> {
            return CompletableFuture.runAsync(() -> {
              filter.push("gtu_modpacks");
            }, background)
                    .thenCompose(barrier::wait)
                    .thenRunAsync(() -> {
                        reload.push("gtu_removal");
                        ReloadableServerResources server = event.getServerResources();
                        RecipeManager recipeManager = server.getRecipeManager();
                        List<RecipeHolder<?>> filterRecipes = new ArrayList<>(recipeManager.getRecipes());
                        int totalRemoved = 0;
                        int totalKept = 0;
                        filterRecipes.removeIf(holder -> {
                            ResourceLocation id = holder.id();
                            switch (id.getNamespace()) {
                                case "gtceu" -> {
                                    switch (id.getPath()) {
                                        case "shaped/axe_flint",
                                             "shaped/knife_flint",
                                             "shaped/pickaxe_flint",
                                             "shaped/shovel_flint",
                                             "shaped/hoe_flint",
                                             "shaped/sword_flint"
                                             -> {
                                            return true;
                                        }
                                    }
                                }
                                default -> {

                                }
                            }
                            return false;
                        });
                        recipeManager.replaceRecipes(filterRecipes);
                        reload.pop();
                    });
        });
    }
}
