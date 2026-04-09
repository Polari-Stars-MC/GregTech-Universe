package org.polaris2023.gtu.core.init;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.block.FlintCraftingTableBlock;
import org.polaris2023.gtu.core.block.GravelOreBlock;
import org.polaris2023.gtu.core.datagen.CraftingTableModels;

public class BlockRegistries {
    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(GregtechUniverseCore.MOD_ID);
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(GregtechUniverseCore.MOD_ID);

    public static final DeferredBlock<CraftingTableBlock> STONE_CRAFTING_TABLE =
            REGISTER.registerBlock("stone_crafting_table", CraftingTableBlock::new);

    public static final DeferredBlock<GravelOreBlock> GRAVEL_COPPER_ORE =
            REGISTER.registerBlock("gravel_copper_ore", GravelOreBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL)
                            .strength(0.6F)
                            .sound(SoundType.GRAVEL)
                            .requiresCorrectToolForDrops());

    public static final DeferredBlock<GravelOreBlock> GRAVEL_TIN_ORE =
            REGISTER.registerBlock("gravel_tin_ore", GravelOreBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL)
                            .strength(0.6F)
                            .sound(SoundType.GRAVEL)
                            .requiresCorrectToolForDrops());

    public static final BlockEntry<FlintCraftingTableBlock> FLINT_CRAFTING_TABLE =
            REGISTRATE.block("flint_crafting_table", FlintCraftingTableBlock::new)
                    .initialProperties(() -> Blocks.CRAFTING_TABLE)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate((ctx, prov) -> prov.simpleBlock(
                            ctx.getEntry(),
                            CraftingTableModels.craftingTableModel(
                                    prov,
                                    ctx.getName(),
                                    GregtechUniverseCore.mid("block/gray_concrete")
                            )
                    ))
                    .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                    .simpleItem()
                    .register();





    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
