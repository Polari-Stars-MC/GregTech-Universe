package org.polaris2023.gtu.core.api.multiblock.runtime.cache;

import net.minecraft.resources.ResourceLocation;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.CompiledStructureTemplate;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureTemplateCompiler;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureTemplateSource;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StructureCacheService {
    public static final long DEFAULT_TEMPLATE_TTL = 30L * 60L * 20L;

    private final StructureTemplateSource templateSource;
    private final Map<ResourceLocation, CachedStructureTemplate> templateCache = new ConcurrentHashMap<>();
    private final long templateTtlTicks;

    public StructureCacheService(StructureTemplateSource templateSource) {
        this(templateSource, DEFAULT_TEMPLATE_TTL);
    }

    public StructureCacheService(StructureTemplateSource templateSource, long templateTtlTicks) {
        this.templateSource = templateSource;
        this.templateTtlTicks = templateTtlTicks;
    }

    public CompiledStructureTemplate getOrCompile(ResourceLocation machineId, long currentTick) {
        CachedStructureTemplate cached = templateCache.get(machineId);
        if (cached != null) {
            templateCache.put(machineId, cached.touch(currentTick));
            return cached.template();
        }

        CompiledStructureTemplate template = StructureTemplateCompiler.compile(machineId, templateSource.load(machineId));
        templateCache.put(machineId, new CachedStructureTemplate(template, currentTick));
        return template;
    }

    public void evictExpired(long currentTick) {
        Iterator<Map.Entry<ResourceLocation, CachedStructureTemplate>> iterator = templateCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, CachedStructureTemplate> entry = iterator.next();
            if (currentTick - entry.getValue().lastAccessTick() >= templateTtlTicks) {
                iterator.remove();
            }
        }
    }

    public void invalidate(ResourceLocation machineId) {
        templateCache.remove(machineId);
    }

    public void clear() {
        templateCache.clear();
    }
}
