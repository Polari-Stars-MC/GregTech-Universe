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
    private static final double TWO_PI = Math.PI * 2.0;

    private final KspSystemDefinition definition;
    private final Map<String, KspBodyDefinition> bodyDefinitions = new LinkedHashMap<>();
    private final Map<String, DynamicBodyState> bodyStates = new LinkedHashMap<>();
    private final Map<String, ArrayDeque<SpaceVector>> bodyHistory = new LinkedHashMap<>();
    private final Map<String, KspVesselState> vesselStates = new LinkedHashMap<>();
    private final Map<String, SpaceVector> bodyThrust = new LinkedHashMap<>();
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
        propagateBodies(deltaSeconds);
        advanceVessels(deltaSeconds);
    }

    public synchronized KspSnapshot snapshot() {
        Map<String, KspBodyState> bodies = publicBodyStates();
        List<RocheViolation> rocheViolations = computeRocheViolations(bodies);
        return new KspSnapshot(
                simulationTick,
                simulationSeconds,
                universeStateAtCurrentTime(),
                Map.copyOf(bodies),
                Map.copyOf(publicBodyHistory()),
                Map.copyOf(vesselStates),
                rocheViolations
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

    private void propagateBodies(double deltaSeconds) {
        Map<String, DynamicBodyState> computed = new ConcurrentHashMap<>();
        for (List<String> layer : bodyLayers) {
            if (layer.size() <= 1) {
                for (String bodyId : layer) {
                    resolveBodyState(bodyId, deltaSeconds, computed);
                }
            } else {
                List<ForkJoinTask<?>> tasks = new ArrayList<>(layer.size());
                for (String bodyId : layer) {
                    tasks.add(bodyPool.submit(() -> resolveBodyState(bodyId, deltaSeconds, computed)));
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

    private DynamicBodyState resolveBodyState(String bodyId, double deltaSeconds, Map<String, DynamicBodyState> computed) {
        DynamicBodyState cached = computed.get(bodyId);
        if (cached != null) {
            return cached;
        }

        SpaceVector thrust = bodyThrust.get(bodyId);
        DynamicBodyState state;
        if (thrust != null) {
            state = integrateBodyVerlet(bodyId, deltaSeconds, thrust, computed);
        } else {
            state = computeBodyStateAt(bodyId, simulationSeconds, computed);
        }
        computed.put(bodyId, state);
        return state;
    }

    private DynamicBodyState integrateBodyVerlet(String bodyId, double dt, SpaceVector thrust,
                                                  Map<String, DynamicBodyState> computed) {
        KspBodyDefinition body = bodyDefinitions.get(bodyId);
        DynamicBodyState previous = bodyStates.get(bodyId);

        SpaceVector gravity = computeGravityOnBody(bodyId, previous.position, computed);
        SpaceVector accel = gravity.add(thrust);

        SpaceVector nextPos = previous.position
                .add(previous.velocity.scale(dt))
                .add(accel.scale(0.5 * dt * dt));

        SpaceVector endGravity = computeGravityOnBody(bodyId, nextPos, computed);
        SpaceVector endAccel = endGravity.add(thrust);
        SpaceVector nextVel = previous.velocity
                .add(accel.add(endAccel).scale(0.5 * dt));

        return new DynamicBodyState(body, previous.referenceBodyId, nextPos, nextVel);
    }

    private SpaceVector computeGravityOnBody(String bodyId, SpaceVector position,
                                              Map<String, DynamicBodyState> computed) {
        SpaceVector total = SpaceVector.zero();
        for (Map.Entry<String, KspBodyDefinition> entry : bodyDefinitions.entrySet()) {
            if (entry.getKey().equals(bodyId)) {
                continue;
            }
            DynamicBodyState other = computed.get(entry.getKey());
            if (other == null) {
                other = bodyStates.get(entry.getKey());
            }
            if (other == null) {
                continue;
            }
            total = total.add(computePointMassAcceleration(position, other.position,
                    entry.getValue().gravitationalParameter()));
        }
        return total;
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
            SpaceVector thrust = bodyThrust.getOrDefault(entry.getKey(), SpaceVector.zero());
            boolean perturbed = bodyThrust.containsKey(entry.getKey());
            double rotationPhase = state.definition.rotationPeriodSeconds() > 0
                    ? ((simulationSeconds / state.definition.rotationPeriodSeconds()) * TWO_PI) % TWO_PI
                    : 0.0;
            result.put(entry.getKey(), new KspBodyState(state.definition, state.referenceBodyId, state.position, state.velocity, perturbed, thrust, rotationPhase));
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

    public synchronized void applyBodyThrust(String bodyId, SpaceVector thrustAcceleration) {
        if (!bodyDefinitions.containsKey(bodyId)) {
            throw new IllegalArgumentException("Unknown body: " + bodyId);
        }
        bodyThrust.put(bodyId, thrustAcceleration);
    }

    public synchronized void clearBodyThrust(String bodyId) {
        if (bodyThrust.remove(bodyId) != null) {
            recomputeOrbitalElements(bodyId);
        }
    }

    public synchronized boolean isBodyPerturbed(String bodyId) {
        return bodyThrust.containsKey(bodyId);
    }

    public synchronized void redirectReferenceBody(String bodyId, String nextReferenceBodyId) {
        DynamicBodyState current = bodyStates.get(bodyId);
        if (current == null) {
            throw new IllegalArgumentException("Unknown body: " + bodyId);
        }
        if (!bodyDefinitions.containsKey(nextReferenceBodyId)) {
            throw new IllegalArgumentException("Unknown reference body: " + nextReferenceBodyId);
        }
        bodyStates.put(bodyId, new DynamicBodyState(current.definition, nextReferenceBodyId, current.position, current.velocity));
        if (!bodyThrust.containsKey(bodyId)) {
            recomputeOrbitalElements(bodyId);
        }
    }

    private void recomputeOrbitalElements(String bodyId) {
        DynamicBodyState current = bodyStates.get(bodyId);
        if (current == null) {
            return;
        }
        KspBodyDefinition oldDef = current.definition;
        if (oldDef.referenceFrameKind() == KspReferenceFrameKind.SYSTEM_CENTER) {
            return;
        }
        DynamicBodyState parent = bodyStates.get(current.referenceBodyId);
        if (parent == null) {
            return;
        }

        double mu = parent.definition.gravitationalParameter();
        SpaceVector r = current.position.subtract(parent.position);
        SpaceVector v = current.velocity.subtract(parent.velocity);
        double rMag = r.length();
        double vMag = v.length();

        if (rMag < 1.0E-9) {
            return;
        }

        SpaceVector h = cross(r, v);
        double hMag = h.length();
        if (hMag < 1.0E-9) {
            return;
        }

        double energy = vMag * vMag * 0.5 - mu / rMag;
        if (Math.abs(energy) < 1.0E-12) {
            return;
        }
        double a = -mu / (2.0 * energy);
        if (a <= 0.0) {
            return;
        }

        SpaceVector eVec = cross(v, h).scale(1.0 / mu).subtract(r.scale(1.0 / rMag));
        double e = eVec.length();
        if (e >= 1.0) {
            return;
        }

        SpaceVector w = new SpaceVector(h.x() / hMag, h.y() / hMag, h.z() / hMag);
        double incl = Math.acos(clampValue(-w.y(), -1.0, 1.0));

        double sinI = Math.sqrt(w.x() * w.x() + w.z() * w.z());
        double omega;
        if (sinI > 1.0E-9) {
            omega = Math.atan2(-w.x(), w.z());
        } else {
            omega = 0.0;
        }

        double argPeri;
        if (sinI > 1.0E-9 && e > 1.0E-9) {
            SpaceVector nDir = new SpaceVector(w.z(), 0.0, -w.x()).normalize();
            double cosArgPeri = clampValue(nDir.dot(eVec.normalize()), -1.0, 1.0);
            argPeri = Math.acos(cosArgPeri);
            if (eVec.y() < 0.0) {
                argPeri = TWO_PI - argPeri;
            }
        } else if (e > 1.0E-9) {
            argPeri = Math.atan2(eVec.z(), eVec.x());
            if (argPeri < 0.0) {
                argPeri += TWO_PI;
            }
        } else {
            argPeri = 0.0;
        }

        double trueAnomaly;
        if (e > 1.0E-9) {
            double cosNu = clampValue(eVec.normalize().dot(r.normalize()), -1.0, 1.0);
            trueAnomaly = Math.acos(cosNu);
            if (r.dot(v) < 0.0) {
                trueAnomaly = TWO_PI - trueAnomaly;
            }
        } else {
            if (sinI > 1.0E-9) {
                SpaceVector nDir = new SpaceVector(w.z(), 0.0, -w.x()).normalize();
                double cosNu = clampValue(nDir.dot(r.normalize()), -1.0, 1.0);
                trueAnomaly = Math.acos(cosNu);
                if (r.dot(v) < 0.0) {
                    trueAnomaly = TWO_PI - trueAnomaly;
                }
            } else {
                trueAnomaly = Math.atan2(r.z(), r.x());
                if (trueAnomaly < 0.0) {
                    trueAnomaly += TWO_PI;
                }
            }
        }

        double cosNu = Math.cos(trueAnomaly);
        double sinNu = Math.sin(trueAnomaly);
        double eccentricAnomaly = Math.atan2(Math.sqrt(Math.max(0.0, 1.0 - e * e)) * sinNu, e + cosNu);
        double meanAnomaly = eccentricAnomaly - e * Math.sin(eccentricAnomaly);
        if (meanAnomaly < 0.0) {
            meanAnomaly += TWO_PI;
        }

        double period = TWO_PI * Math.sqrt(a * a * a / mu);

        KspBodyDefinition newDef = new KspBodyDefinition(
                oldDef.id(), oldDef.displayName(), oldDef.bodyKind(), oldDef.referenceFrameKind(),
                current.referenceBodyId, oldDef.referenceCaptureAllowed(),
                oldDef.initialPosition(), oldDef.initialVelocity(),
                oldDef.gravitationalParameter(), oldDef.radius(), oldDef.sphereOfInfluence(),
                oldDef.rotationPeriodSeconds(), oldDef.rotationAxis(),
                a, e, incl, omega, argPeri, meanAnomaly,
                period, simulationSeconds,
                oldDef.children()
        );

        bodyDefinitions.put(bodyId, newDef);
        bodyStates.put(bodyId, new DynamicBodyState(newDef, current.referenceBodyId, current.position, current.velocity));
    }

    private List<RocheViolation> computeRocheViolations(Map<String, KspBodyState> bodies) {
        List<RocheViolation> violations = new ArrayList<>();
        for (KspBodyState body : bodies.values()) {
            if (body.definition().referenceFrameKind() == KspReferenceFrameKind.SYSTEM_CENTER) {
                continue;
            }
            KspBodyState parent = bodies.get(body.referenceBodyId());
            if (parent == null) {
                continue;
            }
            double roche = parent.definition().rocheLimit(body.definition());
            if (roche <= 0.0) {
                continue;
            }
            double distance = body.absolutePosition().subtract(parent.absolutePosition()).length();
            if (distance < roche) {
                violations.add(new RocheViolation(body.definition().id(), parent.definition().id(), distance, roche));
            }
        }
        for (KspVesselState vessel : vesselStates.values()) {
            KspBodyState primary = bodies.get(vessel.primaryBodyId());
            if (primary == null) {
                continue;
            }
            double distance = vessel.absolutePosition().subtract(primary.absolutePosition()).length();
            double roche = primary.definition().radius() * 2.4554;
            if (distance < roche) {
                violations.add(new RocheViolation(vessel.id(), primary.definition().id(), distance, roche));
            }
        }
        return List.copyOf(violations);
    }

    public synchronized KspSaveData exportState() {
        KspSaveData data = new KspSaveData();
        data.simulationTick = simulationTick;
        data.simulationSeconds = simulationSeconds;
        for (Map.Entry<String, DynamicBodyState> entry : bodyStates.entrySet()) {
            KspSaveData.BodyStateData bsd = new KspSaveData.BodyStateData();
            bsd.position = vecToArray(entry.getValue().position);
            bsd.velocity = vecToArray(entry.getValue().velocity);
            bsd.referenceBodyId = entry.getValue().referenceBodyId;
            data.bodyStates.put(entry.getKey(), bsd);
        }
        for (Map.Entry<String, KspVesselState> entry : vesselStates.entrySet()) {
            KspVesselState v = entry.getValue();
            KspSaveData.VesselStateData vsd = new KspSaveData.VesselStateData();
            vsd.id = v.id();
            vsd.name = v.name();
            vsd.universeId = v.universeId();
            vsd.galaxyId = v.galaxyId();
            vsd.systemId = v.systemId();
            vsd.universeAnchorPosition = new String[]{
                    v.universeAnchorPosition().x().toPlainString(),
                    v.universeAnchorPosition().y().toPlainString(),
                    v.universeAnchorPosition().z().toPlainString()
            };
            vsd.primaryBodyId = v.primaryBodyId();
            vsd.dryMass = v.dryMass();
            vsd.absolutePosition = vecToArray(v.absolutePosition());
            vsd.absoluteVelocity = vecToArray(v.absoluteVelocity());
            data.vesselStates.put(entry.getKey(), vsd);
        }
        for (Map.Entry<String, SpaceVector> entry : bodyThrust.entrySet()) {
            data.bodyThrust.put(entry.getKey(), vecToArray(entry.getValue()));
        }
        return data;
    }

    public synchronized void importState(KspSaveData data) {
        if (data == null) {
            return;
        }
        simulationTick = data.simulationTick;
        simulationSeconds = data.simulationSeconds;
        for (Map.Entry<String, KspSaveData.BodyStateData> entry : data.bodyStates.entrySet()) {
            DynamicBodyState existing = bodyStates.get(entry.getKey());
            if (existing == null) {
                continue;
            }
            KspSaveData.BodyStateData bsd = entry.getValue();
            SpaceVector pos = arrayToVec(bsd.position);
            SpaceVector vel = arrayToVec(bsd.velocity);
            String refId = bsd.referenceBodyId != null ? bsd.referenceBodyId : existing.referenceBodyId;
            bodyStates.put(entry.getKey(), new DynamicBodyState(existing.definition, refId, pos, vel));
        }
        vesselStates.clear();
        for (Map.Entry<String, KspSaveData.VesselStateData> entry : data.vesselStates.entrySet()) {
            KspSaveData.VesselStateData vsd = entry.getValue();
            vesselStates.put(entry.getKey(), new KspVesselState(
                    vsd.id, vsd.name, vsd.universeId, vsd.galaxyId, vsd.systemId,
                    arrayToUniverseVec(vsd.universeAnchorPosition),
                    vsd.primaryBodyId, vsd.dryMass,
                    arrayToVec(vsd.absolutePosition), arrayToVec(vsd.absoluteVelocity)
            ));
        }
        bodyThrust.clear();
        if (data.bodyThrust != null) {
            for (Map.Entry<String, double[]> entry : data.bodyThrust.entrySet()) {
                bodyThrust.put(entry.getKey(), arrayToVec(entry.getValue()));
            }
        }
        bodyHistory.clear();
        for (Map.Entry<String, DynamicBodyState> entry : bodyStates.entrySet()) {
            ArrayDeque<SpaceVector> history = new ArrayDeque<>();
            history.addLast(entry.getValue().position);
            bodyHistory.put(entry.getKey(), history);
        }
    }

    private static double[] vecToArray(SpaceVector v) {
        return new double[]{v.x(), v.y(), v.z()};
    }

    private static SpaceVector arrayToVec(double[] a) {
        if (a == null || a.length < 3) return SpaceVector.zero();
        return new SpaceVector(a[0], a[1], a[2]);
    }

    private static UniverseVector arrayToUniverseVec(String[] a) {
        if (a == null || a.length < 3) return UniverseVector.zero();
        return new UniverseVector(
                new java.math.BigDecimal(a[0]),
                new java.math.BigDecimal(a[1]),
                new java.math.BigDecimal(a[2])
        );
    }

    @Override
    public void close() {
        bodyPool.shutdownNow();
    }

    private static SpaceVector cross(SpaceVector a, SpaceVector b) {
        return new SpaceVector(
                a.y() * b.z() - a.z() * b.y(),
                a.z() * b.x() - a.x() * b.z(),
                a.x() * b.y() - a.y() * b.x()
        );
    }

    private static double clampValue(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DynamicBodyState(
            KspBodyDefinition definition,
            String referenceBodyId,
            SpaceVector position,
            SpaceVector velocity
    ) {
    }
}
