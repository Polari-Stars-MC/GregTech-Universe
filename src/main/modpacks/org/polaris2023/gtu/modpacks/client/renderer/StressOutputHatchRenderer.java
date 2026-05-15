package org.polaris2023.gtu.modpacks.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import org.polaris2023.gtu.modpacks.block.StressOutputHatchBlock;
import org.polaris2023.gtu.modpacks.blockentity.StressOutputHatchBlockEntity;

public class StressOutputHatchRenderer extends KineticBlockEntityRenderer<StressOutputHatchBlockEntity> {
    public StressOutputHatchRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(StressOutputHatchBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(StressOutputHatchBlock.FACING);
        Axis axis = facing.getAxis();
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing);
        kineticRotationTransform(shaft, blockEntity, axis,
                getAngleForBe(blockEntity, blockEntity.getBlockPos(), axis), packedLight);
        shaft.translate(facing.getStepX() * 0.1875F, facing.getStepY() * 0.1875F, facing.getStepZ() * 0.1875F);
        shaft.renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
    }
}
