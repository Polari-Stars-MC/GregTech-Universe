package org.polaris2023.gtu.modpacks.client;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.polaris2023.gtu.modpacks.CopperToolVeinMining;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.polaris2023.gtu.modpacks.init.tag.BlockTags;

import java.util.List;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID, value = Dist.CLIENT)
public final class CopperToolHighlightRenderer {
    private static final double BOX_GROWTH = 0.003D;
    private static final float RED = 0.90F;
    private static final float GREEN = 0.48F;
    private static final float BLUE = 0.16F;
    private static final float ALPHA = 0.95F;

    private CopperToolHighlightRenderer() {
    }

    @SubscribeEvent
    public static void renderHighlights(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !CopperToolKeyMappings.VEIN_MINE_KEY.get().isDown()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (!CopperToolVeinMining.isCopperTool(tool)) {
            return;
        }

        BlockHitResult hit = ToolHelper.getPlayerDefaultRaytrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos origin = hit.getBlockPos();
        BlockState originState = level.getBlockState(origin);
        if (originState.isAir() || !tool.isCorrectToolForDrops(originState)) {
            return;
        }

        List<BlockPos> targets = CopperToolVeinMining.collectTargets(
                level,
                origin,
                originState,
                tool,
                state -> canBreak(player, state)
        );
        if (targets.size() <= 1) {
            return;
        }

        renderTargetBoxes(event.getPoseStack(), event.getCamera(), minecraft.renderBuffers().bufferSource(), targets);
    }

    private static boolean canBreak(Player player, BlockState state) {
        if (player.isCreative() || state.isAir()) {
            return true;
        }
        if (state.is(BlockTags.WHITE_LIST_BREAK)) {
            return true;
        }
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && stack.isCorrectToolForDrops(state);
    }

    private static void renderTargetBoxes(PoseStack poseStack, Camera camera, MultiBufferSource.BufferSource bufferSource,
                                          List<BlockPos> targets) {
        Vec3 cameraPosition = camera.getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0F);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        for (BlockPos target : targets) {
            AABB box = new AABB(target).inflate(BOX_GROWTH);
            LevelRenderer.renderLineBox(poseStack, consumer, box, RED, GREEN, BLUE, ALPHA);
        }
        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.disableBlend();
    }
}
