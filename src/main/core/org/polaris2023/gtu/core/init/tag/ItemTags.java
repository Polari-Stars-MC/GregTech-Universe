package org.polaris2023.gtu.core.init.tag;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.polaris2023.gtu.core.GregtechUniverseCore;

public class ItemTags {
    public static final TagKey<Item> GRASS_FIBER =
            net.minecraft.tags.ItemTags.create(GregtechUniverseCore.id("grass_fiber"));
    public static final TagKey<Item> STAGE =
            net.minecraft.tags.ItemTags.create(GregtechUniverseCore.id("stage"));
}
