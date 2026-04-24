package org.polaris2023.gtu.space.client.render;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.lwjgl.system.MemoryUtil;
import org.polaris2023.gtu.space.network.SpaceSnapshotPacket;
import org.polaris2023.gtu.space.simulation.ksp.KspBodyKind;
import org.polaris2023.gtu.space.simulation.ksp.KspBodyState;
import org.polaris2023.gtu.space.simulation.ksp.KspSnapshot;
import org.polaris2023.gtu.space.simulation.math.SpaceVector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CelestialBodyDataUBO extends UniformBuffer {

    public CelestialBodyDataUBO(int bindingPoint) {
        super(bindingPoint, 8192);
    }

    public void updateFromSnapshot(KspSnapshot snapshot, Vector3d cameraPos, Quaterniond spaceRotate, float partialTick) {
        if (snapshot == null) return;

        List<KspBodyState> stars = new ArrayList<>();
        List<KspBodyState> planets = new ArrayList<>();

        for (KspBodyState body : snapshot.bodies().values()) {
            KspBodyKind kind = body.definition().bodyKind();
            if (kind == KspBodyKind.STAR) {
                stars.add(body);
            } else {
                double renderZoom = getRenderZoom(body);
                SpaceVector absPos = body.absolutePosition();
                Vector3d relPos = new Vector3d(
                        absPos.x() - cameraPos.x,
                        absPos.y() - cameraPos.y,
                        absPos.z() - cameraPos.z
                ).mul(renderZoom);
                double renderSize = getRenderSize(relPos.length(), body.definition().radius() * renderZoom * 2);
                if (renderSize < 0.0075) continue;
                planets.add(body);
            }
        }

        if (stars.size() > 16) stars = stars.subList(0, 16);
        if (planets.size() > 64) planets = planets.subList(0, 64);

        ByteBuffer buffer = MemoryUtil.memCalloc(getSize());

        // Star data
        buffer.putInt(stars.size());
        buffer.putInt(0).putInt(0).putInt(0);
        for (KspBodyState star : stars) {
            double renderZoom = getRenderZoom(star);
            SpaceVector absPos = star.absolutePosition();
            Vector3d pos = new Vector3d(
                    absPos.x() - cameraPos.x,
                    absPos.y() - cameraPos.y,
                    absPos.z() - cameraPos.z
            );
            Vector3d realPos = new Vector3d(pos);
            pos.rotate(spaceRotate).mul(renderZoom);
            realPos.rotate(spaceRotate);

            float[] rgb = starTemperatureToRGB(5778.0);

            buffer.putFloat((float) pos.x);
            buffer.putFloat((float) pos.y);
            buffer.putFloat((float) pos.z);
            buffer.putFloat(0f);

            buffer.putFloat((float) realPos.x);
            buffer.putFloat((float) realPos.y);
            buffer.putFloat((float) realPos.z);
            buffer.putFloat(0f);

            buffer.putFloat(rgb[0]);
            buffer.putFloat(rgb[1]);
            buffer.putFloat(rgb[2]);
            buffer.putFloat(1f);

            buffer.putFloat((float) (star.definition().radius() * renderZoom));
            buffer.putFloat(0f);
            buffer.putFloat(0f);
            buffer.putFloat(0f);
        }
        for (int i = 0; i < 16 - stars.size(); i++) {
            for (int j = 0; j < 16; j++) buffer.putFloat(0f);
        }

        // Planet data
        buffer.putInt(planets.size());
        buffer.putInt(0).putInt(0).putInt(0);
        for (KspBodyState planet : planets) {
            double renderZoom = getRenderZoom(planet);
            SpaceVector absPos = planet.absolutePosition();
            Vector3d pos = new Vector3d(
                    absPos.x() - cameraPos.x,
                    absPos.y() - cameraPos.y,
                    absPos.z() - cameraPos.z
            );
            Vector3d realPos = new Vector3d(pos);
            pos.rotate(spaceRotate).mul(renderZoom);
            realPos.rotate(spaceRotate);

            double radius = planet.definition().radius();
            double g = planet.definition().gravitationalParameter() / (radius * radius);
            double atmosphericHeight = radius * 0.01;

            buffer.putFloat((float) pos.x);
            buffer.putFloat((float) pos.y);
            buffer.putFloat((float) pos.z);
            buffer.putFloat((float) g);

            buffer.putFloat((float) realPos.x);
            buffer.putFloat((float) realPos.y);
            buffer.putFloat((float) realPos.z);
            buffer.putFloat((float) (radius * renderZoom));

            buffer.putFloat((float) (atmosphericHeight * renderZoom));
            buffer.putFloat((float) atmosphericHeight);
            buffer.putFloat(288.0f);
            buffer.putFloat(0.029f);
            buffer.putFloat(1.225f);
            buffer.putFloat(0);
            buffer.putFloat(0);
            buffer.putFloat(0);

            buffer.putFloat(0.4f);
            buffer.putFloat(0.6f);
            buffer.putFloat(1.0f);
            buffer.putFloat(1.0f);
        }
        for (int i = 0; i < 64 - planets.size(); i++) {
            for (int j = 0; j < 24; j++) buffer.putFloat(0f);
        }

        // BlackHole data (empty for now)
        buffer.putInt(0);
        buffer.putInt(0).putInt(0).putInt(0);
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 12; j++) buffer.putFloat(0f);
        }

        buffer.flip();
        update(buffer, false);
    }

    public void updateFromPacket(SpaceSnapshotPacket packet, Vector3d cameraPos, Quaterniond spaceRotate) {
        if (packet == null) return;

        List<SpaceSnapshotPacket.BodyData> stars = new ArrayList<>();
        List<SpaceSnapshotPacket.BodyData> planets = new ArrayList<>();

        for (SpaceSnapshotPacket.BodyData body : packet.bodies()) {
            if (body.kind() == KspBodyKind.STAR) {
                stars.add(body);
            } else {
                Vector3d relPos = new Vector3d(body.posX() - cameraPos.x, body.posY() - cameraPos.y, body.posZ() - cameraPos.z);
                double renderSize = getRenderSize(relPos.length(), body.radius() * 2);
                if (renderSize < 0.0075) continue;
                planets.add(body);
            }
        }

        if (stars.size() > 16) stars = stars.subList(0, 16);
        if (planets.size() > 64) planets = planets.subList(0, 64);

        ByteBuffer buffer = MemoryUtil.memCalloc(getSize());

        buffer.putInt(stars.size());
        buffer.putInt(0).putInt(0).putInt(0);
        for (SpaceSnapshotPacket.BodyData star : stars) {
            Vector3d pos = new Vector3d(star.posX() - cameraPos.x, star.posY() - cameraPos.y, star.posZ() - cameraPos.z);
            Vector3d realPos = new Vector3d(pos);
            pos.rotate(spaceRotate);
            realPos.rotate(spaceRotate);
            float[] rgb = starTemperatureToRGB(5778.0);

            buffer.putFloat((float) pos.x).putFloat((float) pos.y).putFloat((float) pos.z).putFloat(0f);
            buffer.putFloat((float) realPos.x).putFloat((float) realPos.y).putFloat((float) realPos.z).putFloat(0f);
            buffer.putFloat(rgb[0]).putFloat(rgb[1]).putFloat(rgb[2]).putFloat(1f);
            buffer.putFloat((float) star.radius()).putFloat(0f).putFloat(0f).putFloat(0f);
        }
        for (int i = 0; i < 16 - stars.size(); i++) for (int j = 0; j < 16; j++) buffer.putFloat(0f);

        buffer.putInt(planets.size());
        buffer.putInt(0).putInt(0).putInt(0);
        for (SpaceSnapshotPacket.BodyData planet : planets) {
            Vector3d pos = new Vector3d(planet.posX() - cameraPos.x, planet.posY() - cameraPos.y, planet.posZ() - cameraPos.z);
            Vector3d realPos = new Vector3d(pos);
            pos.rotate(spaceRotate);
            realPos.rotate(spaceRotate);
            double radius = planet.radius();
            double atmosphericHeight = radius * 0.01;

            buffer.putFloat((float) pos.x).putFloat((float) pos.y).putFloat((float) pos.z).putFloat((float) (9.8));
            buffer.putFloat((float) realPos.x).putFloat((float) realPos.y).putFloat((float) realPos.z).putFloat((float) radius);
            buffer.putFloat((float) atmosphericHeight).putFloat((float) atmosphericHeight).putFloat(288.0f).putFloat(0.029f);
            buffer.putFloat(1.225f).putFloat(0).putFloat(0).putFloat(0);
            buffer.putFloat(0.4f).putFloat(0.6f).putFloat(1.0f).putFloat(1.0f);
        }
        for (int i = 0; i < 64 - planets.size(); i++) for (int j = 0; j < 24; j++) buffer.putFloat(0f);

        buffer.putInt(0);
        buffer.putInt(0).putInt(0).putInt(0);
        for (int i = 0; i < 16; i++) for (int j = 0; j < 12; j++) buffer.putFloat(0f);

        buffer.flip();
        update(buffer, false);
    }

    private static double getRenderZoom(KspBodyState body) {
        return 1.0;
    }

    private static double getRenderSize(double renderDistance, double renderDiameter) {
        double viewingAngle = Math.atan2(renderDiameter, renderDistance);
        double fov = Math.toRadians(70.0);
        return viewingAngle / fov;
    }

    private static float[] starTemperatureToRGB(double temperature) {
        double t = temperature / 100.0;
        float r, g, b;
        if (t <= 66) {
            r = 1.0f;
            g = (float) Math.clamp((0.39008157876 * Math.log(t) - 0.63184144378) , 0.0, 1.0);
        } else {
            r = (float) Math.clamp(1.29293618606 * Math.pow(t - 60, -0.1332047592), 0.0, 1.0);
            g = (float) Math.clamp(1.12989086089 * Math.pow(t - 60, -0.0755148492), 0.0, 1.0);
        }
        if (t >= 66) {
            b = 1.0f;
        } else if (t <= 19) {
            b = 0.0f;
        } else {
            b = (float) Math.clamp(0.54320678911 * Math.log(t - 10) - 1.19625408914, 0.0, 1.0);
        }
        return new float[]{r, g, b};
    }
}
