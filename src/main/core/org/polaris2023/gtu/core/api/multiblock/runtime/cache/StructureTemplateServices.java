package org.polaris2023.gtu.core.api.multiblock.runtime.cache;

import org.polaris2023.gtu.core.api.multiblock.runtime.cache.StructureCacheService;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureDefinitionBootstrap;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureValidationService;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureTemplateRegistry;

public final class StructureTemplateServices {
    private static final StructureTemplateRegistry REGISTRY = new StructureTemplateRegistry();
    private static final StructureCacheService CACHE = new StructureCacheService(REGISTRY);
    private static final StructureValidationService VALIDATION = new StructureValidationService(CACHE);

    private StructureTemplateServices() {
    }

    public static void bootstrapDefaults() {
        StructureDefinitionBootstrap.bootstrapDefaults(REGISTRY);
    }

    public static StructureTemplateRegistry registry() {
        return REGISTRY;
    }

    public static StructureCacheService cache() {
        return CACHE;
    }

    public static StructureValidationService validation() {
        return VALIDATION;
    }
}
