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
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;

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

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, gtceuItem(item, "copper_pickaxe"))
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("gtceu_unlock_copper_pickaxe", has(Items.COPPER_INGOT))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/copper_pickaxe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, gtceuItem(item, "copper_axe"))
                .pattern("II ")
                .pattern("IS ")
                .pattern(" S ")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("gtceu_unlock_copper_axe", has(Items.COPPER_INGOT))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/copper_axe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, gtceuItem(item, "copper_hoe"))
                .pattern("II ")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("gtceu_unlock_copper_hoe", has(Items.COPPER_INGOT))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/copper_hoe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, gtceuItem(item, "copper_sword"))
                .pattern(" I ")
                .pattern(" I ")
                .pattern(" S ")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("gtceu_unlock_copper_sword", has(Items.COPPER_INGOT))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/copper_sword"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, gtceuItem(item, "copper_shovel"))
                .pattern(" I ")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("gtceu_unlock_copper_shovel", has(Items.COPPER_INGOT))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/copper_shovel"));

        Item createShaft = registryItem(item, "create", "shaft");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistries.DAM_SHAFT.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', createShaft)
                .unlockedBy("gtu_modpacks_unlock_create_shaft", has(createShaft))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistries.WATER_DAM_CONTROLLER.get())
                .pattern("SSS")
                .pattern("PDP")
                .pattern("PPP")
                .define('S', Items.STONE_BRICKS)
                .define('P', gtceuItem(item, "treated_wood_planks"))
                .define('D', BlockRegistries.DAM_SHAFT.get())
                .unlockedBy("gtu_modpacks_unlock_dam_shaft", has(BlockRegistries.DAM_SHAFT.get()))
                .save(recipeOutput);

        for (DamTier tier : DamTier.values()) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistries.getStressHatchByTier(tier).get())
                    .pattern("DDD")
                    .pattern("DCD")
                    .pattern("DDD")
                    .define('D', BlockRegistries.DAM_SHAFT.get())
                    .define('C', tier.getCasingBlock())
                    .unlockedBy("gtu_modpacks_unlock_dam_shaft", has(BlockRegistries.DAM_SHAFT.get()))
                    .save(recipeOutput);
        }
    }

    private static Item gtceuItem(DefaultedRegistry<Item> items, String path) {
        return registryItem(items, "gtceu", path);
    }

    private static Item registryItem(DefaultedRegistry<Item> items, String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        Item item = items.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing item: " + id);
        }
        return item;
    }

}
