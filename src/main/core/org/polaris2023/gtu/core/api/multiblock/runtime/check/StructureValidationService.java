package org.polaris2023.gtu.core.api.multiblock.runtime.check;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.polaris2023.gtu.core.api.multiblock.runtime.cache.StructureCacheService;

public class StructureValidationService {
    private final StructureCacheService cacheService;

    public StructureValidationService(StructureCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public StructureValidationResult validate(Level level, BlockPos controllerPos, ResourceLocation machineId, long currentTick) {
        CompiledStructureTemplate template = cacheService.getOrCompile(level, machineId, currentTick);
        return StructureValidator.validate(level, controllerPos, template);
    }

    public void sweep(long currentTick) {
        cacheService.evictExpired(currentTick);
    }

    public void invalidate(ResourceLocation machineId) {
        cacheService.invalidate(machineId);
    }
}
