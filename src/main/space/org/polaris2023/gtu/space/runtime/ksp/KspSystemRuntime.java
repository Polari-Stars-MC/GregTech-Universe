package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.SpaceVector;
import org.polaris2023.gtu.space.runtime.math.UniverseVector;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;

public final class KspSystemRuntime implements AutoCloseable {
    private static final int MAX_BODY_HISTORY = 4096;

    private final KspSystemDefinition definition;
    private final Map<String, KspBodyDefinition> bodyDefinitions = new LinkedHashMap<>();
    private final Map<String, DynamicBodyState> bodyStates = new LinkedHashMap<>();
    private final Map<String, ArrayDeque<SpaceVector>> bodyHistory = new LinkedHashMap<>();
    private final Map<String, KspVesselState> vesselStates = new LinkedHashMap<>();
    private final List<List<String>> bodyLayers;
    private final ForkJoinPool bodyPool;

    private long simulationTick;
    private double simulationSeconds;

    public KspSystemRuntime(KspSystemDefinition definition) {
        this.definition = definition;
        for (KspBodyDefinition body : definition.flattenBodies()) {
            bodyDefinitions.put(body.id(), body);
        }
        this.bodyLayers = computeBodyLayers();
        this.bodyPool = new ForkJoinPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
                pool -> {
                    ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    thread.setDaemon(true);
                    thread.setName("gtu-space-body-" + thread.getPoolIndex());
                    return thread;
                },
                null,
                false
        );
        initializeBodyStates();
        KspUniverseState initialUniverseState = universeStateAtCurrentTime();
        for (KspVesselState vessel : definition.vessels()) {
            vesselStates.put(vessel.id(), vessel.withUniverseAnchor(
                    initialUniverseState.universeId(),
                    initialUniverseState.galaxy().galaxyId(),
                    definition.systemId(),
                    initialUniverseState.systemUniversePosition()
            ));
        }
    }

    public synchronized void step(double deltaSeconds) {
        simulationTick++;
        simulationSeconds += deltaSeconds;
        propagateBodiesKeplerian();
        advanceVessels(deltaSeconds);
    }

    public synchronized KspSnapshot snapshot() {
        return new KspSnapshot(
                simulationTick,
                simulationSeconds,
                universeStateAtCurrentTime(),
                Map.copyOf(publicBodyStates()),
                Map.copyOf(publicBodyHistory()),
                Map.copyOf(vesselStates)
        );
    }

    private void initializeBodyStates() {
        Map<String, DynamicBodyState> cache = new LinkedHashMap<>();
        for (KspBodyDefinition body : definition.flattenBodies()) {
            computeInitialBodyState(body.id(), cache);
        }
        bodyStates.putAll(cache);
        for (Map.Entry<String, DynamicBodyState> entry : bodyStates.entrySet()) {
            ArrayDeque<SpaceVector> history = new ArrayDeque<>();
            history.addLast(entry.getValue().position);
            bodyHistory.put(entry.getKey(), history);
        }
    }

    private DynamicBodyState computeInitialBodyState(String bodyId, Map<String, DynamicBodyState> cache) {
        DynamicBodyState cached = cache.get(bodyId);
        if (cached != null) {
            return cached;
        }

        KspBodyDefinition body = bodyDefinitions.get(bodyId);
        if (body == null) {
            throw new IllegalStateException("Unknown body " + bodyId);
        }

        SpaceVector absolutePosition;
        SpaceVector absoluteVelocity;
        String referenceBodyId = body.defaultReferenceBodyId();
        if (body.referenceFrameKind() != KspReferenceFrameKind.SYSTEM_CENTER) {
            DynamicBodyState parent = computeInitialBodyState(referenceBodyId, cache);
            KspBodyDefinition parentDefinition = bodyDefinitions.get(referenceBodyId);
            double centralMu = parentDefinition == null ? body.gravitationalParameter() : parentDefinition.gravitationalParameter();
            KspBodyDefinition.OrbitalState relativeState = body.stateAt(0.0, centralMu);
            absolutePosition = parent.position.add(relativeState.position());
            absoluteVelocity = parent.velocity.add(relativeState.velocity());
        } else {
            KspBodyDefinition.OrbitalState relativeState = body.stateAt(0.0, body.gravitationalParameter());
            absolutePosition = relativeState.position();
            absoluteVelocity = relativeState.velocity();
        }

        DynamicBodyState state = new DynamicBodyState(body, referenceBodyId, absolutePosition, absoluteVelocity);
        cache.put(bodyId, state);
        return state;
    }

    private void propagateBodiesKeplerian() {
        Map<String, DynamicBodyState> computed = new ConcurrentHashMap<>();
        for (List<String> layer : bodyLayers) {
            if (layer.size() <= 1) {
                for (String bodyId : layer) {
                    computeBodyStateAt(bodyId, simulationSeconds, computed);
                }
            } else {
                List<ForkJoinTask<?>> tasks = new ArrayList<>(layer.size());
                for (String bodyId : layer) {
                    tasks.add(bodyPool.submit(() -> computeBodyStateAt(bodyId, simulationSeconds, computed)));
                }
                for (ForkJoinTask<?> task : tasks) {
                    task.join();
                }
            }
        }
        bodyStates.clear();
        bodyStates.putAll(computed);
        for (Map.Entry<String, DynamicBodyState> entry : computed.entrySet()) {
            appendBodyHistory(entry.getKey(), entry.getValue().position);
        }
    }

    private DynamicBodyState computeBodyStateAt(String bodyId, double time, Map<String, DynamicBodyState> cache) {
        DynamicBodyState cached = cache.get(bodyId);
        if (cached != null) {
            return cached;
        }

        KspBodyDefinition body = bodyDefinitions.get(bodyId);
        if (body == null) {
            throw new IllegalStateException("Unknown body " + bodyId);
        }

        SpaceVector absolutePosition;
        SpaceVector absoluteVelocity;
        String referenceBodyId = body.defaultReferenceBodyId();

        if (body.referenceFrameKind() == KspReferenceFrameKind.SYSTEM_CENTER) {
            KspBodyDefinition.OrbitalState state = body.stateAt(time);
            absolutePosition = state.position();
            absoluteVelocity = state.velocity();
        } else {
            DynamicBodyState parent = computeBodyStateAt(referenceBodyId, time, cache);
            KspBodyDefinition parentDef = bodyDefinitions.get(referenceBodyId);
            double centralMu = parentDef != null ? parentDef.gravitationalParameter() : body.gravitationalParameter();
            KspBodyDefinition.OrbitalState relState = body.stateAt(time, centralMu);
            absolutePosition = parent.position.add(relState.position());
            absoluteVelocity = parent.velocity.add(relState.velocity());
        }

        DynamicBodyState state = new DynamicBodyState(body, referenceBodyId, absolutePosition, absoluteVelocity);
        cache.put(bodyId, state);
        return state;
    }

    private void advanceVessels(double deltaSeconds) {
        Map<String, KspBodyState> bodies = publicBodyStates();
        KspUniverseState universeState = universeStateAtCurrentTime();
        Map<String, KspVesselState> nextVessels = new LinkedHashMap<>();

        for (KspVesselState vessel : vesselStates.values()) {
            SpaceVector startAcceleration = computeAccelerationFromBodies(vessel.absolutePosition(), bodies);
            SpaceVector nextPosition = vessel.absolutePosition()
                    .add(vessel.absoluteVelocity().scale(deltaSeconds))
                    .add(startAcceleration.scale(0.5 * deltaSeconds * deltaSeconds));
            SpaceVector endAcceleration = computeAccelerationFromBodies(nextPosition, bodies);
            SpaceVector nextVelocity = vessel.absoluteVelocity()
                    .add(startAcceleration.add(endAcceleration).scale(0.5 * deltaSeconds));

            String nextPrimaryBodyId = resolveSphereOfInfluence(nextPosition, vessel.primaryBodyId(), bodies);
            nextVessels.put(vessel.id(), vessel
                    .withState(nextPrimaryBodyId, nextPosition, nextVelocity)
                    .withUniverseAnchor(
                            universeState.universeId(),
                            universeState.galaxy().galaxyId(),
                            definition.systemId(),
                            universeState.systemUniversePosition()
                    ));
        }

        vesselStates.clear();
        vesselStates.putAll(nextVessels);
    }

    private Map<String, KspBodyState> publicBodyStates() {
        Map<String, KspBodyState> result = new LinkedHashMap<>();
        for (Map.Entry<String, DynamicBodyState> entry : bodyStates.entrySet()) {
            DynamicBodyState state = entry.getValue();
            result.put(entry.getKey(), new KspBodyState(state.definition, state.referenceBodyId, state.position, state.velocity));
        }
        return result;
    }

    private Map<String, List<SpaceVector>> publicBodyHistory() {
        Map<String, List<SpaceVector>> result = new LinkedHashMap<>();
        for (Map.Entry<String, ArrayDeque<SpaceVector>> entry : bodyHistory.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return result;
    }

    private void appendBodyHistory(String bodyId, SpaceVector position) {
        ArrayDeque<SpaceVector> history = bodyHistory.computeIfAbsent(bodyId, ignored -> new ArrayDeque<>());
        history.addLast(position);
        while (history.size() > MAX_BODY_HISTORY) {
            history.removeFirst();
        }
    }

    private List<List<String>> computeBodyLayers() {
        Map<String, Integer> depths = new LinkedHashMap<>();
        for (String bodyId : bodyDefinitions.keySet()) {
            computeBodyDepth(bodyId, depths);
        }
        int maxDepth = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<List<String>> layers = new ArrayList<>(maxDepth + 1);
        for (int d = 0; d <= maxDepth; d++) {
            List<String> layer = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : depths.entrySet()) {
                if (entry.getValue() == d) {
                    layer.add(entry.getKey());
                }
            }
            layers.add(List.copyOf(layer));
        }
        return List.copyOf(layers);
    }

    private int computeBodyDepth(String bodyId, Map<String, Integer> cache) {
        Integer cached = cache.get(bodyId);
        if (cached != null) {
            return cached;
        }
        KspBodyDefinition body = bodyDefinitions.get(bodyId);
        if (body.referenceFrameKind() == KspReferenceFrameKind.SYSTEM_CENTER) {
            cache.put(bodyId, 0);
            return 0;
        }
        int depth = computeBodyDepth(body.defaultReferenceBodyId(), cache) + 1;
        cache.put(bodyId, depth);
        return depth;
    }

    private KspUniverseState universeStateAtCurrentTime() {
        KspUniverseDefinition universe = definition.universe();
        KspGalaxyDefinition galaxy = definition.galaxy();
        BigDecimal elapsedSeconds = BigDecimal.valueOf(simulationSeconds);
        UniverseVector galaxyPosition = galaxy.universePosition().addScaled(galaxy.universeDriftPerSecond(), elapsedSeconds);
        UniverseVector systemOffset = definition.systemGalaxyPosition().addScaled(definition.systemGalaxyDriftPerSecond(), elapsedSeconds);
        UniverseVector systemUniversePosition = galaxyPosition.add(systemOffset);
        BigDecimal distance = BigDecimal.valueOf(systemUniversePosition.lengthEstimate());
        BigDecimal distanceMultiplier = distance
                .divide(universe.expansionReferenceDistance(), UniverseVector.MATH_CONTEXT)
                .max(universe.minimumExpansionMultiplier());
        BigDecimal effectiveExpansionRate = universe.baseExpansionRatePerSecond()
                .multiply(distanceMultiplier, UniverseVector.MATH_CONTEXT);
        BigDecimal scaleFactor = universe.initialScaleFactor()
                .add(effectiveExpansionRate.multiply(elapsedSeconds, UniverseVector.MATH_CONTEXT), UniverseVector.MATH_CONTEXT);

        return new KspUniverseState(
                universe.universeId(),
                new KspGalaxyState(
                        galaxy.galaxyId(),
                        galaxy.displayName(),
                        galaxyPosition,
                        galaxy.universeDriftPerSecond()
                ),
                systemUniversePosition,
                definition.systemGalaxyDriftPerSecond().add(galaxy.universeDriftPerSecond()),
                distance,
                scaleFactor,
                effectiveExpansionRate
        );
    }

    private String resolveSphereOfInfluence(SpaceVector vesselPosition, String currentPrimaryId, Map<String, KspBodyState> bodies) {
        String bestBodyId = currentPrimaryId;
        double bestDistance = Double.MAX_VALUE;

        for (KspBodyState body : bodies.values()) {
            if (body.definition().sphereOfInfluence() <= 0.0) {
                continue;
            }

            double distance = vesselPosition.subtract(body.absolutePosition()).length();
            if (distance <= body.definition().sphereOfInfluence() && distance < bestDistance) {
                bestDistance = distance;
                bestBodyId = body.definition().id();
            }
        }

        return bestBodyId;
    }

    private SpaceVector computeAccelerationFromBodies(SpaceVector targetPosition, Map<String, KspBodyState> bodies) {
        SpaceVector total = SpaceVector.zero();
        for (KspBodyState body : bodies.values()) {
            total = total.add(computePointMassAcceleration(targetPosition, body.absolutePosition(), body.definition().gravitationalParameter()));
        }
        return total;
    }

    private SpaceVector computePointMassAcceleration(SpaceVector targetPosition, SpaceVector sourcePosition, double mu) {
        SpaceVector delta = sourcePosition.subtract(targetPosition);
        double distanceSquared = Math.max(delta.lengthSquared(), 1.0);
        double distance = Math.sqrt(distanceSquared);
        double acceleration = mu / distanceSquared;
        return delta.scale(acceleration / distance);
    }

    private KspBodyState fallbackPrimary(Map<String, KspBodyState> bodies) {
        KspBodyState primary = bodies.get(definition.fallbackPrimaryBodyId());
        if (primary == null && !bodies.isEmpty()) {
            return bodies.values().iterator().next();
        }
        return primary;
    }

    public synchronized void redirectReferenceBody(String bodyId, String nextReferenceBodyId) {
        throw new UnsupportedOperationException(
                "Runtime SOI capture for bodies requires orbital element recomputation — not yet implemented. Body: " + bodyId + " -> " + nextReferenceBodyId
        );
    }

    @Override
    public void close() {
        bodyPool.shutdownNow();
    }

    private record DynamicBodyState(
            KspBodyDefinition definition,
            String referenceBodyId,
            SpaceVector position,
            SpaceVector velocity
    ) {
    }
}
