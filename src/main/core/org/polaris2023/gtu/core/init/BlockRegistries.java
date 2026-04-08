package org.polaris2023.gtu.core.init;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.polaris2023.gtu.core.GregtechUniverseCore;

public class BlockRegistries {
    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(GregtechUniverseCore.MOD_ID);

    public static final DeferredBlock<CraftingTableBlock> STONE_CRAFTING_TABLE =
            REGISTER.registerBlock("stone_crafting_table", CraftingTableBlock::new);





    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
