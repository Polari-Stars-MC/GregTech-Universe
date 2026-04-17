package org.polaris2023.gtu.space.runtime.ksp;

import org.polaris2023.gtu.space.runtime.math.SpaceVector;

public final class KspRuntimeDiagnostics {
    private KspRuntimeDiagnostics() {
    }

    public static void main(String[] args) throws InterruptedException {
        KspBackgroundSystem backgroundSystem = new KspBackgroundSystem(KspSystemDefinition.solarSystem());

        for (int sample = 0; sample < 6; sample++) {
            for (int i = 0; i < 20; i++) {
                backgroundSystem.tick();
            }
            KspSnapshot snapshot = backgroundSystem.latestSnapshot();

            KspBodyState earth = snapshot.bodies().get("earth");
            KspBodyState moon = snapshot.bodies().get("moon");
            KspVesselState probe = snapshot.vessels().get("starter_probe");

            double probeEarthDistance = distance(probe.absolutePosition(), earth.absolutePosition());
            double probeMoonDistance = distance(probe.absolutePosition(), moon.absolutePosition());
            double earthMoonDistance = distance(earth.absolutePosition(), moon.absolutePosition());

            System.out.printf(
                    "[sample=%d] tick=%d simT=%.2fs universe=%s galaxy=%s scale=%s primary=%s probe=(%.0f, %.0f, %.0f) vel=(%.2f, %.2f, %.2f)%n",
                    sample,
                    snapshot.simulationTick(),
                    snapshot.simulationSeconds(),
                    snapshot.universe().universeId(),
                    snapshot.universe().galaxy().galaxyId(),
                    snapshot.universe().scaleFactor().toPlainString(),
                    probe.primaryBodyId(),
                    probe.absolutePosition().x(),
                    probe.absolutePosition().y(),
                    probe.absolutePosition().z(),
                    probe.absoluteVelocity().x(),
                    probe.absoluteVelocity().y(),
                    probe.absoluteVelocity().z()
            );
            System.out.printf(
                    "           systemUniverse=(%s, %s, %s) dist=%s effRate=%s earth=(%.0f, %.0f, %.0f) vEarth=(%.2f, %.2f, %.2f) moon=(%.0f, %.0f, %.0f) vMoon=(%.2f, %.2f, %.2f) dProbeEarth=%.0f dProbeMoon=%.0f dEarthMoon=%.0f%n",
                    snapshot.universe().systemUniversePosition().x().toPlainString(),
                    snapshot.universe().systemUniversePosition().y().toPlainString(),
                    snapshot.universe().systemUniversePosition().z().toPlainString(),
                    snapshot.universe().systemDistanceFromOrigin().toPlainString(),
                    snapshot.universe().effectiveExpansionRatePerSecond().toPlainString(),
                    earth.absolutePosition().x(),
                    earth.absolutePosition().y(),
                    earth.absolutePosition().z(),
                    earth.absoluteVelocity().x(),
                    earth.absoluteVelocity().y(),
                    earth.absoluteVelocity().z(),
                    moon.absolutePosition().x(),
                    moon.absolutePosition().y(),
                    moon.absolutePosition().z(),
                    moon.absoluteVelocity().x(),
                    moon.absoluteVelocity().y(),
                    moon.absoluteVelocity().z(),
                    probeEarthDistance,
                    probeMoonDistance,
                    earthMoonDistance
            );
        }

        backgroundSystem.close();
    }

    private static double distance(SpaceVector a, SpaceVector b) {
        return a.subtract(b).length();
    }
}
