package org.polaris2023.gtu.space.runtime;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

import java.util.List;

public final class SpaceDimensions {
    public static final int SD_SLOT_COUNT = 8;
    public static final int SD_SHELL_Y = 160;
    public static final ResourceKey<Level> SLOT_00 = sdSlot("sd_slot_00");
    public static final ResourceKey<Level> SLOT_01 = sdSlot("sd_slot_01");
    public static final ResourceKey<Level> SLOT_02 = sdSlot("sd_slot_02");
    public static final ResourceKey<Level> SLOT_03 = sdSlot("sd_slot_03");
    public static final ResourceKey<Level> SLOT_04 = sdSlot("sd_slot_04");
    public static final ResourceKey<Level> SLOT_05 = sdSlot("sd_slot_05");
    public static final ResourceKey<Level> SLOT_06 = sdSlot("sd_slot_06");
    public static final ResourceKey<Level> SLOT_07 = sdSlot("sd_slot_07");
    private static final List<ResourceKey<Level>> SD_SLOTS = List.of(
            SLOT_00, SLOT_01, SLOT_02, SLOT_03,
            SLOT_04, SLOT_05, SLOT_06, SLOT_07
    );

    private SpaceDimensions() {
    }

    public static List<ResourceKey<Level>> sdSlots() {
        return SD_SLOTS;
    }

    public static ResourceKey<Level> sdSlot(String path) {
        return ResourceKey.create(Registries.DIMENSION, GregtechUniverseSpace.id(path));
    }

    public static ResourceKey<Level> dimensionKey(String location) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(location));
    }
}
