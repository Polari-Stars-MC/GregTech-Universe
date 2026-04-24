package org.polaris2023.gtu.space.mixin.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.polaris2023.gtu.space.simulation.earth.EarthProjectionHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public abstract class MixinClientChunkCacheEarthProjection {
    @Shadow
    final ClientLevel level;

    MixinClientChunkCacheEarthProjection(ClientLevel level) {
        this.level = level;
    }

    @Unique
    private static final ThreadLocal<Boolean> gtu$projectionGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapLevelChunk(int chunkX, int chunkZ, ChunkStatus chunkStatus, boolean load, CallbackInfoReturnable<LevelChunk> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level) || !EarthProjectionHooks.shouldProjectChunk(level, chunkX, chunkZ)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((ClientChunkCache) (Object) this).getChunk(wrappedChunkX, chunkZ, chunkStatus, load));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapChunkAccess(int chunkX, int chunkZ, ChunkStatus chunkStatus, boolean load, CallbackInfoReturnable<ChunkAccess> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level) || !EarthProjectionHooks.shouldProjectChunk(level, chunkX, chunkZ)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((ClientChunkCache) (Object) this).getChunk(wrappedChunkX, chunkZ, chunkStatus, load));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapIncomingChunk(int chunkX, int chunkZ, FriendlyByteBuf buffer, CompoundTag tag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntityOutput, CallbackInfoReturnable<LevelChunk> cir) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level) || !EarthProjectionHooks.shouldProjectChunk(level, chunkX, chunkZ)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkX);
        if (wrappedChunkX == chunkX) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            cir.setReturnValue(((ClientChunkCache) (Object) this).replaceWithPacketData(wrappedChunkX, chunkZ, buffer, tag, blockEntityOutput));
        } finally {
            gtu$projectionGuard.set(false);
        }
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void gtu$wrapDrop(ChunkPos chunkPos, CallbackInfo ci) {
        if (gtu$projectionGuard.get() || !EarthProjectionHooks.shouldProject(level) || !EarthProjectionHooks.shouldProjectChunk(level, chunkPos.x, chunkPos.z)) {
            return;
        }

        int wrappedChunkX = EarthProjectionHooks.wrapChunkX(chunkPos.x);
        if (wrappedChunkX == chunkPos.x) {
            return;
        }

        gtu$projectionGuard.set(true);
        try {
            ((ClientChunkCache) (Object) this).drop(new ChunkPos(wrappedChunkX, chunkPos.z));
            ci.cancel();
        } finally {
            gtu$projectionGuard.set(false);
        }
    }
}
