package org.polaris2023.gtu.modpacks.mixin;

import com.gregtechceu.gtceu.api.material.material.properties.ToolProperty;
import com.gregtechceu.gtceu.data.material.SecondDegreeMaterials;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SecondDegreeMaterials.class)
public class MixinSecondDegreeMaterials {
    @ModifyExpressionValue(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/gregtechceu/gtceu/api/material/material/properties/ToolProperty$Builder;build()Lcom/gregtechceu/gtceu/api/material/material/properties/ToolProperty;")
    )
    private static ToolProperty a(ToolProperty original) {
        if (original.getDurability() == 64) {
            original.setDurability(15);
        }
        return original;
    }
}
