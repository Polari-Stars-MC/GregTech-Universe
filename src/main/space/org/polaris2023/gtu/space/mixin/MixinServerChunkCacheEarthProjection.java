package org.polaris2023.gtu.space.mixin;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.polaris2023.gtu.space.simulation.earth.EarthProjectionHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCacheEarthProjection {
    @Shadow public ServerLevel level;

    @Unique
    private static final ThreadLocal<Boolean> gtu$projectionGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapChunkAccess(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<ChunkAccess> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level)) {
            return;
        }

        if (!EarthProjectionHooks.shouldProjectChunk(level, chunkX, chunkZ)) {
            if (!load) {
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
            cir.setReturnValue(((ServerChunkCache) (Object) this).getChunk(wrappedChunkX, chunkZ, requiredStatus, load));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }

    @Inject(method = "getChunkNow", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapChunkNow(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level) || !EarthProjectionHooks.shouldProjectChunk(level, chunkX, chunkZ)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((ServerChunkCache) (Object) this).getChunkNow(wrappedChunkX, chunkZ));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }

    @Inject(method = "hasChunk", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapHasChunk(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level) || !EarthProjectionHooks.shouldProjectChunk(level, chunkX, chunkZ)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((ServerChunkCache) (Object) this).hasChunk(wrappedChunkX, chunkZ));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }
}
