package org.polaris2023.gtu.modpacks.worldgen.feature;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GroundStickDisplayFeature extends Feature<NoneFeatureConfiguration> {
    public static final String GROUND_STICK_INTERACTION_TAG = "gtu_modpacks.ground_stick_interaction";
    public static final String GROUND_STICK_DISPLAY_TAG = "gtu_modpacks.ground_stick_display";
    public static final String PAIRED_DISPLAY_UUID = "gtu_modpacks_paired_display";
    public static final String PAIRED_INTERACTION_UUID = "gtu_modpacks_paired_interaction";

    public GroundStickDisplayFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        if (surfaceY <= level.getMinBuildHeight()) {
            return false;
        }

        BlockPos groundPos = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        BlockPos displayPos = groundPos.above();
        BlockState groundState = level.getBlockState(groundPos);
        if (!groundState.isFaceSturdy(level, groundPos, Direction.UP) || !level.getBlockState(displayPos).isAir()) {
            return false;
        }

        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level.getLevel());
        display.setPos(origin.getX() + 0.5D, surfaceY + 1.02D, origin.getZ() + 0.5D);
        display.getSlot(0).set(new ItemStack(Items.STICK));

        float yaw = random.nextFloat() * 360.0F;
        float roll = -90.0F + (random.nextFloat() - 0.5F) * 16.0F;
        Quaternionf rotation = Axis.YP.rotationDegrees(yaw).mul(Axis.ZP.rotationDegrees(roll), new Quaternionf());
        Transformation transformation = new Transformation(
                new Vector3f(0.0F, 0.01F, 0.0F),
                rotation,
                new Vector3f(0.65F, 0.65F, 0.65F),
                new Quaternionf()
        );

        CompoundTag tag = display.saveWithoutId(new CompoundTag());
        ItemDisplayContext.CODEC.encodeStart(NbtOps.INSTANCE, ItemDisplayContext.GROUND)
                .result()
                .ifPresent(nbt -> tag.put("item_display", nbt));
        Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation)
                .result()
                .ifPresent(nbt -> tag.put("transformation", nbt));
        tag.putString("billboard", Display.BillboardConstraints.FIXED.getSerializedName());
        display.load(tag);
        display.addTag(GROUND_STICK_DISPLAY_TAG);

        Interaction interaction = new Interaction(EntityType.INTERACTION, level.getLevel());
        interaction.setPos(origin.getX() + 0.5D, surfaceY + 1.1D, origin.getZ() + 0.5D);

        CompoundTag interactionTag = interaction.saveWithoutId(new CompoundTag());
        interactionTag.putFloat("width", 0.6F);
        interactionTag.putFloat("height", 0.2F);
        interactionTag.putBoolean("response", true);
        interaction.load(interactionTag);
        interaction.addTag(GROUND_STICK_INTERACTION_TAG);
        interaction.getPersistentData().putUUID(PAIRED_DISPLAY_UUID, display.getUUID());
        display.getPersistentData().putUUID(PAIRED_INTERACTION_UUID, interaction.getUUID());

        boolean interactionAdded = level.addFreshEntity(interaction);
        boolean displayAdded = level.addFreshEntity(display);
        return interactionAdded && displayAdded;
    }
}
