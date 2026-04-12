package org.polaris2023.gtu.modpacks.mixin.worldgen;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverAquiferProxy;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverTerrainRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NoiseChunk.class, priority = 500)
public abstract class MixinNoiseChunk {
    @Shadow
    protected abstract DensityFunction wrap(DensityFunction densityFunction);

    @Shadow
    public abstract int preliminarySurfaceLevel(int x, int z);

    @Shadow
    @Final
    @Mutable
    private Aquifer aquifer;

    @Shadow
    @Final
    @Mutable
    private NoiseChunk.BlockStateFiller blockStateRule;

    @Shadow
    @Final
    private DensityFunctions.BeardifierOrMarker beardifier;

    @Unique
    private RiverTerrainRuntime gtu$riverTerrain;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void gtu$installRiverTerrain(
            int cellCountXZ,
            RandomState random,
            int firstNoiseX,
            int firstNoiseZ,
            NoiseSettings noiseSettings,
            DensityFunctions.BeardifierOrMarker beardifier,
            NoiseGeneratorSettings noiseGeneratorSettings,
            Aquifer.FluidPicker fluidPicker,
            Blender blendifier,
            CallbackInfo ci
    ) {
        if (!gtu$shouldApplyRivers(noiseGeneratorSettings) || gtu$isRiverHookInstalled()) {
            return;
        }

        NoiseRouter wrappedRouter = random.router().mapAll(this::wrap);
        DensityFunction originalDensity = DensityFunctions.cacheAllInCell(
                DensityFunctions.add(wrappedRouter.finalDensity(), this.beardifier)
        ).mapAll(this::wrap);

        gtu$riverTerrain = new RiverTerrainRuntime(
                random,
                noiseGeneratorSettings.seaLevel(),
                this::preliminarySurfaceLevel,
                wrappedRouter.continents(),
                wrappedRouter.erosion(),
                wrappedRouter.ridges()
        );

        RiverAquiferProxy aquiferProxy = new RiverAquiferProxy(this.aquifer);
        this.aquifer = aquiferProxy;
        NoiseChunk.BlockStateFiller originalRule = this.blockStateRule;
        this.blockStateRule = context -> {
            double carve = gtu$riverTerrain.carve(context);
            if (carve == 0.0) {
                return originalRule.calculate(context);
            }

            double carvedDensity = originalDensity.compute(context) + carve;
            if (carvedDensity > 0.0) {
                return originalRule.calculate(context);
            }

            BlockState riverFluid = gtu$riverTerrain.fluid(context, carvedDensity);
            if (riverFluid != null) {
                aquiferProxy.markRiverFluidUpdate();
                return riverFluid;
            }

            aquiferProxy.clearFluidUpdate();
            return Blocks.AIR.defaultBlockState();
        };
    }

    @Unique
    private boolean gtu$isRiverHookInstalled() {
        return this.aquifer.getClass().getName().endsWith("RiverAquiferProxy");
    }

    @Unique
    private static boolean gtu$shouldApplyRivers(NoiseGeneratorSettings settings) {
        NoiseSettings noise = settings.noiseSettings();
        int minY = noise.minY();
        int maxY = minY + noise.height();
        return settings.defaultFluid().is(Blocks.WATER)
                && settings.seaLevel() > minY
                && settings.seaLevel() < maxY
                && noise.height() >= 256;
    }
}
