package org.polaris2023.gtu.core.datagen;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.polaris2023.gtu.core.GregtechUniverseCore;

public final class OreModels {
    private OreModels() {
    }

    public static BlockModelBuilder oreGravelCopperModel(BlockStateProvider provider,
                                                         String name) {
        return oreGravelModel(provider, name, GregtechUniverseCore.mid("block/gravel"), GregtechUniverseCore.id("block/minecraft/copper_empty"));
    }
    public static BlockModelBuilder oreGravelIronModel(BlockStateProvider provider,
                                                         String name) {
        return oreGravelModel(provider, name, GregtechUniverseCore.mid("block/gravel"), GregtechUniverseCore.id("block/minecraft/iron_empty"));
    }
    public static BlockModelBuilder oreGravelTinModel(BlockStateProvider provider,
                                                         String name) {
        return oreGravelModel(provider, name, GregtechUniverseCore.mid("block/gravel"), GregtechUniverseCore.id("block/minecraft/tin_empty"));
    }


    public static BlockModelBuilder oreGravelModel(BlockStateProvider provider,
                                                   String name,
                                                   ResourceLocation all,
                                                   ResourceLocation ore_all) {
        return provider.models()
                .cubeAll("block/" + name, all)
                .texture("ore_all", ore_all)
                .texture("particle", all)
                .parent(oreGravelModel(provider, name));
    }

    private static ModelFile oreGravelModel(BlockStateProvider provider,
                                            String name) {
        return new ModelFile.UncheckedModelFile(
                provider.models()
                        .getBuilder("block/" + name + "_template")
                        .parent(provider.models().getExistingFile(GregtechUniverseCore.mid("block/block")))
                        .renderType("cutout")
                        .element()
                        .from(0, 0, 0).to(16, 16, 16)
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#all").cullface(Direction.NORTH).end()
                        .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#all").cullface(Direction.EAST).end()
                        .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#all").cullface(Direction.SOUTH).end()
                        .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#all").cullface(Direction.WEST).end()
                        .face(Direction.UP).uvs(0, 0, 16, 16).texture("#all").cullface(Direction.UP).end()
                        .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#all").cullface(Direction.DOWN).end()
                        .end()
                        .element()
                        .from(0, 0, 0).to(16, 16, 16)
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#ore_all").cullface(Direction.NORTH).end()
                        .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#ore_all").cullface(Direction.EAST).end()
                        .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#ore_all").cullface(Direction.SOUTH).end()
                        .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#ore_all").cullface(Direction.WEST).end()
                        .face(Direction.UP).uvs(0, 0, 16, 16).texture("#ore_all").cullface(Direction.UP).end()
                        .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#ore_all").cullface(Direction.DOWN).end()
                        .end()
                        .getLocation()
        );
    }
}
