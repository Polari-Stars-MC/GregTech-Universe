package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.SpaceVector;
import org.polaris2023.gtu.space.runtime.math.UniverseVector;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record KspSystemDefinition(
        String systemId,
        String fallbackPrimaryBodyId,
        KspUniverseDefinition universe,
        KspGalaxyDefinition galaxy,
        UniverseVector systemGalaxyPosition,
        UniverseVector systemGalaxyDriftPerSecond,
        SpaceVector systemCenterPosition,
        SpaceVector systemCenterVelocity,
        List<KspBodyDefinition> stars,
        List<KspVesselState> vessels
) {
    public static final String SYSTEM_CENTER_ID = "system_center";

    public KspSystemDefinition {
        Objects.requireNonNull(systemId, "systemId");
        Objects.requireNonNull(fallbackPrimaryBodyId, "fallbackPrimaryBodyId");
        Objects.requireNonNull(universe, "universe");
        Objects.requireNonNull(galaxy, "galaxy");
        Objects.requireNonNull(systemGalaxyPosition, "systemGalaxyPosition");
        Objects.requireNonNull(systemGalaxyDriftPerSecond, "systemGalaxyDriftPerSecond");
        Objects.requireNonNull(systemCenterPosition, "systemCenterPosition");
        Objects.requireNonNull(systemCenterVelocity, "systemCenterVelocity");
        stars = List.copyOf(stars);
        vessels = List.copyOf(vessels);
        validateHierarchy(fallbackPrimaryBodyId, stars);
    }

    public static KspSystemDefinition earthMoonLike() {
        return solarSystem();
    }

    public static List<KspSystemDefinition> knownSystems() {
        return List.of(solarSystem(), alphaCentauriPrototype());
    }

    public static KspSystemDefinition solarSystem() {
        KspUniverseDefinition universe = new KspUniverseDefinition(
                "observable_universe",
                BigDecimal.ONE,
                new BigDecimal("0.000000000000000001"),
                new BigDecimal("1.000000000000000000000000E22"),
                BigDecimal.ONE
        );
        KspGalaxyDefinition galaxy = new KspGalaxyDefinition(
                "milky_way",
                "Milky Way",
                new UniverseVector(
                        new BigDecimal("2.100000000000000000000000E24"),
                        new BigDecimal("-1.500000000000000000000000E23"),
                        new BigDecimal("0.000000000000000000000000E0")
                ),
                UniverseVector.zero()
        );
        UniverseVector systemGalaxyPosition = new UniverseVector(
                new BigDecimal("2.650000000000000000000000E23"),
                new BigDecimal("1.180000000000000000000000E22"),
                new BigDecimal("-4.400000000000000000000000E21")
        );
        double km = 1_000.0;

        KspBodyDefinition moon = new KspBodyDefinition("moon", "Moon", KspBodyKind.SATELLITE, KspReferenceFrameKind.PLANET, "earth", true,
                SpaceVector.zero(), SpaceVector.zero(),
                4.9048695E12, 1_737.4 * km, 66_100 * km, 384_399 * km, 0.0549, deg(5.145), deg(125.08), deg(318.15), deg(115.3654),
                27.321661 * 24 * 3600, 0.0, List.of());

        KspBodyDefinition mercury = new KspBodyDefinition("mercury", "Mercury", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                2.2032E13, 2_439.7 * km, 112_000 * km, 57_909_227 * km, 0.20563593, deg(7.00487), deg(48.33167), deg(29.12478), deg(174.79588),
                87.9691 * 24 * 3600, 0.0, List.of());
        KspBodyDefinition venus = new KspBodyDefinition("venus", "Venus", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                3.24859E14, 6_051.8 * km, 616_000 * km, 108_209_475 * km, 0.00677672, deg(3.39471), deg(76.68069), deg(54.85229), deg(50.41611),
                224.701 * 24 * 3600, 0.0, List.of());
        KspBodyDefinition earth = new KspBodyDefinition("earth", "Earth", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                3.986004418E14, 6_371.0 * km, 924_000 * km, 149_598_023 * km, 0.0167086, deg(0.00005), deg(-11.26064), deg(114.20783), deg(357.51716),
                365.256363004 * 24 * 3600, 0.0, List.of(moon));
        KspBodyDefinition mars = new KspBodyDefinition("mars", "Mars", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                4.282837E13, 3_389.5 * km, 577_000 * km, 227_939_200 * km, 0.0934, deg(1.85061), deg(49.57854), deg(286.46230), deg(19.41248),
                686.98 * 24 * 3600, 0.0, List.of());
        KspBodyDefinition jupiter = new KspBodyDefinition("jupiter", "Jupiter", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                1.26686534E17, 69_911 * km, 48_200_000 * km, 778_340_821 * km, 0.0489, deg(1.30530), deg(100.55615), deg(273.8777), deg(20.0202),
                4332.59 * 24 * 3600, 0.0, List.of());
        KspBodyDefinition saturn = new KspBodyDefinition("saturn", "Saturn", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                3.7931187E16, 58_232 * km, 54_800_000 * km, 1_426_666_422 * km, 0.0565, deg(2.48446), deg(113.71504), deg(339.392), deg(317.0207),
                10759.22 * 24 * 3600, 0.0, List.of());
        KspBodyDefinition uranus = new KspBodyDefinition("uranus", "Uranus", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                5.793939E15, 25_362 * km, 51_900_000 * km, 2_870_658_186.0 * km, 0.04717, deg(0.76986), deg(74.22988), deg(96.998857), deg(142.2386),
                30688.5 * 24 * 3600, 0.0, List.of());
        KspBodyDefinition neptune = new KspBodyDefinition("neptune", "Neptune", KspBodyKind.PLANET, KspReferenceFrameKind.STAR, "sun", true,
                SpaceVector.zero(), SpaceVector.zero(),
                6.836529E15, 24_622 * km, 86_600_000 * km, 4_498_396_441.0 * km, 0.00858587, deg(1.76917), deg(131.72169), deg(273.187), deg(256.228),
                60182.0 * 24 * 3600, 0.0, List.of());
        List<KspBodyDefinition> asteroidBelt = createAsteroidBelt(km);

        KspBodyDefinition sun = new KspBodyDefinition("sun", "Sun", KspBodyKind.STAR, KspReferenceFrameKind.SYSTEM_CENTER, null, false,
                SpaceVector.zero(), SpaceVector.zero(),
                1.32712440018E20, 696_340 * km, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                mergeChildren(List.of(mercury, venus, earth, mars), asteroidBelt, List.of(jupiter, saturn, uranus, neptune)));

        List<KspBodyDefinition> stars = List.of(sun);
        Map<String, KspBodyDefinition> definitions = indexBodies(stars);
        AbsoluteBodyState earthState = absoluteStateAt("earth", definitions, 0.0);
        SpaceVector earthRadial = normalizeOrFallback(earthState.position(), new SpaceVector(1.0, 0.0, 0.0));
        SpaceVector earthNormal = normalizeOrFallback(cross(earthState.position(), earthState.velocity()), new SpaceVector(0.0, 1.0, 0.0));
        SpaceVector earthTangential = normalizeOrFallback(cross(earthNormal, earthRadial), new SpaceVector(0.0, 0.0, 1.0));

        double probeAltitude = 220_000.0;
        double probeSpeed = 7_780.0;

        return new KspSystemDefinition(
                "sol",
                "sun",
                universe,
                galaxy,
                systemGalaxyPosition,
                UniverseVector.zero(),
                SpaceVector.zero(),
                SpaceVector.zero(),
                stars,
                List.of(
                        new KspVesselState(
                                "starter_probe",
                                "Starter Probe",
                                universe.universeId(),
                                galaxy.galaxyId(),
                                "sol",
                                galaxy.universePosition().add(systemGalaxyPosition),
                                "earth",
                                2_000.0,
                                earthState.position().add(earthRadial.scale(6_371.0 * km + probeAltitude)),
                                earthState.velocity().add(earthTangential.scale(probeSpeed))
                        )
                )
        );
    }

    public static KspSystemDefinition alphaCentauriPrototype() {
        KspUniverseDefinition universe = new KspUniverseDefinition(
                "observable_universe",
                BigDecimal.ONE,
                new BigDecimal("0.000000000000000001"),
                new BigDecimal("1.000000000000000000000000E22"),
                BigDecimal.ONE
        );
        KspGalaxyDefinition galaxy = new KspGalaxyDefinition(
                "milky_way",
                "Milky Way",
                new UniverseVector(
                        new BigDecimal("2.100000000000000000000000E24"),
                        new BigDecimal("-1.500000000000000000000000E23"),
                        new BigDecimal("0.000000000000000000000000E0")
                ),
                UniverseVector.zero()
        );

        double km = 1_000.0;
        KspBodyDefinition alphaCentauriA = new KspBodyDefinition("alpha_centauri_a", "Alpha Centauri A", KspBodyKind.STAR, KspReferenceFrameKind.SYSTEM_CENTER, null, false,
                SpaceVector.zero(), SpaceVector.zero(),
                1.519e20, 834_840 * km, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                List.of());

        return new KspSystemDefinition(
                "alpha_centauri",
                "alpha_centauri_a",
                universe,
                galaxy,
                new UniverseVector(
                        new BigDecimal("2.920000000000000000000000E23"),
                        new BigDecimal("3.600000000000000000000000E22"),
                        new BigDecimal("8.100000000000000000000000E21")
                ),
                UniverseVector.zero(),
                SpaceVector.zero(),
                SpaceVector.zero(),
                List.of(alphaCentauriA),
                List.of()
        );
    }

    public List<KspBodyDefinition> flattenBodies() {
        List<KspBodyDefinition> flattened = new ArrayList<>();
        for (KspBodyDefinition star : stars) {
            flattenInto(star, flattened);
        }
        return List.copyOf(flattened);
    }

    private static void flattenInto(KspBodyDefinition body, List<KspBodyDefinition> target) {
        target.add(body);
        for (KspBodyDefinition child : body.children()) {
            flattenInto(child, target);
        }
    }

    private static List<KspBodyDefinition> createAsteroidBelt(double km) {
        List<KspBodyDefinition> asteroids = new ArrayList<>();
        double innerAuKm = 2.15 * 149_597_870.7;
        double outerAuKm = 3.35 * 149_597_870.7;
        int asteroidCount = 28;

        for (int i = 0; i < asteroidCount; i++) {
            double fraction = asteroidCount == 1 ? 0.0 : i / (double) (asteroidCount - 1);
            double semiMajorAxis = innerAuKm + (outerAuKm - innerAuKm) * fraction;
            double eccentricity = 0.03 + (i % 5) * 0.018;
            double inclination = deg(2.0 + (i % 7) * 1.65);
            double ascendingNode = deg((i * 37.0) % 360.0);
            double argumentOfPeriapsis = deg((i * 53.0 + 17.0) % 360.0);
            double meanAnomaly = deg((i * 29.0 + 11.0) % 360.0);
            double periodSeconds = Math.sqrt(Math.pow(semiMajorAxis / 149_597_870.7, 3.0)) * 365.256363004 * 24.0 * 3600.0;

            asteroids.add(new KspBodyDefinition(
                    "asteroid_belt_" + (i + 1),
                    "Asteroid " + (i + 1),
                    KspBodyKind.PLANET,
                    KspReferenceFrameKind.STAR,
                    "sun",
                    true,
                    SpaceVector.zero(),
                    SpaceVector.zero(),
                    1.0E7 + i * 2.5E5,
                    (40.0 + (i % 6) * 18.0) * km,
                    15_000.0 * km,
                    semiMajorAxis * km,
                    eccentricity,
                    inclination,
                    ascendingNode,
                    argumentOfPeriapsis,
                    meanAnomaly,
                    periodSeconds,
                    0.0,
                    List.of()
            ));
        }

        return List.copyOf(asteroids);
    }

    @SafeVarargs
    private static List<KspBodyDefinition> mergeChildren(List<KspBodyDefinition>... groups) {
        List<KspBodyDefinition> merged = new ArrayList<>();
        for (List<KspBodyDefinition> group : groups) {
            merged.addAll(group);
        }
        return List.copyOf(merged);
    }

    private static void validateHierarchy(String fallbackPrimaryBodyId, List<KspBodyDefinition> stars) {
        Map<String, KspBodyDefinition> definitions = indexBodies(stars);
        if (!definitions.containsKey(fallbackPrimaryBodyId)) {
            throw new IllegalArgumentException("Unknown fallback primary body: " + fallbackPrimaryBodyId);
        }

        for (KspBodyDefinition star : stars) {
            validateNode(star, null);
        }
    }

    private static void validateNode(KspBodyDefinition body, KspBodyDefinition parent) {
        if (parent == null) {
            if (body.bodyKind() != KspBodyKind.STAR || body.referenceFrameKind() != KspReferenceFrameKind.SYSTEM_CENTER) {
                throw new IllegalArgumentException("Top-level body must be a SYSTEM_CENTER STAR: " + body.id());
            }
            if (body.defaultReferenceBodyId() != null) {
                throw new IllegalArgumentException("Top-level STAR must not have reference body: " + body.id());
            }
        } else if (parent.bodyKind() == KspBodyKind.STAR) {
            if (body.bodyKind() != KspBodyKind.PLANET || body.referenceFrameKind() != KspReferenceFrameKind.STAR) {
                throw new IllegalArgumentException("Child of STAR must be STAR-relative PLANET: " + body.id());
            }
            if (!parent.id().equals(body.defaultReferenceBodyId())) {
                throw new IllegalArgumentException("PLANET reference body mismatch: " + body.id());
            }
        } else if (parent.bodyKind() == KspBodyKind.PLANET) {
            if (body.bodyKind() != KspBodyKind.SATELLITE || body.referenceFrameKind() != KspReferenceFrameKind.PLANET) {
                throw new IllegalArgumentException("Child of PLANET must be PLANET-relative SATELLITE: " + body.id());
            }
            if (!parent.id().equals(body.defaultReferenceBodyId())) {
                throw new IllegalArgumentException("SATELLITE reference body mismatch: " + body.id());
            }
        } else {
            throw new IllegalArgumentException("SATELLITE cannot have children: " + parent.id());
        }

        if (body.bodyKind() == KspBodyKind.SATELLITE && !body.children().isEmpty()) {
            throw new IllegalArgumentException("SATELLITE cannot contain child bodies: " + body.id());
        }

        for (KspBodyDefinition child : body.children()) {
            validateNode(child, body);
        }
    }

    private static Map<String, KspBodyDefinition> indexBodies(List<KspBodyDefinition> stars) {
        Map<String, KspBodyDefinition> definitions = new LinkedHashMap<>();
        for (KspBodyDefinition star : stars) {
            indexNode(star, definitions);
        }
        return definitions;
    }

    private static void indexNode(KspBodyDefinition body, Map<String, KspBodyDefinition> definitions) {
        KspBodyDefinition previous = definitions.put(body.id(), body);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate body id: " + body.id());
        }
        for (KspBodyDefinition child : body.children()) {
            indexNode(child, definitions);
        }
    }

    private static AbsoluteBodyState absoluteStateAt(String bodyId, Map<String, KspBodyDefinition> definitions, double elapsedSeconds) {
        KspBodyDefinition definition = definitions.get(bodyId);
        if (definition == null) {
            return new AbsoluteBodyState(SpaceVector.zero(), SpaceVector.zero());
        }

        if (definition.referenceFrameKind() == KspReferenceFrameKind.SYSTEM_CENTER) {
            KspBodyDefinition.OrbitalState relativeState = definition.stateAt(elapsedSeconds, definition.gravitationalParameter());
            return new AbsoluteBodyState(relativeState.position(), relativeState.velocity());
        }

        AbsoluteBodyState parent = absoluteStateAt(definition.defaultReferenceBodyId(), definitions, elapsedSeconds);
        KspBodyDefinition parentDefinition = definitions.get(definition.defaultReferenceBodyId());
        double centralMu = parentDefinition == null ? definition.gravitationalParameter() : parentDefinition.gravitationalParameter();
        KspBodyDefinition.OrbitalState relativeState = definition.stateAt(elapsedSeconds, centralMu);
        return new AbsoluteBodyState(
                parent.position().add(relativeState.position()),
                parent.velocity().add(relativeState.velocity())
        );
    }

    private static SpaceVector normalizeOrFallback(SpaceVector vector, SpaceVector fallback) {
        return vector.lengthSquared() < 1.0E-12 ? fallback : vector.normalize();
    }

    private static SpaceVector cross(SpaceVector a, SpaceVector b) {
        return new SpaceVector(
                a.y() * b.z() - a.z() * b.y(),
                a.z() * b.x() - a.x() * b.z(),
                a.x() * b.y() - a.y() * b.x()
        );
    }

    private static double deg(double degrees) {
        return Math.toRadians(degrees);
    }

    private record AbsoluteBodyState(SpaceVector position, SpaceVector velocity) {
    }
}
