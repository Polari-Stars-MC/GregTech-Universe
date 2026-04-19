package org.polaris2023.gtu.space.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.polaris2023.gtu.space.simulation.earth.EarthProjectionHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class MixinWorldGenRegionEarthProjection {
    @Shadow public abstract ServerLevel getLevel();

    @Unique
    private static final ThreadLocal<Boolean> gtu$projectionGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getChunk(II)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapChunk(int chunkX, int chunkZ, CallbackInfoReturnable<ChunkAccess> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(getLevel()) || !EarthProjectionHooks.shouldProjectChunk(getLevel(), chunkX, chunkZ)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((WorldGenRegion) (Object) this).getChunk(wrappedChunkX, chunkZ));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapChunkStatus(int chunkX, int chunkZ, ChunkStatus chunkStatus, boolean required, CallbackInfoReturnable<ChunkAccess> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(getLevel())) {
            return;
        }

        if (!EarthProjectionHooks.shouldProjectChunk(getLevel(), chunkX, chunkZ)) {
            if (!required) {
                cir.setReturnValue(null);
            }
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((WorldGenRegion) (Object) this).getChunk(wrappedChunkX, chunkZ, chunkStatus, required));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }
}
