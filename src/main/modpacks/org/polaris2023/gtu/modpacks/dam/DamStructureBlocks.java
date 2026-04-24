package org.polaris2023.gtu.modpacks.dam;

import com.gregtechceu.gtceu.api.tag.TagPrefix;
import com.gregtechceu.gtceu.data.block.GTBlocks;
import com.gregtechceu.gtceu.data.block.GTMaterialBlocks;
import com.gregtechceu.gtceu.data.material.GTMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DamStructureBlocks {
    private DamStructureBlocks() {
    }

    public static Block treatedWoodPlanks() {
        return GTBlocks.TREATED_WOOD_PLANK.get();
    }

    public static Block treatedWoodStairs() {
        return GTBlocks.TREATED_WOOD_STAIRS.get();
    }

    public static Block treatedWoodFrame() {
        if (GTMaterialBlocks.MATERIAL_BLOCKS != null) {
            var entry = GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, GTMaterials.TreatedWood);
            if (entry != null) {
                return entry.get();
            }
        }
        return GTBlocks.TREATED_WOOD_FENCE.get();
    }

    public static boolean isBladeBlock(BlockState state) {
        Block block = state.getBlock();
        return block == treatedWoodPlanks()
                || block == treatedWoodStairs()
                || block == treatedWoodFrame();
    }
}
