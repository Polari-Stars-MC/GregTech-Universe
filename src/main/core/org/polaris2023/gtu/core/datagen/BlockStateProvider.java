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

        registerCraftingTable(BlockRegistries.STONE_CRAFTING_TABLE.get(), "stone_crafting_table", GregtechUniverseCore.mid("block/stone"));
    }

    private void registerCraftingTable(Block block, String name, ResourceLocation texture) {
        simpleBlockWithItem(block, CraftingTableModels.craftingTableModel(this, name, texture));
    }
}
