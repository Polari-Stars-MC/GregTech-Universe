package org.polaris2023.gtu.modpacks.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.polaris2023.gtu.modpacks.block.StressOutputHatchBlock;
import org.polaris2023.gtu.modpacks.dam.DamTier;

import java.util.List;
import java.util.Objects;

/**
 * 应力输出仓方块实体。
 * <p>
 * 继承 Create 的 {@link GeneratingKineticBlockEntity}，
 * 作为应力源向 Create 动力学网络输出旋转力。
 * </p>
 */
public class StressOutputHatchBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private float generatedSpeed = 0;
    private boolean linked = false; // 是否连接到水坝控制器
    private double currentStressOutput = 0;
    private BlockPos controllerPos;
    private int segmentIndex = -1;
    private int totalSegments = 0;
    private boolean structureValid = false;

    public StressOutputHatchBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getGeneratedSpeed() {
        return linked ? generatedSpeed : 0;
    }

    /**
     * 由水坝控制器调用，设置输出参数。
     */
    public void setOutputParameters(float speed, double stressOutput, BlockPos controllerPos, int segmentIndex, int totalSegments, boolean structureValid) {
        boolean speedChanged = Math.abs(this.generatedSpeed - speed) > 0.001F;
        boolean stressChanged = Math.abs(this.currentStressOutput - stressOutput) > 0.001D;
        boolean linkChanged = !this.linked;
        boolean controllerChanged = !Objects.equals(this.controllerPos, controllerPos);
        boolean segmentChanged = this.segmentIndex != segmentIndex || this.totalSegments != totalSegments;
        boolean structureChanged = this.structureValid != structureValid;
        if (!speedChanged && !stressChanged && !linkChanged && !controllerChanged && !segmentChanged && !structureChanged) {
            return;
        }

        this.generatedSpeed = speed;
        this.currentStressOutput = stressOutput;
        this.linked = true;
        this.controllerPos = controllerPos;
        this.segmentIndex = segmentIndex;
        this.totalSegments = totalSegments;
        this.structureValid = structureValid;
        updateGeneratedRotation();

        if (level != null && !level.isClientSide && linked && Math.abs(generatedSpeed) > 0.001F) {
            if (!hasNetwork()) {
                applyNewSpeed(0.0F, generatedSpeed);
            }
            if (hasNetwork()) {
                notifyStressCapacityChange(calculateAddedStressCapacity());
                getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
                getOrCreateNetwork().updateStress();
            }
        }

        setChanged();
    }

    /**
     * 取消链接。
     */
    public void unlink() {
        if (!linked && Math.abs(generatedSpeed) < 0.001F && currentStressOutput == 0
                && controllerPos == null && segmentIndex == -1 && totalSegments == 0 && !structureValid) {
            return;
        }

        this.linked = false;
        this.generatedSpeed = 0;
        this.currentStressOutput = 0;
        this.controllerPos = null;
        this.segmentIndex = -1;
        this.totalSegments = 0;
        this.structureValid = false;
        updateGeneratedRotation();
        setChanged();
    }

    public boolean isLinked() {
        return linked;
    }

    public DamTier getTier() {
        if (getBlockState().getBlock() instanceof StressOutputHatchBlock hatchBlock) {
            return hatchBlock.getTier();
        }
        return DamTier.PRIMITIVE;
    }

    // ---- Create 应力容量 ----

    @Override
    public float calculateAddedStressCapacity() {
        if (!linked || Math.abs(generatedSpeed) < 0.001F) {
            return 0;
        }
        return (float) (currentStressOutput / Math.abs(generatedSpeed));
    }

    // ---- 护目镜信息 ----

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        DamTier tier = getTier();

        tooltip.add(Component.literal("应力输出仓").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  等级: ").withStyle(ChatFormatting.GRAY)
                .append(tier.getDisplayComponent()));
        tooltip.add(Component.literal(String.format("  状态: %s", linked ? "已连接" : "未连接"))
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.RED));

        if (linked) {
            tooltip.add(Component.literal(String.format("  应力输出: %.0f SU", currentStressOutput))
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal(String.format("  转速: %.1f RPM", generatedSpeed))
                    .withStyle(ChatFormatting.GREEN));
        }

        if (linked) {
            tooltip.add(Component.literal(String.format("  鍧濇: %d / %d", segmentIndex + 1, totalSegments))
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal(String.format("  缁撴瀯鏍￠獙: %s", structureValid ? "閫氳繃" : "澶辫触"))
                    .withStyle(structureValid ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        return true;
    }

    // ---- NBT (使用 SmartBlockEntity 的 write/read) ----

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("GeneratedSpeed", generatedSpeed);
        tag.putBoolean("Linked", linked);
        tag.putDouble("CurrentStress", currentStressOutput);
        tag.putInt("SegmentIndex", segmentIndex);
        tag.putInt("TotalSegments", totalSegments);
        tag.putBoolean("StructureValid", structureValid);
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        generatedSpeed = tag.getFloat("GeneratedSpeed");
        linked = tag.getBoolean("Linked");
        currentStressOutput = tag.getDouble("CurrentStress");
        segmentIndex = tag.getInt("SegmentIndex");
        totalSegments = tag.getInt("TotalSegments");
        structureValid = tag.getBoolean("StructureValid");
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public int getTotalSegments() {
        return totalSegments;
    }

    public double getCurrentStressOutput() {
        return currentStressOutput;
    }

    public boolean isStructureValid() {
        return structureValid;
    }
}
