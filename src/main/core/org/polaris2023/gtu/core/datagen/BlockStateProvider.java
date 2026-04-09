package org.polaris2023.gtu.core.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
        // 物品模型
        itemModels().basicItem(ItemRegistries.PLANT_FIBER.asItem());
        itemModels().basicItem(ItemRegistries.FLINT_SHARE.asItem());

        // 石制工作台 - 使用自定义模型
        ModelFile modelFile = craftingTableModel();
        BlockModelBuilder stone_crafting_table = models()
                .cube("block/stone_crafting_table",
                        GregtechUniverseCore.mid("block/stone"),
                        GregtechUniverseCore.mid("block/stone"),
                        GregtechUniverseCore.mid("block/stone"),
                        GregtechUniverseCore.mid("block/stone"),
                        GregtechUniverseCore.mid("block/stone"),
                        GregtechUniverseCore.mid("block/stone")
                ).parent(modelFile);
        simpleBlockWithItem(BlockRegistries.STONE_CRAFTING_TABLE.get(), stone_crafting_table);
    }

    /**
     * 生成工作台的自定义模型（两个重叠 element + 自定义纹理）
     */
    private ModelFile craftingTableModel() {
        // 自定义模型无法用标准 API 生成，通过内部 Builder 直接输出 JSON
        return new ModelFile.UncheckedModelFile(
                models().getBuilder("minecraft/default_crafting_table")
                        .renderType("cutout")
                        .texture("east_south", "gtu_core:block/minecraft/empty_crafting_table_side")
                        .texture("north_west", "gtu_core:block/minecraft/empty_crafting_table_front")
                        .texture("up_empty", "gtu_core:block/minecraft/empty_crafting_table_top")
                        .texture("north", "#north_west")
                        .texture("south", "#east_south")
                        .texture("east", "#east_south")
                        .texture("west", "#north_west")
                        .texture("up", "#up_empty")
                        .texture("down", "#missing")
                        .element()
                        .from(0, 0, 0).to(16, 16, 16)
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#north").cullface(Direction.NORTH).end()
                        .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#east").cullface(Direction.EAST).end()
                        .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#south").cullface(Direction.SOUTH).end()
                        .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#west").cullface(Direction.WEST).end()
                        .face(Direction.UP).uvs(0, 0, 16, 16).texture("#up").cullface(Direction.UP).end()
                        .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#down").cullface(Direction.DOWN).end()
                        .end()
                        .element()
                        .from(0, 0, 0).to(16, 16, 16)
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#north_west").cullface(Direction.NORTH).end()
                        .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#east_south").cullface(Direction.EAST).end()
                        .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#east_south").cullface(Direction.SOUTH).end()
                        .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#north_west").cullface(Direction.WEST).end()
                        .face(Direction.UP).uvs(0, 0, 16, 16).texture("#up_empty").cullface(Direction.UP).end()
                        .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#missing").cullface(Direction.DOWN).end()
                        .end()
                        .getLocation()
        );
    }
}
