package org.polaris2023.gtu.modpacks.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

public class ItemModelProvider extends net.neoforged.neoforge.client.model.generators.ItemModelProvider {
    public ItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GregtechUniverseModPacks.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        reuseParentModel("flint_rope_axe", ResourceLocation.fromNamespaceAndPath("gtceu", "item/tools/axe"));
        reuseParentModel("flint_rope_pickaxe", ResourceLocation.fromNamespaceAndPath("gtceu", "item/tools/pickaxe"));
        reuseParentModel("flint_rope_hoe", ResourceLocation.fromNamespaceAndPath("gtceu", "item/tools/hoe"));
        reuseParentModel("flint_rope_sword", ResourceLocation.fromNamespaceAndPath("gtceu", "item/tools/sword"));
        reuseParentModel("flint_rope_shovel", ResourceLocation.fromNamespaceAndPath("gtceu", "item/tools/shovel"));
    }

    private void reuseParentModel(String name, ResourceLocation parentModel) {
        withExistingParent(name, parentModel);
    }
}
