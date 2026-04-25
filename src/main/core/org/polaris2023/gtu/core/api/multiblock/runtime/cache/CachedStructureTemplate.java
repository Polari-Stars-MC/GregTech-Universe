package org.polaris2023.gtu.core.api.multiblock.runtime.cache;

import org.polaris2023.gtu.core.api.multiblock.runtime.check.CompiledStructureTemplate;

import org.polaris2023.gtu.core.api.multiblock.runtime.check.CompiledStructureTemplate;

public record CachedStructureTemplate(
        CompiledStructureTemplate template,
        long lastAccessTick
) {
    public CachedStructureTemplate touch(long tick) {
        return new CachedStructureTemplate(template, tick);
    }
}
