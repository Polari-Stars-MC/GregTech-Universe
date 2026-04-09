package org.polaris2023.gtu.core.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.ItemRegistries;

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

        registerCraftingTable(BlockRegistries.STONE_CRAFTING_TABLE.get(), "stone_crafting_table", GregtechUniverseCore.mid("block/stone"));
        simpleBlockWithItem(BlockRegistries.GRAVEL_COPPER_ORE.get(), models().cubeAll("gravel_copper_ore", modLoc("block/gravel_copper_ore")));
        simpleBlockWithItem(BlockRegistries.GRAVEL_TIN_ORE.get(), models().cubeAll("gravel_tin_ore", modLoc("block/gravel_tin_ore")));
    }

    private void registerCraftingTable(Block block, String name, ResourceLocation texture) {
        simpleBlockWithItem(block, CraftingTableModels.craftingTableModel(this, name, texture));
    }
}
