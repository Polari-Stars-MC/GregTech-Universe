package org.polaris2023.gtu.space.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.polaris2023.gtu.space.client.PlanetRenderInfo;
import org.polaris2023.gtu.space.client.SpaceShaderManager;

import java.util.Map;

public final class PlanetShaderRenderer {
    private static final Map<String, TextureSet> TEXTURES = Map.of(
            "mercury", TextureSet.surface("mercury"),
            "venus", TextureSet.surface("venus"),
            "earth", TextureSet.earth(),
            "moon", TextureSet.surface("moon"),
            "mars", TextureSet.surface("mars"),
            "jupiter", TextureSet.surface("jupiter"),
            "saturn", TextureSet.surface("saturn"),
            "uranus", TextureSet.surface("uranus"),
            "neptune", TextureSet.surface("neptune")
    );

    public boolean canRender(PlanetRenderInfo planet) {
        return TEXTURES.containsKey(planet.id());
    }

    public void render(PlanetRenderInfo planet, Matrix4f modelView, Matrix4f projection) {
        TextureSet textures = TEXTURES.get(planet.id());
        if (textures == null) {
            return;
        }
        renderSurface(planet, textures, modelView, projection);
        if (textures.cloud() != null) {
            renderClouds(planet, textures, modelView, projection);
        }
    }

    private void renderSurface(PlanetRenderInfo planet, TextureSet textures, Matrix4f modelView, Matrix4f projection) {
        if (SpaceShaderManager.PlanetRender == null) {
            return;
        }
        ShaderInstance shader = SpaceShaderManager.PlanetRender.get();
        if (shader == null) {
            return;
        }

        Vector3d renderPos = planet.worldPosition();
        float radius = (float) planet.radius();
        int lod = SphereMeshBuilder.selectLOD(renderPos.length(), radius * 2.0);

        shader.safeGetUniform("PlanetRenderPos").set((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        shader.safeGetUniform("PlanetRealPos").set((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        shader.safeGetUniform("PlanetRenderRadiusZoom").set(radius);
        shader.safeGetUniform("PlanetR").set(radius);
        shader.safeGetUniform("tModelViewMat").set(new Matrix4f(modelView));
        shader.safeGetUniform("tProjMat").set(new Matrix4f(projection));

        Matrix3f rotation = rotationMatrix(planet);
        shader.safeGetUniform("PlanetRotate").set(rotation);
        shader.safeGetUniform("iPlanetRotate").set(new Matrix3f(rotation).invert());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, planet.alpha());
        RenderSystem.setShaderTexture(0, textures.surface());
        RenderSystem.setShaderTexture(1, textures.nightOrSurface());
        SphereMeshBuilder.drawSphere(lod, modelView, projection, shader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderClouds(PlanetRenderInfo planet, TextureSet textures, Matrix4f modelView, Matrix4f projection) {
        if (SpaceShaderManager.PlanetCloudRender == null) {
            return;
        }
        ShaderInstance shader = SpaceShaderManager.PlanetCloudRender.get();
        if (shader == null) {
            return;
        }

        Vector3d renderPos = planet.worldPosition();
        float cloudRadius = (float) (planet.radius() * 1.022);
        int lod = SphereMeshBuilder.selectLOD(renderPos.length(), cloudRadius * 2.0);

        shader.safeGetUniform("PlanetRenderPos").set((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        shader.safeGetUniform("PlanetRealPos").set((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        shader.safeGetUniform("PlanetRenderRadiusZoom").set(cloudRadius);
        shader.safeGetUniform("tModelViewMat").set(new Matrix4f(modelView));
        shader.safeGetUniform("tProjMat").set(new Matrix4f(projection));
        shader.safeGetUniform("PlanetRotate").set(new Matrix4f().set(rotationMatrix(planet)));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(planet.alpha() * 0.92F, 0.92F));
        RenderSystem.setShaderTexture(1, textures.cloud());
        SphereMeshBuilder.drawSphere(lod, modelView, projection, shader);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static Matrix3f rotationMatrix(PlanetRenderInfo planet) {
        Quaternionf rotation = new Quaternionf();
        Vector3d axis = planet.rotationAxis();
        rotation.rotateAxis((float) planet.rotationPhaseRadians(), (float) axis.x, (float) axis.y, (float) axis.z);
        return new Matrix3f().rotate(rotation);
    }

    private record TextureSet(ResourceLocation surface, ResourceLocation night, ResourceLocation cloud) {
        private static TextureSet surface(String planetId) {
            String base = "textures/celestial_body/planet/" + planetId + "/";
            return new TextureSet(
                    GregtechUniverseSpace.id(base + "surface.png"),
                    null,
                    null
            );
        }

        private static TextureSet earth() {
            String base = "textures/celestial_body/planet/earth/";
            return new TextureSet(
                    GregtechUniverseSpace.id(base + "surface.png"),
                    GregtechUniverseSpace.id(base + "surface_night.png"),
                    GregtechUniverseSpace.id(base + "cloud.png")
            );
        }

        private ResourceLocation nightOrSurface() {
            return night != null ? night : surface;
        }
    }
}
