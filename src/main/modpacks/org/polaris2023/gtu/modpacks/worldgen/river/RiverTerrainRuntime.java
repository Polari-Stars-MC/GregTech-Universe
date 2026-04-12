package org.polaris2023.gtu.modpacks.worldgen.river;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.function.IntBinaryOperator;

public final class RiverTerrainRuntime {
    private static final int TRACE_STEP = 8;
    private static final int FLOW_LOOKAHEAD = 16;
    private static final int BANK_NEAR = 5;
    private static final int BANK_FAR = 11;
    private static final int MAX_UPSTREAM_STEPS = 28;
    private static final int MAX_DOWNSTREAM_STEPS = 48;

    private static final Direction[] DIRECTIONS = new Direction[]{
            new Direction(1, 0, 1.0),
            new Direction(1, 1, Math.sqrt(2.0)),
            new Direction(0, 1, 1.0),
            new Direction(-1, 1, Math.sqrt(2.0)),
            new Direction(-1, 0, 1.0),
            new Direction(-1, -1, Math.sqrt(2.0)),
            new Direction(0, -1, 1.0),
            new Direction(1, -1, Math.sqrt(2.0))
    };

    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final RiverColumnSample NO_RIVER = new RiverColumnSample(false, 0.0, 0.0, 0, 0, 0, 0);

    private final int seaLevel;
    private final IntBinaryOperator preliminarySurface;
    private final DensityFunction continents;
    private final DensityFunction erosion;
    private final DensityFunction ridges;
    private final NormalNoise shift;
    private final NormalNoise bank;
    private final NormalNoise secondary;
    private final Long2ObjectOpenHashMap<RiverColumnSample> columnCache = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<FlowSample> flowCache = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<NetworkSummary> networkCache = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap surfaceHeightCache = new Long2IntOpenHashMap();
    private final LongOpenHashSet networkGuard = new LongOpenHashSet();

    public RiverTerrainRuntime(
            RandomState random,
            int seaLevel,
            IntBinaryOperator preliminarySurface,
            DensityFunction continents,
            DensityFunction erosion,
            DensityFunction ridges
    ) {
        this.seaLevel = seaLevel;
        this.preliminarySurface = preliminarySurface;
        this.continents = continents;
        this.erosion = erosion;
        this.ridges = ridges;
        this.shift = random.getOrCreateNoise(Noises.SHIFT);
        this.bank = random.getOrCreateNoise(Noises.SURFACE);
        this.secondary = random.getOrCreateNoise(Noises.SURFACE_SECONDARY);
        this.surfaceHeightCache.defaultReturnValue(Integer.MIN_VALUE);
    }

    public double carve(DensityFunction.FunctionContext context) {
        RiverColumnSample sample = sample(context.blockX(), context.blockZ());
        return sample.carve(context.blockY());
    }

    public BlockState fluid(DensityFunction.FunctionContext context, double carvedDensity) {
        if (carvedDensity > 0.0) {
            return null;
        }

        RiverColumnSample sample = sample(context.blockX(), context.blockZ());
        if (!sample.active()) {
            return null;
        }

        int y = context.blockY();
        if (y > sample.waterSurfaceY() || y <= sample.floorY()) {
            return null;
        }

        return WATER;
    }

    private RiverColumnSample sample(int x, int z) {
        long key = pack(x, z);
        RiverColumnSample cached = columnCache.get(key);
        if (cached != null) {
            return cached;
        }

        RiverColumnSample computed = computeColumn(x, z);
        columnCache.put(key, computed);
        return computed;
    }

    private RiverColumnSample computeColumn(int x, int z) {
        int terrainHeight = surfaceHeight(x, z);
        if (terrainHeight <= seaLevel + 1) {
            return NO_RIVER;
        }

        int channelX = snapToTrace(x);
        int channelZ = snapToTrace(z);
        DensityFunction.SinglePointContext point = new DensityFunction.SinglePointContext(channelX, seaLevel, channelZ);
        double continental = continents.compute(point);
        if (continental < -0.16) {
            return NO_RIVER;
        }

        double erosionValue = erosion.compute(point);
        double ridgeValue = Math.abs(ridges.compute(point));
        double runoff = runoffFactor(channelX, channelZ, continental, erosionValue, ridgeValue);
        if (runoff < 0.12) {
            return NO_RIVER;
        }

        FlowSample flow = flow(channelX, channelZ);
        if (flow.directionIndex() < 0 && terrainHeight > seaLevel + 4) {
            return NO_RIVER;
        }

        TraceMetrics downstream = traceDownstream(channelX, channelZ);
        TraceMetrics upstream = traceUpstream(channelX, channelZ);
        ValleyProfile valley = valleyProfile(x, z, flow);
        double sourcePotential = headwaterPotential(channelX, channelZ, surfaceHeight(channelX, channelZ), ridgeValue, flow, downstream, valley);
        NetworkSummary network = networkSummary(channelX, channelZ, 0);
        if (!network.active()) {
            return NO_RIVER;
        }
        double mainStemJoin = mainStemJoinSignal(channelX, channelZ);

        double lowland = Mth.clamp((seaLevel + 26 - terrainHeight) / 44.0, 0.0, 1.0);
        double upland = Mth.clamp((terrainHeight - seaLevel - 6) / 54.0, 0.0, 1.0);
        double riverOrder = Mth.clamp(
                (upstream.steps() * 1.2 + upstream.branches() * 1.8 + downstream.branches() + upstream.drop() / 9.0) / 18.0,
                0.0,
                1.4
        );
        riverOrder = Math.max(riverOrder, sourcePotential * 0.4);
        riverOrder = Math.max(riverOrder, network.streamOrder() * 0.5 + network.distanceFromSource() * 0.055);
        double tributaryCorridor = Math.max(0.0, 1.25 - riverOrder)
                * (0.55 + upland * 0.7 + network.sourceStrength() * 0.45 + mainStemJoin * 0.85);
        Direction flowDirection = DIRECTIONS[flow.directionIndex()];
        int segmentEndX = channelX + flowDirection.dx() * TRACE_STEP;
        int segmentEndZ = channelZ + flowDirection.dz() * TRACE_STEP;
        ChannelProjection projection = projectToSegment(x, z, channelX, channelZ, segmentEndX, segmentEndZ);
        double sourceRun = network.distanceFromSource() + projection.progress();
        double channelRadius = 0.45
                + Math.min(4.8, sourceRun * 0.11)
                + Math.max(0.0, network.streamOrder() - 1) * 0.28
                + network.immediateTributaries() * 0.2
                + Math.sqrt(Math.max(0.0, network.discharge())) * 0.16
                + lowland * 0.2
                + tributaryCorridor * 0.12;
        channelRadius = Mth.clamp(channelRadius, 0.45, 6.5);
        double lateralMask = Mth.clamp(
                (channelRadius - projection.distance()) / Math.max(0.18, channelRadius),
                0.0,
                1.0
        );
        if (lateralMask <= 0.0) {
            return NO_RIVER;
        }

        double outletMask = downstream.reachesSea() ? 1.0 : Mth.clamp(downstream.steps() / 9.0, 0.38, 1.0);
        double headwaterMask = upstream.steps() == 0
                ? Math.max(Mth.clamp(flow.drop() / 4.0, 0.35, 1.0), sourcePotential)
                : 1.0;
        headwaterMask = Math.max(headwaterMask, 0.45 + network.sourceStrength() * 0.8 + mainStemJoin * 0.25);
        double valleyFloorThreshold = Mth.lerp(
                sourcePotential,
                Math.max(0.25, 0.8 - riverOrder * 0.45),
                Math.max(0.55, 1.45 - riverOrder * 0.95)
        );
        double valleyMask = Mth.clamp(
                (valley.crossDepth() - valleyFloorThreshold) / (1.4 + riverOrder + lowland * 0.8),
                0.0,
                1.0
        );
        valleyMask = Math.max(
                valleyMask,
                sourcePotential * Mth.clamp((valley.crossDepth() + valley.bankRise() * 0.08 - 0.3) / (1.6 + riverOrder), 0.0, 1.0)
        );
        valleyMask = Math.max(valleyMask, network.sourceStrength() * Mth.clamp((valley.crossDepth() - 0.18) / 1.9, 0.0, 1.0));
        double balanceMask = Mth.clamp(1.0 - valley.lateralOffset() / (1.28 + channelRadius * 0.12), 0.35, 1.0);
        double lineageMask = Mth.clamp((network.discharge() - 0.6) / 1.2, 0.0, 1.0);
        lineageMask = Math.max(lineageMask, 0.25 + network.sourceStrength() * 0.85);
        double branchDropMask = Mth.clamp((sourcePotential + upland * 0.7 + mainStemJoin * 0.65 + slopePerStepHint(downstream, flow) * 0.16 - 0.12), 0.0, 1.0);
        double basinPenalty = lowland
                * Mth.clamp((0.95 - valley.crossDepth()) / 0.95, 0.0, 1.0)
                * Mth.clamp((1.8 - slopePerStepHint(downstream, flow)) / 1.8, 0.0, 1.0);
        double channelTightness = Mth.lerp(Mth.clamp(tributaryCorridor, 0.0, 1.0), 1.15, 1.55);
        double channelMask = Math.pow(lateralMask, channelTightness);
        double mask = runoff * outletMask * headwaterMask * valleyMask * channelMask * lineageMask * balanceMask * (1.0 - basinPenalty * 0.82);
        mask = Math.max(mask, (sourcePotential + mainStemJoin * 0.55) * branchDropMask * channelMask * 0.74);
        if (mask < Math.max(0.028, 0.045 - tributaryCorridor * 0.015)) {
            return NO_RIVER;
        }

        double slopePerStep = downstream.steps() > 0 ? downstream.drop() / (double) downstream.steps() : flow.drop();
        double tributaryBoost = Math.max(0.0, 1.15 - riverOrder)
                * (sourcePotential * 2.1 + upland * 1.05 + network.sourceStrength() * 0.95 + tributaryCorridor * 0.8 + mainStemJoin * 1.2);
        double branchIncision = sourcePotential * (2.4 + upland * 1.25) + branchDropMask * 1.7 + tributaryBoost;
        double channelTerrain = Mth.lerp(
                (float) projection.progress(),
                surfaceHeight(channelX, channelZ),
                surfaceHeight(segmentEndX, segmentEndZ)
        );
        double incision = 1.5 + riverOrder * 3.1 + slopePerStep * 0.32 + valley.crossDepth() * 0.22 + lowland * 0.18
                + network.streamOrder() * 0.7 + network.discharge() * 0.14 + branchIncision;
        int waterSurface = Mth.floor(channelTerrain) - Mth.floor(incision);
        if (terrainHeight <= seaLevel + 5) {
            waterSurface = Math.min(waterSurface, seaLevel);
        }
        waterSurface = Math.min(waterSurface, Mth.floor(channelTerrain) - 1);

        int depth = 1 + Mth.floor(riverOrder * 2.0 + valleyMask * 1.0 + slopePerStep * 0.22 + sourcePotential * 1.95
                + network.streamOrder() * 0.45 + network.discharge() * 0.16 + upland * 1.05
                + tributaryBoost * 1.2 + tributaryCorridor * 0.8);
        int floorY = waterSurface - depth;
        int bankTop = waterSurface + 1 + Mth.floor(riverOrder * 2.6 + valley.bankRise() * 0.1 + lowland * 0.5
                + sourcePotential * 0.4 + network.streamOrder() * 0.65 + network.immediateTributaries() * 0.4
                + tributaryCorridor * 0.2 + channelRadius * 0.6);
        int topY = Math.max(terrainHeight + 1, bankTop);
        if (topY <= floorY + 2) {
            return NO_RIVER;
        }

        return new RiverColumnSample(true, mask, riverOrder, terrainHeight, floorY, waterSurface, topY);
    }

    private double runoffFactor(int x, int z, double continental, double erosionValue, double ridgeValue) {
        double wetNoise = 0.72
                + bank.getValue(x * 0.0058, 7.0, z * 0.0058) * 0.22
                + secondary.getValue(x * 0.0098, 19.0, z * 0.0098) * 0.12;
        double continentalMask = Mth.clampedMap(continental, -0.12, 0.42, 0.3, 1.0);
        double erosionMask = Mth.clampedMap(erosionValue, -1.0, 0.55, 1.0, 0.58);
        double ridgeMask = 0.82 + ridgeValue * 0.22;
        return wetNoise * continentalMask * erosionMask * ridgeMask;
    }

    private double headwaterPotential(int x, int z, int terrainHeight, double ridgeValue, FlowSample flow,
                                      TraceMetrics downstream, ValleyProfile valley) {
        if (flow.directionIndex() < 0) {
            return 0.0;
        }

        double highland = Mth.clamp((terrainHeight - seaLevel - 2) / 38.0, 0.0, 1.0);
        double alpine = Mth.clamp((ridgeValue + 0.04) / 0.24, 0.0, 1.0);
        double localDrop = Mth.clamp(flow.drop() / 2.2, 0.0, 1.0);
        double downstreamDrop = Mth.clamp((downstream.drop() + flow.drop()) / 8.0, 0.0, 1.0);
        double relief = Mth.clamp(localRelief(x, z, FLOW_LOOKAHEAD) / 6.0, 0.0, 1.0);
        double notch = Mth.clamp((valley.crossDepth() + valley.bankRise() * 0.08 - 0.02) / 0.9, 0.0, 1.0);
        return highland * alpine * relief * Math.max(localDrop, downstreamDrop) * (0.45 + notch * 0.55);
    }

    private NetworkSummary networkSummary(int x, int z, int depth) {
        int nodeX = snapToTrace(x);
        int nodeZ = snapToTrace(z);
        long key = pack(nodeX, nodeZ);
        NetworkSummary cached = networkCache.get(key);
        if (cached != null) {
            return cached;
        }
        if (depth > MAX_UPSTREAM_STEPS || !networkGuard.add(key)) {
            return NetworkSummary.INACTIVE;
        }

        int terrainHeight = surfaceHeight(nodeX, nodeZ);
        DensityFunction.SinglePointContext point = new DensityFunction.SinglePointContext(nodeX, seaLevel, nodeZ);
        double continental = continents.compute(point);
        double erosionValue = erosion.compute(point);
        double ridgeValue = Math.abs(ridges.compute(point));
        double runoff = runoffFactor(nodeX, nodeZ, continental, erosionValue, ridgeValue);
        FlowSample flow = flow(nodeX, nodeZ);
        TraceMetrics downstream = traceDownstream(nodeX, nodeZ);
        ValleyProfile valley = valleyProfile(nodeX, nodeZ, flow);
        double localSourceStrength = headwaterPotential(nodeX, nodeZ, terrainHeight, ridgeValue, flow, downstream, valley);
        double mainStemJoin = mainStemJoinSignal(nodeX, nodeZ);

        int activeInputs = 0;
        int sourceCount = 0;
        int distanceFromSource = 0;
        int streamOrder = 0;
        int maxOrderCount = 0;
        double discharge = 0.88 + runoff * 1.1 + mainStemJoin * 0.9;

        for (Direction direction : DIRECTIONS) {
            int upstreamX = nodeX - direction.dx() * TRACE_STEP;
            int upstreamZ = nodeZ - direction.dz() * TRACE_STEP;
            if (!flowsInto(upstreamX, upstreamZ, nodeX, nodeZ)) {
                continue;
            }

            NetworkSummary upstream = networkSummary(upstreamX, upstreamZ, depth + 1);
            if (!upstream.active()) {
                continue;
            }

            activeInputs++;
            sourceCount += upstream.sourceCount();
            distanceFromSource = Math.max(distanceFromSource, upstream.distanceFromSource() + 1);
            discharge += upstream.discharge() * 1.12;
            if (upstream.streamOrder() > streamOrder) {
                streamOrder = upstream.streamOrder();
                maxOrderCount = 1;
            } else if (upstream.streamOrder() == streamOrder) {
                maxOrderCount++;
            }
        }

        boolean isSource = activeInputs == 0 && (localSourceStrength >= 0.14 || (mainStemJoin >= 0.38 && flow.drop() >= 1));
        if (isSource) {
            sourceCount = 1;
            streamOrder = 1;
            maxOrderCount = 1;
            distanceFromSource = 0;
            discharge += 2.0 + localSourceStrength * 2.8 + mainStemJoin * 1.4;
        } else if (activeInputs >= 2 && maxOrderCount >= 2) {
            streamOrder += 1;
        } else if (activeInputs > 0) {
            streamOrder = Math.max(1, streamOrder);
        }

        boolean active = sourceCount > 0
                && (downstream.reachesSea() || downstream.steps() >= 1 || activeInputs > 0 || mainStemJoin >= 0.25 || (isSource && flow.drop() >= 2))
                && (discharge >= 0.26 || isSource || activeInputs > 0 || localSourceStrength >= 0.18 || mainStemJoin >= 0.32);

        NetworkSummary computed = active
                ? new NetworkSummary(
                true,
                sourceCount,
                distanceFromSource,
                streamOrder,
                Math.max(0, activeInputs - 1),
                localSourceStrength,
                discharge
        )
                : NetworkSummary.INACTIVE;
        networkGuard.remove(key);
        networkCache.put(key, computed);
        return computed;
    }

    private TraceMetrics traceDownstream(int startX, int startZ) {
        int x = startX;
        int z = startZ;
        int height = surfaceHeight(x, z);
        int steps = 0;
        int drop = 0;
        int branches = 0;
        boolean reachesSea = false;

        for (int i = 0; i < MAX_DOWNSTREAM_STEPS; i++) {
            FlowSample flow = flow(x, z);
            if (flow.directionIndex() < 0) {
                reachesSea = height <= seaLevel + 2;
                break;
            }

            Direction direction = DIRECTIONS[flow.directionIndex()];
            int nextX = x + direction.dx() * TRACE_STEP;
            int nextZ = z + direction.dz() * TRACE_STEP;
            int nextHeight = surfaceHeight(nextX, nextZ);
            if (nextHeight >= height) {
                reachesSea = height <= seaLevel + 2;
                break;
            }

            steps++;
            drop += height - nextHeight;
            if (steps <= 8) {
                branches += Math.max(0, countUpstreamInputs(nextX, nextZ) - 1);
            }

            x = nextX;
            z = nextZ;
            height = nextHeight;
            if (height <= seaLevel + 1) {
                reachesSea = true;
                break;
            }
        }

        return new TraceMetrics(steps, drop, branches, reachesSea);
    }

    private TraceMetrics traceUpstream(int startX, int startZ) {
        int x = startX;
        int z = startZ;
        int height = surfaceHeight(x, z);
        int steps = 0;
        int rise = 0;
        int branches = 0;
        int previousDirection = -1;

        for (int i = 0; i < MAX_UPSTREAM_STEPS; i++) {
            UpstreamChoice choice = chooseUpstream(x, z, height, previousDirection);
            branches += Math.max(0, choice.candidateCount() - 1);
            if (!choice.found()) {
                break;
            }

            steps++;
            rise += choice.height() - height;
            x = choice.x();
            z = choice.z();
            height = choice.height();
            previousDirection = choice.directionIndex();
        }

        return new TraceMetrics(steps, rise, branches, false);
    }

    private UpstreamChoice chooseUpstream(int x, int z, int currentHeight, int previousDirection) {
        int bestX = 0;
        int bestZ = 0;
        int bestHeight = 0;
        int bestDirection = -1;
        int candidateCount = 0;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int directionIndex = 0; directionIndex < DIRECTIONS.length; directionIndex++) {
            Direction direction = DIRECTIONS[directionIndex];
            int nextX = x + direction.dx() * TRACE_STEP;
            int nextZ = z + direction.dz() * TRACE_STEP;
            FlowSample candidateFlow = flow(nextX, nextZ);
            int nextHeight = candidateFlow.terrainHeight();
            int rise = nextHeight - currentHeight;
            if (rise < 1 || candidateFlow.directionIndex() < 0 || !flowsToward(nextX, nextZ, candidateFlow.directionIndex(), x, z)) {
                continue;
            }

            candidateCount++;
            double turnAlignment = previousDirection < 0
                    ? 0.0
                    : dot(
                    direction.dx(),
                    direction.dz(),
                    DIRECTIONS[previousDirection].dx(),
                    DIRECTIONS[previousDirection].dz()
            );
            double score = rise * 1.35 + candidateFlow.drop() * 0.85 + turnAlignment * 1.4;
            if (score > bestScore) {
                bestScore = score;
                bestX = nextX;
                bestZ = nextZ;
                bestHeight = nextHeight;
                bestDirection = directionIndex;
            }
        }

        return new UpstreamChoice(bestDirection >= 0, bestX, bestZ, bestHeight, bestDirection, candidateCount);
    }

    private int countUpstreamInputs(int x, int z) {
        int currentHeight = surfaceHeight(x, z);
        int inputs = 0;
        for (Direction direction : DIRECTIONS) {
            int sourceX = x + direction.dx() * TRACE_STEP;
            int sourceZ = z + direction.dz() * TRACE_STEP;
            FlowSample flow = flow(sourceX, sourceZ);
            if (flow.directionIndex() < 0 || flow.terrainHeight() <= currentHeight) {
                continue;
            }
            if (flowsToward(sourceX, sourceZ, flow.directionIndex(), x, z)) {
                inputs++;
            }
        }
        return inputs;
    }

    private boolean flowsInto(int sourceX, int sourceZ, int targetX, int targetZ) {
        FlowSample sourceFlow = flow(sourceX, sourceZ);
        if (sourceFlow.directionIndex() < 0) {
            return false;
        }
        Direction direction = DIRECTIONS[sourceFlow.directionIndex()];
        return sourceX + direction.dx() * TRACE_STEP == targetX
                && sourceZ + direction.dz() * TRACE_STEP == targetZ;
    }

    private boolean flowsToward(int sourceX, int sourceZ, int directionIndex, int targetX, int targetZ) {
        Direction direction = DIRECTIONS[directionIndex];
        int projectedX = sourceX + direction.dx() * TRACE_STEP;
        int projectedZ = sourceZ + direction.dz() * TRACE_STEP;
        int projectedDx = projectedX - targetX;
        int projectedDz = projectedZ - targetZ;
        if (projectedDx * projectedDx + projectedDz * projectedDz <= TRACE_STEP * TRACE_STEP / 2) {
            return true;
        }

        return dot(
                direction.dx(),
                direction.dz(),
                targetX - sourceX,
                targetZ - sourceZ
        ) > 0.7;
    }

    private ValleyProfile valleyProfile(int x, int z, FlowSample flow) {
        Direction axis = flow.directionIndex() >= 0
                ? DIRECTIONS[flow.directionIndex()]
                : dominantDirection(flow.downhillX(), flow.downhillZ());
        Direction perpendicular = axis.perpendicular();
        int center = flow.terrainHeight();
        int leftNear = surfaceHeight(x + perpendicular.dx() * BANK_NEAR, z + perpendicular.dz() * BANK_NEAR);
        int rightNear = surfaceHeight(x - perpendicular.dx() * BANK_NEAR, z - perpendicular.dz() * BANK_NEAR);
        int leftFar = surfaceHeight(x + perpendicular.dx() * BANK_FAR, z + perpendicular.dz() * BANK_FAR);
        int rightFar = surfaceHeight(x - perpendicular.dx() * BANK_FAR, z - perpendicular.dz() * BANK_FAR);

        double nearDepth = ((leftNear + rightNear) * 0.5) - center;
        double farDepth = ((leftFar + rightFar) * 0.5) - center;
        double crossDepth = Math.max(0.0, nearDepth * 0.55 + farDepth * 0.45);
        double bankRise = Math.max(Math.max(leftNear, rightNear) - center, 0)
                + Math.max(Math.max(leftFar, rightFar) - center, 0) * 0.35;
        double lateralOffset = (
                Math.abs(leftNear - rightNear) * 0.65
                        + Math.abs(leftFar - rightFar) * 0.35
        ) / Math.max(4.0, crossDepth * 3.4 + 4.0);
        return new ValleyProfile(crossDepth, lateralOffset, bankRise);
    }

    private FlowSample flow(int x, int z) {
        long key = pack(x, z);
        FlowSample cached = flowCache.get(key);
        if (cached != null) {
            return cached;
        }

        int height = surfaceHeight(x, z);
        double downhillX = surfaceHeight(x - FLOW_LOOKAHEAD, z) - surfaceHeight(x + FLOW_LOOKAHEAD, z);
        double downhillZ = surfaceHeight(x, z - FLOW_LOOKAHEAD) - surfaceHeight(x, z + FLOW_LOOKAHEAD);
        double noiseX = shift.getValue(x * 0.0052, 0.0, z * 0.0052)
                + secondary.getValue(x * 0.0104, 9.0, z * 0.0104) * 0.45;
        double noiseZ = shift.getValue(x * 0.0052, 17.0, z * 0.0052)
                + bank.getValue(x * 0.0104, 21.0, z * 0.0104) * 0.45;

        int bestDirection = -1;
        int bestDrop = Integer.MIN_VALUE;
        int bestFarDrop = Integer.MIN_VALUE;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int directionIndex = 0; directionIndex < DIRECTIONS.length; directionIndex++) {
            Direction direction = DIRECTIONS[directionIndex];
            int nextHeight = surfaceHeight(x + direction.dx() * TRACE_STEP, z + direction.dz() * TRACE_STEP);
            int farHeight = surfaceHeight(x + direction.dx() * TRACE_STEP * 2, z + direction.dz() * TRACE_STEP * 2);
            int drop = height - nextHeight;
            int farDrop = height - farHeight;
            double slopeScore = Math.max(0.0, drop) / direction.length()
                    + Math.max(0.0, farDrop) / (direction.length() * 2.2);
            double terrainAlignment = dot(direction.dx(), direction.dz(), downhillX, downhillZ);
            double meanderAlignment = dot(direction.dx(), direction.dz(), noiseX, noiseZ);
            double score = slopeScore + terrainAlignment * 1.15 + meanderAlignment * 0.25;
            if (drop <= 0 && farDrop > 0) {
                score += Math.min(1.3, farDrop * 0.11);
            }
            if (drop <= 0 && farDrop <= 0 && height > seaLevel + 4) {
                score -= 2.2 + Math.abs(drop) * 0.35;
            }
            if (score > bestScore) {
                bestScore = score;
                bestDirection = directionIndex;
                bestDrop = drop;
                bestFarDrop = farDrop;
            }
        }

        int effectiveDrop = Math.max(bestDrop, Mth.floor(bestFarDrop * 0.55));
        if (effectiveDrop <= 0 && height > seaLevel + 4) {
            bestDirection = -1;
        }

        FlowSample computed = new FlowSample(height, bestDirection, Math.max(effectiveDrop, 0), downhillX, downhillZ);
        flowCache.put(key, computed);
        return computed;
    }

    private int surfaceHeight(int x, int z) {
        long key = pack(x, z);
        int cached = surfaceHeightCache.get(key);
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int computed = preliminarySurface.applyAsInt(x, z);
        surfaceHeightCache.put(key, computed);
        return computed;
    }

    private int localRelief(int x, int z, int radius) {
        int center = surfaceHeight(x, z);
        int lowest = center;
        for (Direction direction : DIRECTIONS) {
            lowest = Math.min(lowest, surfaceHeight(x + direction.dx() * radius, z + direction.dz() * radius));
        }
        return center - lowest;
    }

    private double mainStemJoinSignal(int startX, int startZ) {
        int x = startX;
        int z = startZ;
        int height = surfaceHeight(x, z);
        double best = 0.0;

        for (int step = 0; step < 10; step++) {
            FlowSample flow = flow(x, z);
            if (flow.directionIndex() < 0) {
                break;
            }

            Direction direction = DIRECTIONS[flow.directionIndex()];
            int nextX = x + direction.dx() * TRACE_STEP;
            int nextZ = z + direction.dz() * TRACE_STEP;
            int nextHeight = surfaceHeight(nextX, nextZ);
            if (nextHeight >= height && height > seaLevel + 2) {
                break;
            }

            int inflowCount = countUpstreamInputs(nextX, nextZ);
            if (inflowCount >= 2) {
                double distanceFactor = 1.0 - step / 10.0;
                double trunkFactor = Mth.clamp((inflowCount - 1) / 3.0, 0.0, 1.0);
                best = Math.max(best, distanceFactor * (0.45 + trunkFactor * 0.55));
            }

            x = nextX;
            z = nextZ;
            height = nextHeight;
        }

        return best;
    }

    private static ChannelProjection projectToSegment(int x, int z, int startX, int startZ, int endX, int endZ) {
        double dx = endX - startX;
        double dz = endZ - startZ;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared < 1.0E-6) {
            return new ChannelProjection(0.0, 0.0);
        }

        double t = ((x - startX) * dx + (z - startZ) * dz) / lengthSquared;
        t = Mth.clamp(t, 0.0, 1.0);
        double projectionX = startX + dx * t;
        double projectionZ = startZ + dz * t;
        double distance = Math.sqrt((x - projectionX) * (x - projectionX) + (z - projectionZ) * (z - projectionZ));
        return new ChannelProjection(t, distance);
    }

    private static double slopePerStepHint(TraceMetrics downstream, FlowSample flow) {
        return downstream.steps() > 0 ? downstream.drop() / (double) downstream.steps() : flow.drop();
    }

    private static Direction dominantDirection(double x, double z) {
        Direction best = DIRECTIONS[0];
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Direction direction : DIRECTIONS) {
            double score = dot(direction.dx(), direction.dz(), x, z);
            if (score > bestScore) {
                bestScore = score;
                best = direction;
            }
        }
        return best;
    }

    private static double dot(double ax, double az, double bx, double bz) {
        double aLength = Math.sqrt(ax * ax + az * az);
        double bLength = Math.sqrt(bx * bx + bz * bz);
        if (aLength < 1.0E-6 || bLength < 1.0E-6) {
            return 0.0;
        }
        return (ax * bx + az * bz) / (aLength * bLength);
    }

    private static int snapToTrace(int value) {
        return Math.floorDiv(value, TRACE_STEP) * TRACE_STEP;
    }

    private static long pack(int x, int z) {
        return ((long) x & 4294967295L) << 32 | ((long) z & 4294967295L);
    }

    private record RiverColumnSample(boolean active, double mask, double riverOrder, int terrainY,
                                     int floorY, int waterSurfaceY, int topY) {
        double carve(int y) {
            if (!active || y <= floorY - 1 || y > topY) {
                return 0.0;
            }

            double bankSpan = Math.max(1.0, topY - waterSurfaceY);
            double bedSpan = Math.max(1.0, waterSurfaceY - floorY);
            double aboveWater = y > waterSurfaceY ? Math.pow(Mth.clamp((topY - y) / bankSpan, 0.0, 1.0), 1.55) : 1.0;
            double belowSurface = y <= waterSurfaceY
                    ? 1.0 - Mth.clamp((y - floorY) / bedSpan, 0.0, 1.0) * 0.16
                    : 1.0;
            double tributaryBoost = Math.max(0.0, 1.25 - riverOrder);
            double activeMask = Mth.clampedMap(mask, 0.028, 1.0, 0.72, 1.0);
            double surfaceOpen = y >= terrainY - 1 ? 1.0 : 0.0;
            double trenchDepth = y <= waterSurfaceY + 1 ? 1.0 : 0.0;
            double bowl = activeMask * (7.5 + riverOrder * 6.2 + tributaryBoost * 6.5);
            double shoulders = activeMask * (2.2 + riverOrder * 1.4 + tributaryBoost * 1.6);
            double strength = bowl * aboveWater * belowSurface;
            strength += surfaceOpen * (3.8 + tributaryBoost * 2.0);
            strength += trenchDepth * (4.0 + riverOrder * 2.3 + tributaryBoost * 2.4);
            if (y <= waterSurfaceY + 2) {
                strength += shoulders + tributaryBoost * 0.18;
            }
            return -strength;
        }
    }

    private record FlowSample(int terrainHeight, int directionIndex, int drop, double downhillX, double downhillZ) {
    }

    private record TraceMetrics(int steps, int drop, int branches, boolean reachesSea) {
    }

    private record ValleyProfile(double crossDepth, double lateralOffset, double bankRise) {
    }

    private record ChannelProjection(double progress, double distance) {
    }

    private record NetworkSummary(
            boolean active,
            int sourceCount,
            int distanceFromSource,
            int streamOrder,
            int immediateTributaries,
            double sourceStrength,
            double discharge
    ) {
        private static final NetworkSummary INACTIVE = new NetworkSummary(false, 0, 0, 0, 0, 0.0, 0.0);
    }

    private record UpstreamChoice(boolean found, int x, int z, int height, int directionIndex, int candidateCount) {
    }

    private record Direction(int dx, int dz, double length) {
        Direction perpendicular() {
            return new Direction(-dz, dx, length);
        }
    }
}
