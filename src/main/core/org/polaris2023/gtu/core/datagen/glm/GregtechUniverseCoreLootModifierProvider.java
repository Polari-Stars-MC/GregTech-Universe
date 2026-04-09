package org.polaris2023.gtu.core.datagen.glm;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.loot.GrassFiberModifier;
import org.polaris2023.gtu.core.loot.GravelFlintModifier;

import java.util.concurrent.CompletableFuture;

public class GregtechUniverseCoreLootModifierProvider extends GlobalLootModifierProvider {
    public GregtechUniverseCoreLootModifierProvider(PackOutput output,
                                                    CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, GregtechUniverseCore.MOD_ID);
    }

    @Override
    protected void start() {
        // 草纤维掉落 - 矮草
        add("grass_fiber_short_grass", new GrassFiberModifier(
                new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(Blocks.SHORT_GRASS)
                                .build()
                }
        ));

        // 草纤维掉落 - 高草
        add("grass_fiber_tall_grass", new GrassFiberModifier(
                new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(Blocks.TALL_GRASS)
                                .build()
                }
        ));

        // 草纤维掉落 - 蕨
        add("grass_fiber_fern", new GrassFiberModifier(
                new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(Blocks.FERN)
                                .build()
                }
        ));

        // 草纤维掉落 - 大型蕨
        add("grass_fiber_large_fern", new GrassFiberModifier(
                new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(Blocks.LARGE_FERN)
                                .build()
                }
        ));
        add("gravel_flint", new GravelFlintModifier(
                new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(Blocks.GRAVEL)
                                .build()
                }
        ));
    }
}
