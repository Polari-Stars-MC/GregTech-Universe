package org.polaris2023.gtu.core.worldgen.river;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.function.IntBinaryOperator;

/**
 * 真实水系地形运行时 - 通过多层噪声叠加模拟树状水系(dendritic drainage)。
 *
 * 雪山/高山顶部1格泉眼发源地 -> 主干逐渐变宽 -> 一级/二级支流汇入
 * 河床(砂砾底) + 河堤(两侧隆起) + 阶梯式高低落差(瀑布段)
 */
public final class RiverTerrainRuntime {

    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final RiverColumnSample NO_RIVER =
            new RiverColumnSample(false, 0.0, 0.0, 0.0, 0, 0, 0, 0.0);

    private final int seaLevel;
    private final IntBinaryOperator preliminarySurface;
    private final DensityFunction continents;
    private final DensityFunction erosion;
    private final DensityFunction ridges;
    private final NormalNoise warpNoise;
    private final NormalNoise trunkNoise;
    private final NormalNoise branchNoise;
    private final NormalNoise tributaryNoise;
    private final NormalNoise fallNoise;
    private final NormalNoise bankNoise;
    private final NormalNoise auxNoise;
    private final Long2ObjectOpenHashMap<RiverColumnSample> cache = new Long2ObjectOpenHashMap<>();

    public RiverTerrainRuntime(
            RandomState random, int seaLevel, IntBinaryOperator preliminarySurface,
            DensityFunction continents, DensityFunction erosion, DensityFunction ridges
    ) {
        this.seaLevel = seaLevel;
        this.preliminarySurface = preliminarySurface;
        this.continents = continents;
        this.erosion = erosion;
        this.ridges = ridges;
        this.warpNoise = random.getOrCreateNoise(Noises.SHIFT);
        this.trunkNoise = random.getOrCreateNoise(Noises.EROSION);
        this.branchNoise = random.getOrCreateNoise(Noises.RIDGE);
        this.tributaryNoise = random.getOrCreateNoise(Noises.NOODLE);
        this.fallNoise = random.getOrCreateNoise(Noises.CONTINENTALNESS);
        this.bankNoise = random.getOrCreateNoise(Noises.SURFACE);
        this.auxNoise = random.getOrCreateNoise(Noises.SURFACE_SECONDARY);
    }

    public double carve(DensityFunction.FunctionContext ctx) {
        return sample(ctx.blockX(), ctx.blockZ()).carve(ctx.blockY());
    }

    public BlockState fluid(DensityFunction.FunctionContext ctx, double carvedDensity) {
        if (carvedDensity > 0.0) return null;
        RiverColumnSample col = sample(ctx.blockX(), ctx.blockZ());
        if (!col.active) return null;
        int y = ctx.blockY();
        return (y <= col.waterSurfaceY && y > col.floorY) ? WATER : null;
    }

    private RiverColumnSample sample(int x, int z) {
        long key = pack(x, z);
        RiverColumnSample cached = cache.get(key);
        if (cached != null) return cached;
        RiverColumnSample computed = computeColumn(x, z);
        cache.put(key, computed);
        return computed;
    }

    private RiverColumnSample computeColumn(int x, int z) {
        int terrainH = preliminarySurface.applyAsInt(x, z);
        if (terrainH < seaLevel + 3) return NO_RIVER;

        var pt = new DensityFunction.SinglePointContext(x, seaLevel, z);
        double continental = continents.compute(pt);
        if (continental < -0.15) return NO_RIVER;

        double erosionVal = erosion.compute(pt);
        double ridgeVal = Math.abs(ridges.compute(pt));

        // 1. 坐标扭曲(让河道蜿蜒)
        double wx = warpNoise.getValue(x * 0.003, 0.0, z * 0.003) * 48.0
                + auxNoise.getValue(x * 0.007, 11.0, z * 0.007) * 14.0;
        double wz = warpNoise.getValue(x * 0.003, 17.0, z * 0.003) * 48.0
                + auxNoise.getValue(x * 0.007, 23.0, z * 0.007) * 14.0;
        double sx = x + wx, sz = z + wz;

        // 2. 多级河道距离场: 主干(低频宽) / 一级支流(中频) / 二级支流(高频窄)
        double trunkDist = Math.abs(trunkNoise.getValue(sx * 0.0035, 0.0, sz * 0.0035));
        double branchDist = Math.abs(branchNoise.getValue(sx * 0.008, 0.0, sz * 0.008));
        double tribDist = Math.abs(tributaryNoise.getValue(sx * 0.016, 0.0, sz * 0.016));

        double mtn = Mth.clamp((terrainH - seaLevel - 8) / 100.0 + ridgeVal * 0.2, 0.0, 1.3);

        // 3. 合成树状水系 - 各级通道mask
        double tw = 0.10 + (1.0 - mtn) * 0.08
                + Math.max(0.0, bankNoise.getValue(sx * 0.01, 3.0, sz * 0.01)) * 0.02;
        double trunkMask = Mth.clamp((tw - trunkDist) / tw, 0.0, 1.0);
        double bw = 0.065 + (1.0 - mtn) * 0.04;
        double branchMask = Mth.clamp((bw - branchDist) / bw, 0.0, 1.0);
        double tbw = 0.04 + mtn * 0.025;
        double tribMask = Mth.clamp((tbw - tribDist) / tbw, 0.0, 1.0);

        double mask = Math.max(trunkMask, Math.max(branchMask * 0.75, tribMask * 0.5));
        mask *= Mth.clampedMap(continental, -0.10, 0.45, 0.1, 1.0);
        mask *= Mth.clampedMap(erosionVal, -1.0, 0.5, 1.0, 0.35);
        if (mask < 0.10) return NO_RIVER;

        // 4. 发源地判定: 高山+极窄通道=1格泉眼
        boolean src = mtn > 0.85 && terrainH > seaLevel + 45
                && mask > 0.15 && mask < 0.45
                && tribMask > branchMask && tribMask > trunkMask;

        // 5. 河道宽度: 发源地1格, 支流2-3格, 主干下游5-8格
        double width;
        if (src) {
            width = 1.0;
        } else if (trunkMask >= branchMask && trunkMask >= tribMask) {
            width = 3.0 + (1.0 - mtn) * 5.0;
        } else if (branchMask >= tribMask) {
            width = 2.0 + (1.0 - mtn) * 2.5;
        } else {
            width = 1.5 + (1.0 - mtn) * 1.0;
        }
        width = Math.max(1.0, width + auxNoise.getValue(sx * 0.02, 41.0, sz * 0.02) * 0.6);

        // 6. 高低落差(阶梯+瀑布)
        double fallVal = fallNoise.getValue(sx * 0.002, 0.0, sz * 0.002);
        double terraceVal = bankNoise.getValue(sx * 0.006, 31.0, sz * 0.006);
        double steps = Math.max(0, Math.floor((fallVal + 1.0) * (1.0 + mtn * 2.2)));
        int incision = 2 + Mth.floor(mtn * 5.0 + Math.max(0, terraceVal) * 2.5 + steps * 1.8);
        int waterSurf = terrainH - incision;
        if (terrainH <= seaLevel + 6) waterSurf = Math.min(waterSurf, seaLevel);

        // 7. 河床 & 河堤
        int bedDepth = src ? 1 : 2 + Mth.floor(mask * 2.5 + mtn * 2.0);
        int floorY = waterSurf - bedDepth;
        int bankH = src ? 1 : 3 + Mth.floor(mtn * 12.0 + Math.max(0, terraceVal) * 3.5);
        int topY = waterSurf + bankH;
        if (topY <= floorY + 2) return NO_RIVER;

        return new RiverColumnSample(true, mask, mtn, width, floorY, waterSurf, topY, src ? 1.0 : 0.0);
    }

    private static long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) << 32 | ((long) z & 0xFFFFFFFFL);
    }

    /**
     * 单列河流参数。
     * 剖面形状:
     *   河堤 \         / 河堤
     *         \  水面  /
     *          \______/  河床
     */
    private record RiverColumnSample(
            boolean active, double mask, double mountain, double width,
            int floorY, int waterSurfaceY, int topY, double sourceFactor
    ) {
        double carve(int y) {
            if (!active || y <= floorY - 1 || y > topY) return 0.0;

            double bankSpan = Math.max(1.0, topY - waterSurfaceY);
            double bedSpan = Math.max(1.0, waterSurfaceY - floorY);

            // 水面以上: 越靠近河堤顶部雕刻越弱(形成斜坡)
            double aboveWater = y > waterSurfaceY
                    ? Mth.clamp((topY - y) / bankSpan, 0.0, 1.0) : 1.0;
            // 水面以下: 越靠近河床底部雕刻略弱(形成碗形)
            double belowWater = y <= waterSurfaceY
                    ? 1.0 - Mth.clamp((y - floorY) / bedSpan, 0.0, 1.0) * 0.3 : 1.0;

            double bowl = mask * mask;
            double shoulders = Math.sqrt(mask);
            double strength = (0.85 + mountain * 2.8) * bowl * aboveWater * belowWater;

            // 水面附近额外加强确保河道贯通
            if (y <= waterSurfaceY + 1) {
                strength += (0.4 + mountain * 1.0) * shoulders;
            }
            // 发源地缩小雕刻范围
            if (sourceFactor > 0.5) {
                strength *= 0.6;
            }
            // 河堤隆起: 水面以上1-3格处边缘区域减弱雕刻 -> 保留地形形成堤岸
            if (y > waterSurfaceY + 1 && y <= waterSurfaceY + 3 && mask < 0.5) {
                strength *= 0.3;
            }
            return -strength;
        }
    }
}
