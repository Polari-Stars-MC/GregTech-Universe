package org.polaris2023.gtu.modpacks.datagen;

import com.gregtechceu.gtceu.data.item.GTItems;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.ItemRegistries;

import java.util.concurrent.CompletableFuture;

public class RecipesProvider extends RecipeProvider {
    public RecipesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);

    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        DefaultedRegistry<Item> item = BuiltInRegistries.ITEM;
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_axe")))
                .pattern(" # ")
                .pattern("#A#")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARE)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_axe",has(ItemRegistries.FLINT_SHARE.get()))
                .save(recipeOutput, GregtechUniverseCore.id("shaped/gtceu/flint_axe"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_pickaxe")))
                .pattern("## ")
                .pattern(" A#")
                .pattern("B #")
                .define('#', ItemRegistries.FLINT_SHARE)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_pickaxe",has(ItemRegistries.FLINT_SHARE.get()))
                .save(recipeOutput, GregtechUniverseCore.id("shaped/gtceu/flint_pickaxe"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_hoe")))
                .pattern("## ")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARE)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_hoe",has(ItemRegistries.FLINT_SHARE.get()))
                .save(recipeOutput, GregtechUniverseCore.id("shaped/gtceu/flint_hoe"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_sword")))
                .pattern("  #")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARE)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_sword",has(ItemRegistries.FLINT_SHARE.get()))
                .save(recipeOutput, GregtechUniverseCore.id("shaped/gtceu/flint_sword"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_shovel")))
                .pattern("#  ")
                .pattern(" A ")
                .pattern("  B")
                .define('#', ItemRegistries.FLINT_SHARE)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_shovel",has(ItemRegistries.FLINT_SHARE.get()))
                .save(recipeOutput, GregtechUniverseCore.id("shaped/gtceu/flint_shovel"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_knife")))
                .pattern(" #")
                .pattern("B ")
                .define('#', ItemRegistries.FLINT_SHARE)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_knife",has(ItemRegistries.FLINT_SHARE.get()))
                .save(recipeOutput, GregtechUniverseCore.id("shaped/gtceu/flint_knife"));



    }
}
