package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;

public interface StructureTemplateSource {
    Collection<StructureNodeDefinition> load(Level level, ResourceLocation machineId);
}
