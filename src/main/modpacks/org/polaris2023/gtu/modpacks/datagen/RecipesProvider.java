package org.polaris2023.gtu.modpacks.datagen;

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
import org.polaris2023.gtu.core.init.ItemRegistries;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

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
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_shard_axe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_axe"));


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, org.polaris2023.gtu.modpacks.init.ItemRegistries.FLINT_ROPE_AXE)
                .pattern(" # ")
                .pattern("#A#")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.ROPE)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_rope_axe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput);


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_pickaxe")))
                .pattern("## ")
                .pattern(" A#")
                .pattern("B #")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_shard_pickaxe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_pickaxe"));

        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, org.polaris2023.gtu.modpacks.init.ItemRegistries.FLINT_ROPE_PICKAXE)
                .pattern("## ")
                .pattern(" A#")
                .pattern("B #")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.ROPE)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_rope_pickaxe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput);


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_hoe")))
                .pattern("## ")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_hoe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_hoe"));

        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, org.polaris2023.gtu.modpacks.init.ItemRegistries.FLINT_ROPE_HOE)
                .pattern("## ")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.ROPE)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_hoe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_rope_hoe"));


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_sword")))
                .pattern("  #")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_sword",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_sword"));

        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, org.polaris2023.gtu.modpacks.init.ItemRegistries.FLINT_ROPE_SWORD)
                .pattern("  #")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.ROPE)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_rope_sword",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput);


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_shovel")))
                .pattern("#  ")
                .pattern(" A ")
                .pattern("  B")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_shovel",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_shovel"));

        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, org.polaris2023.gtu.modpacks.init.ItemRegistries.FLINT_ROPE_SHOVEL)
                .pattern("#  ")
                .pattern(" A ")
                .pattern("  B")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.ROPE)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_shovel",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput);


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_knife")))
                .pattern(" #")
                .pattern("B ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_knife",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_knife"));


    }
}
