package org.polaris2023.gtu.modpacks.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

import java.util.ArrayList;
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
                        filterRecipes.removeIf(holder -> {
                            ResourceLocation id = holder.id();
                            Recipe<?> value = holder.value();
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
                                        default -> {

                                        }
                                    }
                                }
                            }

                            ItemStack result = value.getResultItem(server.getRegistryLookup());
                            if (!result.isEmpty() && result.is(ItemTags.PLANKS)) {
                                return true;
                            }

                            ResourceLocation resultId = result.getItemHolder().unwrapKey()
                                    .map(key -> key.location())
                                    .orElse(null);
                            if (resultId != null && isPlankLike(resultId)) {
                                return true;
                            }

                            return isPlankRecipe(id);
                        });
                        recipeManager.replaceRecipes(filterRecipes);
                        reload.pop();
                    });
        });
    }

    private static boolean isPlankRecipe(ResourceLocation recipeId) {
        String path = recipeId.getPath();
        return path.contains("planks") || path.contains("_plank") || path.endsWith("/plank") || path.endsWith("/planks");
    }

    private static boolean isPlankLike(ResourceLocation itemId) {
        String path = itemId.getPath();
        return path.contains("planks") || path.endsWith("_plank") || path.endsWith("_planks");
    }
}
