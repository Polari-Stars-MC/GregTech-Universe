package org.polaris2023.gtu.core.datagen;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public final class CraftingTableModels {
    private CraftingTableModels() {
    }

    public static BlockModelBuilder craftingTableModel(net.neoforged.neoforge.client.model.generators.BlockStateProvider provider,
                                                       String name,
                                                       ResourceLocation texture) {
        return provider.models()
                .cube("block/" + name, texture, texture, texture, texture, texture, texture)
                .texture("particle", texture)
                .parent(craftingTableTemplate(provider, name));
    }

    private static ModelFile craftingTableTemplate(net.neoforged.neoforge.client.model.generators.BlockStateProvider provider,
                                                   String name) {
        return new ModelFile.UncheckedModelFile(
                provider.models().getBuilder("block/" + name + "_template")
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
