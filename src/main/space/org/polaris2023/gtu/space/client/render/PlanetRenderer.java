package org.polaris2023.gtu.space.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;
import org.polaris2023.gtu.space.client.SpaceShaderManager;
import org.polaris2023.gtu.space.runtime.ksp.KspBodyState;
import org.polaris2023.gtu.space.runtime.math.SpaceVector;

public class PlanetRenderer {

    public void render(KspBodyState planet, Matrix4f modelView, Matrix4f projection,
                       float partialTick, Vector3d cameraPos, Quaterniond spaceRotate) {
        renderSurface(planet, modelView, projection, partialTick, cameraPos, spaceRotate);
    }

    private void renderSurface(KspBodyState planet, Matrix4f modelView, Matrix4f projection,
                                float partialTick, Vector3d cameraPos, Quaterniond spaceRotate) {
        if (SpaceShaderManager.PlanetRender == null) return;
        ShaderInstance shader = SpaceShaderManager.PlanetRender.get();
        if (shader == null) return;

        SpaceVector absPos = planet.absolutePosition();
        Vector3d relPos = new Vector3d(
                absPos.x() - cameraPos.x,
                absPos.y() - cameraPos.y,
                absPos.z() - cameraPos.z
        );
        Vector3d renderPos = new Vector3d(relPos).rotate(spaceRotate);
        Vector3d realPos = new Vector3d(relPos).rotate(spaceRotate);

        double radius = planet.definition().radius();
        double distance = renderPos.length();
        int lod = SphereMeshBuilder.selectLOD(distance, radius * 2);

        shader.safeGetUniform("PlanetRenderPos").set(
                (float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        shader.safeGetUniform("PlanetRealPos").set(
                (float) realPos.x, (float) realPos.y, (float) realPos.z);
        shader.safeGetUniform("PlanetRenderRadiusZoom").set((float) radius);
        shader.safeGetUniform("PlanetR").set((float) radius);

        Matrix4f tModelView = new Matrix4f(modelView);
        Matrix4f tProj = new Matrix4f(projection);
        shader.safeGetUniform("tModelViewMat").set(tModelView);
        shader.safeGetUniform("tProjMat").set(tProj);

        Quaternionf rotation = new Quaternionf();
        double phase = planet.rotationPhaseRadians();
        SpaceVector axis = planet.definition().rotationAxis();
        rotation.rotateAxis((float) phase, (float) axis.x(), (float) axis.y(), (float) axis.z());
        Matrix3f rotMat = new Matrix3f().rotate(rotation);
        Matrix3f iRotMat = new Matrix3f(rotMat).invert();

        shader.safeGetUniform("PlanetRotate").set(rotMat);
        shader.safeGetUniform("iPlanetRotate").set(iRotMat);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        SphereMeshBuilder.drawSphere(lod, modelView, projection, shader);
    }

    public void renderCloud(KspBodyState planet, Matrix4f modelView, Matrix4f projection,
                             float partialTick, Vector3d cameraPos, Quaterniond spaceRotate) {
        if (SpaceShaderManager.PlanetCloudRender == null) return;
        ShaderInstance shader = SpaceShaderManager.PlanetCloudRender.get();
        if (shader == null) return;

        SpaceVector absPos = planet.absolutePosition();
        Vector3d relPos = new Vector3d(
                absPos.x() - cameraPos.x,
                absPos.y() - cameraPos.y,
                absPos.z() - cameraPos.z
        );
        Vector3d renderPos = new Vector3d(relPos).rotate(spaceRotate);
        Vector3d realPos = new Vector3d(relPos).rotate(spaceRotate);

        double radius = planet.definition().radius();
        double cloudRadius = radius * 1.005;
        double distance = renderPos.length();
        int lod = SphereMeshBuilder.selectLOD(distance, cloudRadius * 2);

        shader.safeGetUniform("PlanetRenderPos").set(
                (float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        shader.safeGetUniform("PlanetRealPos").set(
                (float) realPos.x, (float) realPos.y, (float) realPos.z);
        shader.safeGetUniform("PlanetRenderRadiusZoom").set((float) cloudRadius);

        Matrix4f tModelView = new Matrix4f(modelView);
        Matrix4f tProj = new Matrix4f(projection);
        shader.safeGetUniform("tModelViewMat").set(tModelView);
        shader.safeGetUniform("tProjMat").set(tProj);

        Quaternionf rotation = new Quaternionf();
        double phase = planet.rotationPhaseRadians() * 0.9;
        SpaceVector axis = planet.definition().rotationAxis();
        rotation.rotateAxis((float) phase, (float) axis.x(), (float) axis.y(), (float) axis.z());
        shader.safeGetUniform("PlanetRotate").set(new Matrix4f().rotate(rotation));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);

        SphereMeshBuilder.drawSphere(lod, modelView, projection, shader);

        RenderSystem.defaultBlendFunc();
    }
}
