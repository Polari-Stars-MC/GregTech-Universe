package org.polaris2023.gtu.modpacks.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamMachineBlockEntity;

@OnlyIn(Dist.CLIENT)
public class DamWheelRenderer implements BlockEntityRenderer<WaterDamMachineBlockEntity> {
    public DamWheelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaterDamMachineBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Intentionally empty.
        // Water dam wheels must be shown only by Create contraption entities so the
        // world blocks are not double-rendered alongside a client-side fallback.
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(WaterDamMachineBlockEntity blockEntity) {
        return true;
    }
}
