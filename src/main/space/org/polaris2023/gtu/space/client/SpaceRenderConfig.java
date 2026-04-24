package org.polaris2023.gtu.space.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import org.polaris2023.gtu.space.GregtechUniverseSpace;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record SpaceRenderConfig(
        Planet planet,
        Transition transition,
        Space space,
        Effects effects
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SpaceRenderConfig DEFAULT = new SpaceRenderConfig(
            new Planet(5000.0, new float[]{0.3F, 0.5F, 0.9F}, 0.6F, new double[]{2000.0, 5000.0, 10000.0, 30000.0}),
            new Transition(12_000.0, 64_000.0, 160_000.0),
            new Space(1.0F, 0.5F, 3000.0),
            new Effects(true, true, true)
    );

    public static SpaceRenderConfig defaults() {
        return DEFAULT;
    }

    public static SpaceRenderConfig load() {
        Path path = resolvePath();
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(DEFAULT, writer);
                }
                return DEFAULT;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                SpaceRenderConfig loaded = GSON.fromJson(reader, SpaceRenderConfig.class);
                return loaded == null ? DEFAULT : loaded.withSanitizedValues();
            }
        } catch (IOException | JsonIOException | JsonSyntaxException exception) {
            return DEFAULT;
        }
    }

    private static Path resolvePath() {
        Minecraft minecraft = Minecraft.getInstance();
        Path gameDir = minecraft == null ? Path.of(".") : minecraft.gameDirectory.toPath();
        return gameDir.resolve("config").resolve(GregtechUniverseSpace.MOD_ID).resolve("space_render.json");
    }

    public SpaceRenderConfig withSanitizedValues() {
        float[] atmosphereColor = planet.atmosphereColor == null || planet.atmosphereColor.length != 3
                ? DEFAULT.planet.atmosphereColor.clone()
                : new float[]{planet.atmosphereColor[0], planet.atmosphereColor[1], planet.atmosphereColor[2]};
        double[] lodDistances = planet.lodDistances == null || planet.lodDistances.length != 4
                ? DEFAULT.planet.lodDistances.clone()
                : new double[]{planet.lodDistances[0], planet.lodDistances[1], planet.lodDistances[2], planet.lodDistances[3]};
        return new SpaceRenderConfig(
                new Planet(Math.max(1.0, planet.cubeHalfSize), atmosphereColor, clamp01(planet.atmosphereIntensity), lodDistances),
                new Transition(
                        Math.max(DEFAULT.transition.atmosphereStart, transition.atmosphereStart),
                        Math.max(DEFAULT.transition.atmosphereEnd, transition.atmosphereEnd),
                        Math.max(DEFAULT.transition.orbitStart, transition.orbitStart)
                ),
                new Space(
                        Math.max(0.0F, space.starIntensity),
                        Math.max(0.0F, space.starTwinkleSpeed),
                        Math.max(0.0, space.maxVesselRenderDistance)
                ),
                effects
        );
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public record Planet(double cubeHalfSize, float[] atmosphereColor, float atmosphereIntensity, double[] lodDistances) {
    }

    public record Transition(double atmosphereStart, double atmosphereEnd, double orbitStart) {
    }

    public record Space(float starIntensity, float starTwinkleSpeed, double maxVesselRenderDistance) {
    }

    public record Effects(boolean enableAtmosphereScatter, boolean enableBloom, boolean enableAntialiasing) {
    }
}
