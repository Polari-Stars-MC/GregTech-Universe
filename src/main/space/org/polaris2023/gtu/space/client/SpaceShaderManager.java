package org.polaris2023.gtu.space.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.polaris2023.gtu.space.GregtechUniverseSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Supplier;

@EventBusSubscriber(modid = GregtechUniverseSpace.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SpaceShaderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceShaderManager.class);

    private static ShaderInstance cubePlanetShader;
    private static ShaderInstance planetAtmosphereShader;
    private static ShaderInstance portalRenderShader;

    public static Supplier<ShaderInstance> SkyBoxRender;
    public static Supplier<ShaderInstance> StarRender;
    public static Supplier<ShaderInstance> PlanetRender;
    public static Supplier<ShaderInstance> PlanetCloudRender;
    public static Supplier<ShaderInstance> PlanetRingRender;
    public static Supplier<ShaderInstance> BlackHoleRender;

    public static PostChain BufferSwitch;
    public static PostChain StarBloom;
    public static PostChain PlanetAtmospherePost;
    public static PostChain BlackHoleGravitationalLens;

    private SpaceShaderManager() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "cube_planet"),
                            DefaultVertexFormat.POSITION_COLOR),
                    shader -> cubePlanetShader = shader
            );
        } catch (IOException e) {
            LOGGER.warn("Failed to load cube_planet shader", e);
        }

        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "planet_atmosphere"),
                            DefaultVertexFormat.POSITION_COLOR),
                    shader -> planetAtmosphereShader = shader
            );
        } catch (IOException e) {
            LOGGER.warn("Failed to load planet_atmosphere shader", e);
        }

        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "portal_render"),
                            DefaultVertexFormat.POSITION_COLOR),
                    shader -> portalRenderShader = shader
            );
        } catch (IOException e) {
            LOGGER.warn("Failed to load portal_render shader", e);
        }

        registerShader(event, "sky_box/sky_box_render", DefaultVertexFormat.POSITION_TEX,
                shader -> SkyBoxRender = () -> shader);
        registerShader(event, "star/star_render", DefaultVertexFormat.POSITION,
                shader -> StarRender = () -> shader);
        registerShader(event, "planet/planet_render", DefaultVertexFormat.NEW_ENTITY,
                shader -> PlanetRender = () -> shader);
        registerShader(event, "planet/planet_cloud_render", DefaultVertexFormat.NEW_ENTITY,
                shader -> PlanetCloudRender = () -> shader);
        registerShader(event, "planet/planet_ring_render", DefaultVertexFormat.NEW_ENTITY,
                shader -> PlanetRingRender = () -> shader);
        registerShader(event, "black_hole/black_hole_render", DefaultVertexFormat.POSITION,
                shader -> BlackHoleRender = () -> shader);
    }

    private static void registerShader(RegisterShadersEvent event, String name,
                                        com.mojang.blaze3d.vertex.VertexFormat format,
                                        java.util.function.Consumer<ShaderInstance> callback) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, name),
                            format),
                    callback
            );
        } catch (IOException e) {
            LOGGER.warn("Failed to load shader: {}", name, e);
        }
    }

    private static boolean postChainsInitAttempted = false;

    public static void initPostChains() {
        if (postChainsInitAttempted) return;
        postChainsInitAttempted = true;

        Minecraft mc = Minecraft.getInstance();
        try {
            BufferSwitch = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "shaders/post/buffer_switch.json"));
            BufferSwitch.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            LOGGER.warn("Failed to load BufferSwitch post chain", e);
            BufferSwitch = null;
        }
        try {
            StarBloom = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "shaders/post/star/star_bloom.json"));
            StarBloom.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            LOGGER.warn("Failed to load StarBloom post chain", e);
            StarBloom = null;
        }
        try {
            PlanetAtmospherePost = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "shaders/post/planet/planet_atmosphere.json"));
            PlanetAtmospherePost.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            LOGGER.warn("Failed to load PlanetAtmosphere post chain", e);
            PlanetAtmospherePost = null;
        }
        try {
            BlackHoleGravitationalLens = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(GregtechUniverseSpace.MOD_ID, "shaders/post/black_hole/black_hole_gravitational_lens.json"));
            BlackHoleGravitationalLens.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            LOGGER.warn("Failed to load BlackHoleGravitationalLens post chain", e);
            BlackHoleGravitationalLens = null;
        }
    }

    public static void resizePostChains(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (BufferSwitch != null) BufferSwitch.resize(width, height);
        if (StarBloom != null) StarBloom.resize(width, height);
        if (PlanetAtmospherePost != null) PlanetAtmospherePost.resize(width, height);
        if (BlackHoleGravitationalLens != null) BlackHoleGravitationalLens.resize(width, height);
    }

    public static Supplier<ShaderInstance> getCubePlanetShader() {
        return () -> cubePlanetShader;
    }

    public static Supplier<ShaderInstance> getPlanetAtmosphereShader() {
        return () -> planetAtmosphereShader;
    }

    public static Supplier<ShaderInstance> getPortalRenderShader() {
        return () -> portalRenderShader;
    }

    public static boolean isCubePlanetShaderLoaded() {
        return cubePlanetShader != null;
    }

    public static boolean isPortalRenderShaderLoaded() {
        return portalRenderShader != null;
    }
}
