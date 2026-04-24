package org.polaris2023.gtu.space.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.polaris2023.gtu.space.client.SpaceShaderManager;
import org.polaris2023.gtu.space.simulation.ksp.KspBodyState;
import org.polaris2023.gtu.space.simulation.math.SpaceVector;

public class StarRenderer {

    public void render(KspBodyState star, Matrix4f modelView, Matrix4f projection,
                       float partialTick, Vector3d cameraPos, Quaterniond spaceRotate) {
        if (SpaceShaderManager.StarRender == null) return;
        ShaderInstance shader = SpaceShaderManager.StarRender.get();
        if (shader == null) return;

        SpaceVector absPos = star.absolutePosition();
        Vector3d relPos = new Vector3d(
                absPos.x() - cameraPos.x,
                absPos.y() - cameraPos.y,
                absPos.z() - cameraPos.z
        ).rotate(spaceRotate);

        double radius = star.definition().radius();
        double distance = relPos.length();
        int lod = SphereMeshBuilder.selectLOD(distance, radius * 2);

        float[] rgb = temperatureToRGB(5778.0);

        shader.safeGetUniform("StarRenderPos").set((float) relPos.x, (float) relPos.y, (float) relPos.z);
        shader.safeGetUniform("StarRenderRadiusZoom").set((float) radius);
        shader.safeGetUniform("StarColor").set(rgb[0], rgb[1], rgb[2], 1.0f);

        Quaternionf rotation = new Quaternionf();
        double phase = star.rotationPhaseRadians();
        SpaceVector axis = star.definition().rotationAxis();
        rotation.rotateAxis((float) phase, (float) axis.x(), (float) axis.y(), (float) axis.z());
        shader.safeGetUniform("StarRotate").set(new Matrix4f().rotate(rotation));

        SphereMeshBuilder.drawSphere(lod, modelView, projection, shader);
    }

    public static float[] temperatureToRGB(double temperature) {
        double t = temperature / 100.0;
        float r, g, b;
        if (t <= 66) {
            r = 1.0f;
            g = (float) Math.clamp(0.39008157876 * Math.log(t) - 0.63184144378, 0.0, 1.0);
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
