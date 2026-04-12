package org.polaris2023.gtu.core.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RealisticRiverFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction2D[] DIRECTIONS = new Direction2D[]{
            new Direction2D(1, 0),
            new Direction2D(1, 1),
            new Direction2D(0, 1),
            new Direction2D(-1, 1),
            new Direction2D(-1, 0),
            new Direction2D(-1, -1),
            new Direction2D(0, -1),
            new Direction2D(1, -1)
    };

    private static final int SOURCE_SCAN_RADIUS = 28;
    private static final int SOURCE_SCAN_STEP = 4;
    private static final int MIN_SOURCE_HEIGHT_ABOVE_SEA = 18;
    private static final int MIN_SOURCE_DROP = 7;
    private static final int MIN_TOTAL_DROP = 15;
    private static final int MAX_SEGMENTS = 20;
    private static final int BASE_STEP_LENGTH = 4;
    private static final int MAX_STALLS = 2;

    public RealisticRiverFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int seaLevel = level.getSeaLevel();

        RiverNode source = findSource(level, context.origin(), random, seaLevel);
        if (source == null) {
            return false;
        }

        Set<Long> visited = new HashSet<>();
        visited.add(pack(source.x, source.z));

        List<RiverNode> path = new ArrayList<>();
        path.add(source);

        RiverNode current = source;
        Direction2D previousDirection = null;
        int deepestWater = current.waterY;
        int stalledSegments = 0;

        for (int index = 0; index < MAX_SEGMENTS; index++) {
            RiverNode next = findNextNode(level, current, previousDirection, visited, seaLevel, index);
            if (next == null) {
                break;
            }

            if (next.waterY >= current.waterY) {
                stalledSegments++;
                if (stalledSegments > MAX_STALLS) {
                    break;
                }
            } else {
                stalledSegments = 0;
            }

            path.add(next);
            deepestWater = Math.min(deepestWater, next.waterY);
            previousDirection = new Direction2D(Integer.signum(next.x - current.x), Integer.signum(next.z - current.z));
            current = next;
            visited.add(pack(current.x, current.z));

            if (isWaterSurface(level, current.x, current.z) && current.waterY <= seaLevel + 2) {
                break;
            }
        }

        if (path.size() < 2 || source.waterY - deepestWater < MIN_TOTAL_DROP) {
            return false;
        }

        carveHeadwater(level, random, source, seaLevel);
        int lastSegmentIndex = path.size() - 1;
        for (int index = 1; index < path.size(); index++) {
            RiverNode start = path.get(index - 1);
            RiverNode end = path.get(index);
            float downstream = index / (float) lastSegmentIndex;
            float width = 2.2F + downstream * 3.8F;
            int depth = 2 + Mth.floor(downstream * 2.0F);
            carveSegment(level, random, start, end, width, depth, downstream, seaLevel);
            if (start.waterY - end.waterY >= 4) {
                carveWaterfall(level, start, end, width);
            }
        }
        carveMouth(level, random, path.get(path.size() - 1), seaLevel);
        return true;
    }

    private RiverNode findSource(WorldGenLevel level, BlockPos origin, RandomSource random, int seaLevel) {
        RiverNode best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int dx = -SOURCE_SCAN_RADIUS; dx <= SOURCE_SCAN_RADIUS; dx += SOURCE_SCAN_STEP) {
            for (int dz = -SOURCE_SCAN_RADIUS; dz <= SOURCE_SCAN_RADIUS; dz += SOURCE_SCAN_STEP) {
                if (dx * dx + dz * dz > SOURCE_SCAN_RADIUS * SOURCE_SCAN_RADIUS) {
                    continue;
                }

                int x = origin.getX() + dx + random.nextInt(3) - 1;
                int z = origin.getZ() + dz + random.nextInt(3) - 1;
                SurfaceSample sample = sampleSurface(level, x, z);
                if (sample == null || sample.surfaceY < seaLevel + MIN_SOURCE_HEIGHT_ABOVE_SEA) {
                    continue;
                }
                if (sample.isWater || !isRiverSourceSurface(sample.state)) {
                    continue;
                }

                int localDrop = localDrop(level, x, z, sample.surfaceY);
                if (localDrop < MIN_SOURCE_DROP) {
                    continue;
                }

                int distancePenalty = Math.abs(dx) + Math.abs(dz);
                int score = sample.surfaceY * 4 + localDrop * 7 - distancePenalty;
                if (score > bestScore) {
                    int waterY = Math.min(sample.surfaceY - 1, sample.surfaceY - Math.max(1, localDrop / 4));
                    best = new RiverNode(x, z, sample.surfaceY, waterY);
                    bestScore = score;
                }
            }
        }

        return best;
    }

    private int localDrop(WorldGenLevel level, int x, int z, int surfaceY) {
        int bestDrop = Integer.MIN_VALUE;

        for (Direction2D direction : DIRECTIONS) {
            for (int distance : new int[]{4, 8, 12}) {
                SurfaceSample sample = sampleSurface(level, x + direction.dx * distance, z + direction.dz * distance);
                if (sample == null) {
                    continue;
                }
                bestDrop = Math.max(bestDrop, surfaceY - sample.surfaceY);
            }
        }

        return bestDrop == Integer.MIN_VALUE ? Integer.MIN_VALUE : bestDrop;
    }

    private RiverNode findNextNode(WorldGenLevel level, RiverNode current, Direction2D previousDirection,
                                   Set<Long> visited, int seaLevel, int segmentIndex) {
        RiverNode best = null;
        int bestScore = Integer.MIN_VALUE;
        int stepLength = BASE_STEP_LENGTH + Math.min(2, segmentIndex / 6);

        for (Direction2D direction : DIRECTIONS) {
            int directionBias = previousDirection == null ? 0 : direction.dot(previousDirection);
            for (int distance : new int[]{stepLength, stepLength + 2}) {
                int x = current.x + direction.dx * distance;
                int z = current.z + direction.dz * distance;
                if (visited.contains(pack(x, z))) {
                    continue;
                }

                SurfaceSample sample = sampleSurface(level, x, z);
                if (sample == null) {
                    continue;
                }

                int terrainDrop = current.surfaceY - sample.surfaceY;
                boolean joinsExistingWater = sample.isWater && sample.surfaceY <= current.waterY + 1;
                if (terrainDrop < 0 && !joinsExistingWater) {
                    continue;
                }

                int nextWaterY = joinsExistingWater
                        ? Math.min(sample.surfaceY, current.waterY)
                        : Math.min(current.waterY - Math.max(1, terrainDrop / 3), sample.surfaceY - 1);
                if (nextWaterY < level.getMinBuildHeight() + 2) {
                    continue;
                }

                int score = terrainDrop * 8;
                score += directionBias * 6;
                score -= Math.abs(direction.dx) + Math.abs(direction.dz);
                if (joinsExistingWater) {
                    score += 24;
                }
                if (sample.surfaceY <= seaLevel + 4) {
                    score += 8;
                }
                if (previousDirection != null && direction.dot(previousDirection) < 0) {
                    score -= 18;
                }

                if (score > bestScore) {
                    best = new RiverNode(x, z, sample.surfaceY, nextWaterY);
                    bestScore = score;
                }
            }
        }

        return best;
    }

    private void carveHeadwater(WorldGenLevel level, RandomSource random, RiverNode source, int seaLevel) {
        carveCrossSection(level, random, source.x, source.z, source.waterY, 2.4F, 2, 0.0F, seaLevel, true);
        carveCrossSection(level, random, source.x + 1, source.z, source.waterY, 1.8F, 2, 0.0F, seaLevel, true);
    }

    private void carveMouth(WorldGenLevel level, RandomSource random, RiverNode node, int seaLevel) {
        carveCrossSection(level, random, node.x, node.z, Math.min(node.waterY, seaLevel), 4.8F, 3, 1.0F, seaLevel, true);
        for (Direction2D direction : DIRECTIONS) {
            carveCrossSection(level, random, node.x + direction.dx * 2, node.z + direction.dz * 2,
                    Math.min(node.waterY, seaLevel), 3.2F, 2, 1.0F, seaLevel, false);
        }
    }

    private void carveSegment(WorldGenLevel level, RandomSource random, RiverNode start, RiverNode end,
                              float width, int depth, float downstream, int seaLevel) {
        int steps = Math.max(Math.abs(end.x - start.x), Math.abs(end.z - start.z));
        if (steps <= 0) {
            return;
        }

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Mth.floor(Mth.lerp(t, start.x, end.x));
            int z = Mth.floor(Mth.lerp(t, start.z, end.z));
            int waterY = Mth.floor(Mth.lerp(t, start.waterY, end.waterY));
            float localWidth = width + (1.0F - Math.abs(t - 0.5F) * 2.0F) * 0.4F;
            carveCrossSection(level, random, x, z, waterY, localWidth, depth, downstream, seaLevel, false);
        }
    }

    private void carveWaterfall(WorldGenLevel level, RiverNode start, RiverNode end, float width) {
        int radius = Math.max(1, Mth.floor(width * 0.35F));
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) {
                    continue;
                }
                for (int y = end.waterY; y <= start.waterY; y++) {
                    mutable.set(end.x + dx, y, end.z + dz);
                    if (canCarve(level.getBlockState(mutable))) {
                        level.setBlock(mutable, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void carveCrossSection(WorldGenLevel level, RandomSource random, int centerX, int centerZ, int waterY,
                                   float width, int depth, float downstream, int seaLevel, boolean spring) {
        int radius = Mth.ceil(width + 1.5F);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float distance = Mth.sqrt(dx * dx + dz * dz);
                if (distance > width + 0.85F) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;
                SurfaceSample sample = sampleSurface(level, x, z);
                if (sample == null) {
                    continue;
                }

                float normalized = Mth.clamp(distance / Math.max(width, 0.001F), 0.0F, 1.0F);
                int localDepth = depth + Mth.floor((1.0F - normalized) * 2.5F);
                if (spring) {
                    localDepth = Math.max(localDepth, 3);
                }
                int floorY = waterY - localDepth;
                int clearTopY = Math.max(sample.surfaceY + 2, waterY + 1);
                boolean flooded = normalized < 0.9F || spring;

                for (int y = clearTopY; y > floorY; y--) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    if (y <= waterY && flooded) {
                        if (!state.is(Blocks.WATER)) {
                            level.setBlock(mutable, Blocks.WATER.defaultBlockState(), 2);
                        }
                    } else if (canCarve(state)) {
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                mutable.set(x, floorY, z);
                level.setBlock(mutable, pickRiverbedBlock(random, downstream, waterY, seaLevel), 2);

                if (!flooded) {
                    mutable.set(x, floorY + 1, z);
                    if (canCarve(level.getBlockState(mutable))) {
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private BlockState pickRiverbedBlock(RandomSource random, float downstream, int waterY, int seaLevel) {
        if (downstream > 0.8F && random.nextInt(6) == 0) {
            return Blocks.CLAY.defaultBlockState();
        }
        if (waterY <= seaLevel + 4) {
            return random.nextBoolean() ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        }
        if (downstream > 0.45F && random.nextInt(4) == 0) {
            return Blocks.SAND.defaultBlockState();
        }
        return Blocks.GRAVEL.defaultBlockState();
    }

    private boolean isRiverSourceSurface(BlockState state) {
        return !state.isAir()
                && !state.liquid()
                && !state.is(Blocks.BEDROCK)
                && !state.is(Blocks.LAVA)
                && !state.is(Blocks.WATER)
                && !state.is(BlockTags.LOGS)
                && !state.is(BlockTags.LEAVES);
    }

    private boolean canCarve(BlockState state) {
        return !state.isAir()
                && !state.is(Blocks.BEDROCK)
                && !state.is(Blocks.END_PORTAL_FRAME)
                && !state.is(Blocks.END_PORTAL)
                && !state.is(Blocks.NETHER_PORTAL);
    }

    private SurfaceSample sampleSurface(WorldGenLevel level, int x, int z) {
        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int surfaceY = height - 1;
        if (surfaceY < level.getMinBuildHeight() + 2) {
            return null;
        }

        BlockPos pos = new BlockPos(x, surfaceY, z);
        BlockState state = level.getBlockState(pos);
        boolean isWater = state.is(Blocks.WATER);
        if (state.isAir()) {
            int oceanFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            if (oceanFloor < level.getMinBuildHeight() + 2) {
                return null;
            }
            pos = new BlockPos(x, oceanFloor, z);
            state = level.getBlockState(pos);
            surfaceY = oceanFloor;
            isWater = state.is(Blocks.WATER);
        }

        return new SurfaceSample(surfaceY, state, isWater);
    }

    private boolean isWaterSurface(WorldGenLevel level, int x, int z) {
        SurfaceSample sample = sampleSurface(level, x, z);
        return sample != null && sample.isWater;
    }

    private long pack(int x, int z) {
        return ((long) x & 4294967295L) << 32 | ((long) z & 4294967295L);
    }

    private record SurfaceSample(int surfaceY, BlockState state, boolean isWater) {
    }

    private record RiverNode(int x, int z, int surfaceY, int waterY) {
    }

    private record Direction2D(int dx, int dz) {
        int dot(Direction2D other) {
            return dx * other.dx + dz * other.dz;
        }
    }
}
