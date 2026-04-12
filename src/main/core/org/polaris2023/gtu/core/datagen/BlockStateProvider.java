package org.polaris2023.gtu.core.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
        generatedItem("washed_copper_concentrate", mcLoc("item/raw_copper"));
        generatedItem("washed_tin_concentrate", mcLoc("item/raw_iron"));
        generatedItem("washed_iron_concentrate", mcLoc("item/raw_iron"));
        generatedItem("rope", mcLoc("item/string"));
        itemModels().basicItem(ItemRegistries.UNFIRED_CLAY_BUCKET.asItem());
        itemModels().basicItem(ItemRegistries.CLAY_BUCKET.asItem());
        itemModels().basicItem(ItemRegistries.WATER_CLAY_BUCKET.asItem());
        unfiredCauldronItemModel();

        registerCraftingTable(BlockRegistries.FLINT_CRAFTING_TABLE.get(), "flint_crafting_table", GregtechUniverseCore.mid("block/gravel"));
        registerCraftingTable(BlockRegistries.STONE_CRAFTING_TABLE.get(), "stone_crafting_table", GregtechUniverseCore.mid("block/stone"));
        registerCopperOre(BlockRegistries.GRAVEL_COPPER_ORE.get(), "gravel_copper_ore");
        registerTinOre(BlockRegistries.GRAVEL_TIN_ORE.get(), "gravel_tin_ore");
        registerIronOre(BlockRegistries.GRAVEL_IRON_ORE.get(), "gravel_iron_ore");
        registerClayCauldrons();
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

    private void registerClayCauldrons() {
        ModelFile emptyModel = models().withExistingParent("block/clay_cauldron", mcLoc("block/cauldron"))
                .texture("particle", GregtechUniverseCore.id("block/clay_cauldron_side"))
                .texture("top", GregtechUniverseCore.id("block/clay_cauldron_top"))
                .texture("bottom", GregtechUniverseCore.id("block/clay_cauldron_bottom"))
                .texture("side", GregtechUniverseCore.id("block/clay_cauldron_side"))
                .texture("inside", GregtechUniverseCore.id("block/clay_cauldron_inner"));
        simpleBlockWithItem(BlockRegistries.CLAY_CAULDRON.get(), emptyModel);

        ModelFile level1 = waterCauldronModel("block/water_clay_cauldron_level1", mcLoc("block/template_cauldron_level1"));
        ModelFile level2 = waterCauldronModel("block/water_clay_cauldron_level2", mcLoc("block/template_cauldron_level2"));
        ModelFile level3 = waterCauldronModel("block/water_clay_cauldron_full", mcLoc("block/template_cauldron_full"));

        getVariantBuilder(BlockRegistries.WATER_CLAY_CAULDRON.get())
                .partialState().with(LayeredCauldronBlock.LEVEL, 1).modelForState().modelFile(level1).addModel()
                .partialState().with(LayeredCauldronBlock.LEVEL, 2).modelForState().modelFile(level2).addModel()
                .partialState().with(LayeredCauldronBlock.LEVEL, 3).modelForState().modelFile(level3).addModel();
    }

    private ModelFile waterCauldronModel(String name, ResourceLocation parent) {
        return models().withExistingParent(name, parent)
                .texture("bottom", GregtechUniverseCore.id("block/clay_cauldron_bottom"))
                .texture("content", mcLoc("block/water_still"))
                .texture("inside", GregtechUniverseCore.id("block/clay_cauldron_inner"))
                .texture("particle", GregtechUniverseCore.id("block/clay_cauldron_side"))
                .texture("side", GregtechUniverseCore.id("block/clay_cauldron_side"))
                .texture("top", GregtechUniverseCore.id("block/clay_cauldron_top"));
    }

    private void unfiredCauldronItemModel() {
        itemModels().withExistingParent("unfired_clay_cauldron", mcLoc("block/cauldron"))
                .texture("particle", GregtechUniverseCore.id("block/unfired_clay_cauldron_side"))
                .texture("top", GregtechUniverseCore.id("block/unfired_clay_cauldron_top"))
                .texture("bottom", GregtechUniverseCore.id("block/unfired_clay_cauldron_bottom"))
                .texture("side", GregtechUniverseCore.id("block/unfired_clay_cauldron_side"))
                .texture("inside", GregtechUniverseCore.id("block/unfired_clay_cauldron_inner"));
    }
}
