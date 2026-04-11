package org.polaris2023.gtu.modpacks.datagen;

import com.gregtechceu.gtceu.data.item.GTItems;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.ItemRegistries;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.GregtechUniverseModpacksDatagen;

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
                .unlockedBy("gtceu_unlock_fint_share_axe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_axe"));

        ItemStack stack = item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_axe")).getDefaultInstance();
        stack.set(DataComponents.MAX_DAMAGE, (int) (stack.getMaxDamage() * 1.2F));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_axe"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, stack)
                .pattern(" # ")
                .pattern("#A#")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_axe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_rope_axe"));


        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_pickaxe")))
                .pattern("## ")
                .pattern(" A#")
                .pattern("B #")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_pickaxe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_pickaxe"));
        ItemStack stack1 = item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_pickaxe")).getDefaultInstance();
        stack1.set(DataComponents.MAX_DAMAGE, (int) (stack1.getMaxDamage() * 1.2F));
        stack1.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_pickaxe"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, stack1)
                .pattern("## ")
                .pattern(" A#")
                .pattern("B #")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_pickaxe",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_rope_pickaxe"));


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
        ItemStack stack2 = item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_hoe")).getDefaultInstance();
        stack2.set(DataComponents.DAMAGE, (int) (stack2.getMaxDamage() * 1.2F));
        stack2.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_hoe"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, stack2)
                .pattern("## ")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
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
        ItemStack stack3 = item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_sword")).getDefaultInstance();
        stack3.set(DataComponents.DAMAGE, (int) (stack3.getMaxDamage() * 1.2F));
        stack3.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_sword"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, stack3)
                .pattern("  #")
                .pattern(" A ")
                .pattern("B  ")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_sword",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_rope_sword"));


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
        ItemStack stack4 = item.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_shovel")).getDefaultInstance();
        stack4.set(DataComponents.DAMAGE, (int) (stack4.getMaxDamage() * 1.2F));
        stack4.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_shovel"));
        ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, stack4)
                .pattern("#  ")
                .pattern(" A ")
                .pattern("  B")
                .define('#', ItemRegistries.FLINT_SHARD)
                .define('A', ItemRegistries.PLANT_FIBER)
                .define('B', Items.STICK)
                .unlockedBy("gtceu_unlock_fint_share_shovel",has(ItemRegistries.FLINT_SHARD.get()))
                .save(recipeOutput, GregtechUniverseModPacks.id("shaped/gtceu/flint_rope_shovel"));


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
