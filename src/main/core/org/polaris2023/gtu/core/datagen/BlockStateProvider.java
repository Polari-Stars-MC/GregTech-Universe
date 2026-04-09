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

        itemModels().withExistingParent("flint_shard", mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/flint"));
        itemModels().withExistingParent("gravely_copper", mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/raw_copper"));
        itemModels().withExistingParent("gravely_tin", mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/raw_iron"));

        itemModels().basicItem(ItemRegistries.FLINT_SHARE.asItem());

        registerCraftingTable(BlockRegistries.FLINT_CRAFTING_TABLE.get(), "flint_crafting_table", GregtechUniverseCore.mid("block/gravel"));
        registerCraftingTable(BlockRegistries.STONE_CRAFTING_TABLE.get(), "stone_crafting_table", GregtechUniverseCore.mid("block/stone"));
        registerCopperOre(BlockRegistries.GRAVEL_COPPER_ORE.get(), "gravel_copper_ore");
        registerTinOre(BlockRegistries.GRAVEL_TIN_ORE.get(), "gravel_tin_ore");
//        registerCopperOre(BlockRegistries.GRAVEL_COPPER_ORE.get(), "gravel_iron_ore", GregtechUniverseCore.mid("block/gravel"));
    }

    private void registerCraftingTable(Block block, String name, ResourceLocation texture) {
        simpleBlockWithItem(block, CraftingTableModels.craftingTableModel(this, name, texture));
    }

    private void registerCopperOre(Block block, String name) {
        simpleBlock(block, OreModels.oreGravelCopperModel(this, name));
    }

    private void registerIronOre(Block block, String name) {
        simpleBlock(block, OreModels.oreGravelIronModel(this, name));
    }
    private void registerTinOre(Block block, String name) {
        simpleBlock(block, OreModels.oreGravelTinModel(this, name));
    }
}
