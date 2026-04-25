package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public interface StructureTemplateSource {
    Collection<StructureNodeDefinition> load(ResourceLocation machineId);
}
