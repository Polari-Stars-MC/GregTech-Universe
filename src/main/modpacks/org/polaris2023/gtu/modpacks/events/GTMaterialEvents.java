package org.polaris2023.gtu.modpacks.events;

import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.material.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.material.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.material.material.properties.ToolProperty;
import com.gregtechceu.gtceu.data.material.GTMaterials;

public final class GTMaterialEvents {
    private static final GTToolType[] COPPER_TOOL_TYPES = {
            GTToolType.SWORD,
            GTToolType.PICKAXE,
            GTToolType.SHOVEL,
            GTToolType.AXE,
            GTToolType.HOE
    };

    private GTMaterialEvents() {
    }

    public static void addCopperToolStats(PostMaterialEvent event) {
        ToolProperty property = GTMaterials.Copper.getProperty(PropertyKey.TOOL);
        if (property == null) {
            GTMaterials.Copper.setProperty(PropertyKey.TOOL, createCopperToolProperty());
            return;
        }

        property.setHarvestSpeed(2.0F);
        property.setAttackDamage(2.0F);
        property.setDurability(288);
        property.setHarvestLevel(2);
        property.setEnchantability(14);
        property.setTypes(COPPER_TOOL_TYPES);
    }

    private static ToolProperty createCopperToolProperty() {
        return ToolProperty.Builder.of(2.0F, 2.0F, 288, 2, COPPER_TOOL_TYPES)
                .enchantability(14)
                .build();
    }
}
