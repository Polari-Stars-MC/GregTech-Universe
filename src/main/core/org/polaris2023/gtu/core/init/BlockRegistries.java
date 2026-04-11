package org.polaris2023.gtu.core.init;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.block.FlintCraftingTableBlock;
import org.polaris2023.gtu.core.block.GravelOreBlock;

public class BlockRegistries {
    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(GregtechUniverseCore.MOD_ID);

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
    public static final DeferredBlock<GravelOreBlock> GRAVEL_IRON_ORE =
            REGISTER.registerBlock("gravel_iron_ore", GravelOreBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL)
                            .strength(0.6F)
                            .sound(SoundType.GRAVEL)
                            .requiresCorrectToolForDrops());

    public static final DeferredBlock<FlintCraftingTableBlock> FLINT_CRAFTING_TABLE =
            REGISTER
                    .registerBlock("flint_crafting_table", properties -> new FlintCraftingTableBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.CRAFTING_TABLE).noCollission()))
            ;
    ;





    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
