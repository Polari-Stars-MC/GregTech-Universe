package org.polaris2023.gtu.modpacks.mixin.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class GtuModpacksMixinPlugin implements IMixinConfigPlugin {
    private static final String NOISE_CHUNK = "net.minecraft.world.level.levelgen.NoiseChunk";
    private static final String NOISE_CHUNK_BLOCK_STATE_FILLER_DESC =
            "Lnet/minecraft/world/level/levelgen/NoiseChunk$BlockStateFiller;";
    private static final String AQUIFER_DESC = "Lnet/minecraft/world/level/levelgen/Aquifer;";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.endsWith("worldgen.MixinNoiseChunk") || NOISE_CHUNK.equals(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!mixinClassName.endsWith("worldgen.MixinNoiseChunk")) {
            return;
        }

        if (!hasField(targetClass, AQUIFER_DESC) || !hasField(targetClass, NOISE_CHUNK_BLOCK_STATE_FILLER_DESC)) {
            throw new IllegalStateException("GTU modpacks river hook could not find NoiseChunk terrain fields");
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean hasField(ClassNode targetClass, String descriptor) {
        for (FieldNode field : targetClass.fields) {
            if (descriptor.equals(field.desc)) {
                return true;
            }
        }
        return false;
    }
}
