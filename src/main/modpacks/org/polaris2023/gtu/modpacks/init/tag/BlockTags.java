package org.polaris2023.gtu.modpacks.init.tag;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

public class BlockTags {
    public static final TagKey<Block> WHITE_LIST_BREAK =
            net.minecraft.tags.BlockTags.create(GregtechUniverseModPacks.id("white_list_break"));
    public static final TagKey<Block> INCORRECT_FOR_FLINT_TOOL = net.minecraft.tags.BlockTags.create(GregtechUniverseModPacks.id("incorrect_for_flint_tool"));
}
