package org.polaris2023.gtu.core.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

@SuppressWarnings("SameParameterValue")
public class BlockStateProvider extends net.neoforged.neoforge.client.model.generators.BlockStateProvider {
    public BlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, GregtechUniverseCore.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        itemModels().basicItem(ItemRegistries.PLANT_FIBER.asItem());
        itemModels().basicItem(ItemRegistries.FLINT_SHARD.asItem());
        generatedItem("stone_shard", mcLoc("block/stone"));
        generatedItem("gravely_copper", mcLoc("item/raw_copper"));
        generatedItem("gravely_tin", mcLoc("item/raw_iron"));
        generatedItem("gravely_iron", mcLoc("item/raw_iron"));
        generatedItem("rope", mcLoc("item/string"));

        registerCraftingTable(BlockRegistries.FLINT_CRAFTING_TABLE.get(), "flint_crafting_table", GregtechUniverseCore.mid("block/gravel"));
        registerCraftingTable(BlockRegistries.STONE_CRAFTING_TABLE.get(), "stone_crafting_table", GregtechUniverseCore.mid("block/stone"));
        registerCopperOre(BlockRegistries.GRAVEL_COPPER_ORE.get(), "gravel_copper_ore");
        registerTinOre(BlockRegistries.GRAVEL_TIN_ORE.get(), "gravel_tin_ore");
        registerIronOre(BlockRegistries.GRAVEL_IRON_ORE.get(), "gravel_iron_ore");
    }

    private void registerCraftingTable(Block block, String name, ResourceLocation texture) {
        simpleBlockWithItem(block, CraftingTableModels.craftingTableModel(this, name, texture));
    }

    private void generatedItem(String name, ResourceLocation texture) {
        itemModels().withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", texture);
    }

    private void registerCopperOre(Block block, String name) {
        simpleBlockWithItem(block, OreModels.oreGravelCopperModel(this, name));
    }

    private void registerIronOre(Block block, String name) {
        simpleBlockWithItem(block, OreModels.oreGravelIronModel(this, name));
    }

    private void registerTinOre(Block block, String name) {
        simpleBlockWithItem(block, OreModels.oreGravelTinModel(this, name));
    }
}
