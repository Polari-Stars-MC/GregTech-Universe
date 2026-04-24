package org.polaris2023.gtu.space.mixin;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.apache.commons.lang3.mutable.MutableObject;
import org.polaris2023.gtu.space.simulation.earth.EarthBounds;
import org.polaris2023.gtu.space.simulation.earth.EarthProjection;
import org.polaris2023.gtu.space.simulation.earth.EarthProjectionService;
import org.polaris2023.gtu.space.simulation.earth.EarthWorldgenBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinNoiseBasedChunkGeneratorEarthProjection {
    @Shadow
    protected abstract OptionalInt iterateNoiseColumn(LevelHeightAccessor heightAccessor, RandomState randomState, int x, int z, MutableObject<NoiseColumn> noiseColumn, Predicate<BlockState> stopPredicate);

    @Inject(method = "getBaseHeight", at = @At("HEAD"), cancellable = true)
    private void gtu$projectBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor heightAccessor, RandomState randomState, CallbackInfoReturnable<Integer> cir) {
        if (EarthBounds.isOutsideProjectedEarth(x, z)) {
            cir.setReturnValue(heightAccessor.getMinBuildHeight());
            return;
        }

        int wrappedX = EarthProjection.longitudeDegreesToBlockX(EarthProjection.xToLongitudeDegrees(x));
        int clampedZ = EarthBounds.clampBlockZ(z);
        if (wrappedX == x && clampedZ == z) {
            return;
        }

        cir.setReturnValue(
                iterateNoiseColumn(heightAccessor, randomState, wrappedX, clampedZ, null, heightmapType.isOpaque())
                        .orElse(heightAccessor.getMinBuildHeight())
        );
    }

    @Inject(method = "getBaseColumn", at = @At("HEAD"), cancellable = true)
    private void gtu$projectBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState, CallbackInfoReturnable<NoiseColumn> cir) {
        if (EarthBounds.isOutsideProjectedEarth(x, z)) {
            cir.setReturnValue(createOutOfBoundsColumn(heightAccessor));
            return;
        }

        int wrappedX = EarthProjection.longitudeDegreesToBlockX(EarthProjection.xToLongitudeDegrees(x));
        int clampedZ = EarthBounds.clampBlockZ(z);
        if (wrappedX == x && clampedZ == z) {
            return;
        }

        MutableObject<NoiseColumn> column = new MutableObject<>();
        iterateNoiseColumn(heightAccessor, randomState, wrappedX, clampedZ, column, null);
        cir.setReturnValue(column.getValue());
    }

    @Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
    private void gtu$skipOutOfBoundsNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (!EarthWorldgenBridge.shouldGenerateChunk(chunk.getPos().x, chunk.getPos().z)) {
            cir.setReturnValue(CompletableFuture.completedFuture(chunk));
        }
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("HEAD"), cancellable = true)
    private void gtu$skipOutOfBoundsSurface(net.minecraft.server.level.WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk, CallbackInfo ci) {
        if (!EarthWorldgenBridge.shouldGenerateChunk(chunk.getPos().x, chunk.getPos().z)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    private void gtu$skipOutOfBoundsCarvers(net.minecraft.server.level.WorldGenRegion region, long seed, RandomState randomState, net.minecraft.world.level.biome.BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving carving, CallbackInfo ci) {
        if (!EarthWorldgenBridge.shouldGenerateChunk(chunk.getPos().x, chunk.getPos().z)) {
            ci.cancel();
        }
    }

    private static NoiseColumn createOutOfBoundsColumn(LevelHeightAccessor heightAccessor) {
        BlockState state = EarthProjectionService.get().config().outOfBoundsPolicy().fallbackBlockState();
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for (int i = 0; i < states.length; i++) {
            states[i] = state;
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }
}
