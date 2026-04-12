package org.polaris2023.gtu.core.datagen;

import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

import java.util.concurrent.CompletableFuture;

public class RecipesProvider extends RecipeProvider {
    public RecipesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistries.STONE_CRAFTING_TABLE)
                .define('#', Items.COBBLESTONE)
                .pattern("##")
                .pattern("##")
                .unlockedBy("unlock_right_away", PlayerTrigger.TriggerInstance.tick())
                .showNotification(false)
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlockRegistries.FLINT_CRAFTING_TABLE)
                .define('F', Items.FLINT)
                .define('S', Items.STICK)
                .pattern("FF")
                .pattern("SS")
                .unlockedBy("unlock_right_away", PlayerTrigger.TriggerInstance.tick())
                .showNotification(false)
                .save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistries.ROPE)
                .pattern("# #")
                .pattern(" # ")
                .define('#', ItemRegistries.PLANT_FIBER)
                .unlockedBy("has_plant_fiber",has(ItemRegistries.PLANT_FIBER))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistries.UNFIRED_CLAY_BUCKET)
                .pattern("# #")
                .pattern(" # ")
                .define('#', Items.CLAY_BALL)
                .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistries.UNFIRED_CLAY_CAULDRON)
                .pattern("# #")
                .pattern("# #")
                .pattern("###")
                .define('#', Items.CLAY_BALL)
                .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                .save(recipeOutput);

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ItemRegistries.UNFIRED_CLAY_BUCKET), RecipeCategory.TOOLS, ItemRegistries.CLAY_BUCKET, 0.1F, 200)
                .unlockedBy("has_unfired_clay_bucket", has(ItemRegistries.UNFIRED_CLAY_BUCKET))
                .save(recipeOutput, GregtechUniverseCore.id("smelting/clay_bucket"));
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ItemRegistries.UNFIRED_CLAY_BUCKET), RecipeCategory.TOOLS, ItemRegistries.CLAY_BUCKET, 0.1F, 600)
                .unlockedBy("has_unfired_clay_bucket", has(ItemRegistries.UNFIRED_CLAY_BUCKET))
                .save(recipeOutput, GregtechUniverseCore.id("campfire/clay_bucket"));

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ItemRegistries.UNFIRED_CLAY_CAULDRON), RecipeCategory.DECORATIONS, ItemRegistries.CLAY_CAULDRON, 0.1F, 200)
                .unlockedBy("has_unfired_clay_cauldron", has(ItemRegistries.UNFIRED_CLAY_CAULDRON))
                .save(recipeOutput, GregtechUniverseCore.id("smelting/clay_cauldron"));
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ItemRegistries.UNFIRED_CLAY_CAULDRON), RecipeCategory.DECORATIONS, ItemRegistries.CLAY_CAULDRON, 0.1F, 600)
                .unlockedBy("has_unfired_clay_cauldron", has(ItemRegistries.UNFIRED_CLAY_CAULDRON))
                .save(recipeOutput, GregtechUniverseCore.id("campfire/clay_cauldron"));
    }
}
