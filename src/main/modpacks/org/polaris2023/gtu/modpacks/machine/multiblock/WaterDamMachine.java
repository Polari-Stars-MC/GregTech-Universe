package org.polaris2023.gtu.modpacks.machine.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.IControlContraption;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.modpacks.block.DamShaftBlock;
import org.polaris2023.gtu.modpacks.block.StressOutputHatchBlock;
import org.polaris2023.gtu.modpacks.blockentity.StressOutputHatchBlockEntity;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamMachineBlockEntity;
import org.polaris2023.gtu.modpacks.contraption.DamWheelContraption;
import org.polaris2023.gtu.modpacks.dam.DamBlueprint;
import org.polaris2023.gtu.modpacks.dam.DamMultiblockPatterns;
import org.polaris2023.gtu.modpacks.dam.DamSegmentState;
import org.polaris2023.gtu.modpacks.dam.DamStructureBlocks;
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverCurrentSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WaterDamMachine extends MultiblockControllerMachine implements IDisplayUIMachine, IInteractedMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger("GTU Water Dam");
    private static final int AXIS_SEARCH_HORIZONTAL_RADIUS = 16;
    private static final int AXIS_SEARCH_VERTICAL_DOWN = 10;
    private static final int AXIS_SEARCH_VERTICAL_UP = 4;
    private static final int LIGHT_REFRESH_INTERVAL = 20;
    private static final int FULL_RECOVERY_INTERVAL = 200;
    private static final double CONTRAPTION_SEARCH_RADIUS = 2.5;
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WaterDamMachine.class,
            MultiblockControllerMachine.MANAGED_FIELD_HOLDER
    );

    @Persisted
    @DescSynced
    private int tierIndex = DamTier.PRIMITIVE.getIndex();
    @Persisted
    @DescSynced
    private boolean tierLocked = false;
    @Persisted
    @DescSynced
    private double riverFlowSpeed = 0.0;
    @Persisted
    @DescSynced
    private double stressOutput = 0.0;
    @Persisted
    @DescSynced
    private float rotationAngle = 0.0F;
    @Persisted
    @DescSynced
    private float rotationSpeed = 0.0F;
    @DescSynced
    private int segmentCount = 0;
    @DescSynced
    private int connectedDamCount = 0;
    @DescSynced
    private int activeWheelCount = 0;
    @DescSynced
    private String lastIssue = "";

    private final List<DamSegmentState> segments = new ArrayList<>();
    private final List<String> structureIssues = new ArrayList<>();
    private boolean pendingRecovery = false;
    @Nullable
    private String lastContraptionFailureKey;
    private long lastContraptionFailureTick = Long.MIN_VALUE;
    @Nullable
    private TickableSubscription serverSubscription;

    public WaterDamMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote() && serverSubscription == null) {
            serverSubscription = subscribeServerTick(this::serverTickDam);
        }
        if (!isRemote() && isFormed()) {
            pendingRecovery = true;
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribe(serverSubscription);
        serverSubscription = null;
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (isFormed()) {
            tickClientContraptions();
        }
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        recoverFormedState();
    }

    @Override
    public void onStructureInvalid() {
        resetOperationalState(true);
        super.onStructureInvalid();
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isFormed() && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (getLevel() != null && getLevel().isClientSide()) {
                int duration = ConfigHolder.INSTANCE.client.inWorldPreviewDuration;
                float tickRate = getLevel().tickRateManager().tickrate();
                MultiblockInWorldPreviewRenderer.showPreview(getPos(), self(), Mth.floor(duration * tickRate));
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemInteractionResult onUseWithItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        net.minecraft.world.level.block.Block heldBlock = blockItem.getBlock();
        if (heldBlock == Blocks.AIR) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        DamTier newTier = DamTier.fromCasingBlock(heldBlock);
        if (newTier == DamTier.PRIMITIVE && heldBlock != DamTier.PRIMITIVE.getCasingBlock()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (newTier == getTier() && tierLocked) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        tierLocked = true;
        setTier(newTier);
        player.displayClientMessage(
                Component.literal("Water Dam Tier Locked: ").withStyle(ChatFormatting.GOLD)
                        .append(newTier.getDisplayComponent()),
                true
        );
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(true, activeWheelCount > 0 && rotationSpeed > 0.01F)
                .addWorkingStatusLine();

        if (!isFormed()) {
            if (!lastIssue.isEmpty()) {
                textList.add(Component.literal(lastIssue).withStyle(ChatFormatting.RED));
            }
            return;
        }

        textList.add(Component.translatable("gui.gtu_modpacks.water_dam.tier")
                .append(": ")
                .append(getTierDisplayComponent()));
        textList.add(Component.literal(String.format("Segments: %d", segmentCount)).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("gui.gtu_modpacks.water_dam.connected_dams")
                .append(": ")
                .append(Component.literal(Integer.toString(connectedDamCount + 1)).withStyle(ChatFormatting.YELLOW)));
        textList.add(Component.translatable("gui.gtu_modpacks.water_dam.flow_speed")
                .append(": ")
                .append(Component.literal(String.format("%.2f", riverFlowSpeed)).withStyle(ChatFormatting.AQUA)));
        textList.add(Component.translatable("gui.gtu_modpacks.water_dam.stress_output")
                .append(": ")
                .append(Component.literal(String.format("%.0f SU", stressOutput)).withStyle(ChatFormatting.GREEN)));
        textList.add(Component.translatable("gui.gtu_modpacks.water_dam.rotation_speed")
                .append(": ")
                .append(Component.literal(String.format("%.1f RPM", rotationSpeed)).withStyle(ChatFormatting.GOLD)));
        textList.add(Component.literal(String.format("Active Wheels: %d / %d", activeWheelCount, segmentCount))
                .withStyle(ChatFormatting.BLUE));
        if (!lastIssue.isEmpty()) {
            textList.add(Component.literal(lastIssue).withStyle(ChatFormatting.RED));
        }

        IDisplayUIMachine.super.addDisplayText(textList);
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var screen = new DraggableScrollableWidgetGroup(7, 4, 162, 121).setBackground(getScreenTexture());
        screen.addWidget(new LabelWidget(4, 5, self().getBlockState().getBlock().getDescriptionId()));
        screen.addWidget(new ComponentPanelWidget(4, 17, this::addDisplayText)
                .textSupplier(this.getLevel() != null && this.getLevel().isClientSide ? null : this::addDisplayText)
                .setMaxWidthLimit(150)
                .clickHandler(this::handleDisplayClick));
        return new ModularUI(176, 216, this, entityPlayer)
                .background(GuiTextures.BACKGROUND)
                .widget(screen)
                .widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(), GuiTextures.SLOT, 7, 134, true));
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isFormed()) {
            tooltip.add(Component.literal("Water Dam not formed").withStyle(ChatFormatting.RED));
            if (!lastIssue.isEmpty()) {
                tooltip.add(Component.literal(lastIssue).withStyle(ChatFormatting.DARK_RED));
            }
            return true;
        }

        tooltip.add(Component.literal("Water Dam Controller").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  ")
                .append(Component.translatable("gui.gtu_modpacks.water_dam.tier").withStyle(ChatFormatting.GRAY))
                .append(": ")
                .append(getTierDisplayComponent()));
        tooltip.add(Component.literal(String.format("  Stress Output: %.0f SU", stressOutput)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(String.format("  Rotation: %.1f RPM", rotationSpeed)).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(String.format("  Flow: %.2f", riverFlowSpeed)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal(String.format("  Connected Dams: %d", connectedDamCount + 1)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(String.format("  Active Wheels: %d / %d", activeWheelCount, segmentCount))
                .withStyle(ChatFormatting.AQUA));
        if (isPlayerSneaking) {
            BlockPos axis = getAxisPosition();
            tooltip.add(Component.literal(String.format("  Axis: [%d, %d, %d]", axis.getX(), axis.getY(), axis.getZ()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lastIssue.isEmpty()) {
            tooltip.add(Component.literal("  " + lastIssue).withStyle(ChatFormatting.RED));
        }
        return true;
    }

    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        UUID contraptionId = contraption.getUUID();
        for (DamSegmentState segment : segments) {
            if (contraptionId.equals(segment.contraptionId())) {
                return true;
            }
        }
        return false;
    }

    public void attach(ControlledContraptionEntity entity) {
        Direction facing = getDamFacing();
        DamSegmentState nearest = segments.stream()
                .min(Comparator.comparingDouble(segment -> segment.axisPos().distSqr(BlockPos.containing(entity.position()))))
                .orElse(null);
        BlockPos axisPos = nearest != null ? nearest.axisPos() : findNearestKnownAxisPos(BlockPos.containing(entity.position()), facing);
        if (axisPos == null) {
            return;
        }

        if (nearest != null) {
            nearest.setContraptionId(entity.getUUID());
            nearest.setAssembled(true);
        }

        entity.setPos(axisPos.getX(), axisPos.getY(), axisPos.getZ());
        entity.setRotationAxis(facing.getAxis());
        entity.setAngle(rotationAngle);
        if (nearest != null) {
            updateActiveWheelCount();
        } else if (activeWheelCount <= 0) {
            activeWheelCount = 1;
        }
    }

    public void onStall() {
        structureIssues.add("Dam wheel contraption stalled.");
        markLastIssue();
    }

    public boolean isControllerValid() {
        return isFormed() && !isInValid() && getLevel() != null && getLevel().getBlockEntity(getPos()) == holder.self();
    }

    public DamTier getTier() {
        return DamTier.byIndex(tierIndex);
    }

    public void setTier(DamTier tier) {
        this.tierIndex = tier.getIndex();
        if (!isRemote() && isFormed()) {
            recoverFormedState();
        }
        markDirty();
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public boolean hasActiveContraptions() {
        return activeWheelCount > 0;
    }

    public boolean hasClientContraptionEntity() {
        if (getLevel() == null || !getLevel().isClientSide()) {
            return hasActiveContraptions();
        }
        for (BlockPos axisPos : getKnownAxisPositions(getDamFacing())) {
            if (findContraptionEntityAt(axisPos) != null) {
                return true;
            }
        }
        return false;
    }

    public BlockPos getAxisPosition() {
        if (!segments.isEmpty()) {
            return segments.get(0).axisPos();
        }
        BlockPos locatedAxis = locateAxisPos(getPos());
        if (locatedAxis != null) {
            return locatedAxis;
        }
        return DamBlueprint.toWorld(getPos(), DamBlueprint.AXIS_LOCAL, getDamFacing());
    }

    private void serverTickDam() {
        if (!isFormed() || getLevel() == null || getLevel().isClientSide()) {
            return;
        }

        if (pendingRecovery || segments.isEmpty() || getOffsetTimer() % FULL_RECOVERY_INTERVAL == 0) {
            pendingRecovery = false;
            recoverFormedState();
        } else if (getOffsetTimer() % LIGHT_REFRESH_INTERVAL == 0) {
            refreshKnownState();
        }

        updateRotation();
        tickContraptions();
    }

    private void recoverFormedState() {
        if (getLevel() == null || getLevel().isClientSide() || !isFormed()) {
            return;
        }

        structureIssues.clear();
        Direction facing = getDamFacing();
        if (!matchesSegmentStructure(getPos(), 0, facing)) {
            structureIssues.add("Main dam structure no longer matches the blueprint.");
            markLastIssue();
            invalidateGtceuStructure();
            return;
        }

        if (!rebuildSegments(facing)) {
            resetOperationalState(false);
            markLastIssue();
            markDirty();
            return;
        }

        updateFlowAndStress(facing);
        assignOutputsToHatches();
        assembleDamContraptions();
        updateActiveWheelCount();
        markLastIssue();
        markDirty();
    }

    private void refreshKnownState() {
        if (getLevel() == null || getLevel().isClientSide() || !isFormed()) {
            return;
        }

        structureIssues.clear();
        Direction facing = getDamFacing();
        if (!validateKnownSegments(facing)) {
            pendingRecovery = true;
            markLastIssue();
            markDirty();
            return;
        }

        updateFlowAndStress(facing);
        assignOutputsToHatches();
        assembleDamContraptions();
        updateActiveWheelCount();
        markLastIssue();
        markDirty();
    }

    private boolean rebuildSegments(Direction facing) {
        List<DamSegmentState> detected = collectSegments(facing);
        if (detected.isEmpty()) {
            structureIssues.add("No valid dam segments were detected.");
            return false;
        }

        Map<Integer, DamSegmentState> previousSegments = new HashMap<>();
        for (DamSegmentState segment : segments) {
            previousSegments.put(segment.index(), segment);
        }

        Set<Integer> keptIndexes = new HashSet<>();
        for (DamSegmentState detectedSegment : detected) {
            keptIndexes.add(detectedSegment.index());
            DamSegmentState previous = previousSegments.get(detectedSegment.index());
            if (previous == null) {
                continue;
            }
            detectedSegment.setContraptionId(previous.contraptionId());
            detectedSegment.setAssembled(previous.assembled());
        }

        for (DamSegmentState previous : segments) {
            if (!keptIndexes.contains(previous.index())) {
                disassembleSegment(previous);
            }
        }

        segments.clear();
        segments.addAll(detected);
        segmentCount = segments.size();
        connectedDamCount = Math.max(0, segmentCount - 1);
        return true;
    }

    private boolean validateKnownSegments(Direction facing) {
        if (segments.isEmpty()) {
            structureIssues.add("No known dam segments are available for refresh.");
            return false;
        }

        for (int i = 0; i < segments.size(); i++) {
            DamSegmentState segment = segments.get(i);
            boolean structureValid = i == 0
                    ? DamMultiblockPatterns.matchesMainEntityized(getLevel(), segment.controllerPos(), facing)
                    : DamMultiblockPatterns.matchesStackEntityized(getLevel(), segment.controllerPos(), facing);
            if (!structureValid) {
                structureIssues.add("Segment " + (i + 1) + " no longer matches the formed dam structure.");
                return false;
            }

            if (!validateOutputChain(segment, facing)) {
                return false;
            }
        }

        return true;
    }

    private List<DamSegmentState> collectSegments(Direction facing) {
        List<DamSegmentState> detected = new ArrayList<>();

        for (int segmentIndex = 0; segmentIndex < DamBlueprint.MAX_SEGMENTS; segmentIndex++) {
            BlockPos segmentControllerPos = DamBlueprint.stackControllerPos(getPos(), facing, segmentIndex);
            boolean validStructure = matchesSegmentStructure(segmentControllerPos, segmentIndex, facing);
            if (!validStructure) {
                if (segmentIndex == 0) {
                    structureIssues.add("Primary dam segment does not match the blueprint.");
                }
                break;
            }

            DamSegmentGeometry geometry = locateSegmentGeometry(segmentControllerPos);
            if (geometry == null) {
                if (segmentIndex == 0) {
                    return List.of();
                }
                break;
            }

            if (segmentIndex > 0 && !isStackedSegmentGeometryValid(detected, segmentControllerPos, geometry, facing)) {
                break;
            }

            DamSegmentState state = new DamSegmentState(
                    segmentIndex,
                    segmentControllerPos,
                    geometry.axisPos(),
                    geometry.shaftPos(),
                    geometry.hatchPos()
            );

            if (!validateOutputChain(state, facing)) {
                if (segmentIndex == 0) {
                    return List.of();
                }
                break;
            }

            detected.add(state);
        }

        return detected;
    }

    private boolean validateOutputChain(DamSegmentState segment, Direction facing) {
        normalizeOutputChain(segment, facing);
        Direction outputDirection = getSegmentOutputDirection(segment, facing);

        BlockState shaftState = getLevel().getBlockState(segment.shaftPos());
        if (!(shaftState.getBlock() instanceof DamShaftBlock)) {
            structureIssues.add("Segment " + (segment.index() + 1) + " is missing its dam shaft.");
            segment.setHatchValid(false);
            return false;
        }
        if (shaftState.getValue(RotatedPillarBlock.AXIS) != outputDirection.getAxis()) {
            structureIssues.add("Segment " + (segment.index() + 1) + " has a misaligned dam shaft.");
            segment.setHatchValid(false);
            return false;
        }

        BlockState hatchState = getLevel().getBlockState(segment.hatchPos());
        if (!(hatchState.getBlock() instanceof StressOutputHatchBlock hatchBlock)) {
            structureIssues.add("Segment " + (segment.index() + 1) + " is missing its stress output hatch.");
            segment.setHatchValid(false);
            return false;
        }
        if (hatchState.getValue(DirectionalBlock.FACING).getAxis() != outputDirection.getAxis()) {
            structureIssues.add("Segment " + (segment.index() + 1) + " has a misaligned stress output hatch.");
            segment.setHatchValid(false);
            return false;
        }

        if (!tierLocked && segment.index() == 0) {
            tierIndex = hatchBlock.getTier().getIndex();
        }

        if (hatchBlock.getTier() != getTier()) {
            structureIssues.add("Segment " + (segment.index() + 1) + " uses "
                    + hatchBlock.getTier().getDisplayName()
                    + " stress output hatches, but the controller is set to "
                    + getTier().getDisplayName() + ".");
            segment.setHatchValid(false);
            return false;
        }

        segment.setHatchValid(true);
        return true;
    }

    private void updateFlowAndStress(Direction facing) {
        double totalFlow = 0.0;
        for (DamSegmentState segment : segments) {
            double flow = sampleSegmentFlow(segment, facing);
            segment.setFlowSpeed(flow);
            totalFlow += flow;
        }

        riverFlowSpeed = segments.isEmpty() ? 0.0 : totalFlow / segments.size();
        stressOutput = getTier().calculateTotalStress(segments.size(), riverFlowSpeed);
        rotationSpeed = (float) getTier().calculateRpm(riverFlowSpeed);

        double segmentStress = getTier().calculateSegmentStress(segments.size(), riverFlowSpeed);
        for (DamSegmentState segment : segments) {
            segment.setStressShare(segmentStress);
        }
    }

    private void assignOutputsToHatches() {
        if (getLevel() == null) {
            return;
        }
        for (DamSegmentState segment : segments) {
            if (getLevel().getBlockEntity(segment.hatchPos()) instanceof StressOutputHatchBlockEntity hatch) {
                hatch.setOutputParameters(
                        rotationSpeed,
                        segment.stressShare(),
                        getPos(),
                        segment.index(),
                        segments.size(),
                        segment.hatchValid()
                );
                hatch.setChanged();
            }
        }
    }

    private void unlinkStressHatches() {
        if (getLevel() == null) {
            return;
        }
        for (DamSegmentState segment : segments) {
            if (getLevel().getBlockEntity(segment.hatchPos()) instanceof StressOutputHatchBlockEntity hatch) {
                hatch.unlink();
                hatch.setChanged();
            }
        }
    }

    private void assembleDamContraptions() {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }

        IControlContraption controller = resolveContraptionController();
        debugContraption("Assembling dam contraptions for {} segment(s); controller={}",
                segments.size(),
                controller == null ? "null" : controller.getClass().getName());
        if (controller == null) {
            structureIssues.add("Controller block entity is unavailable for contraption binding.");
            return;
        }

        for (DamSegmentState segment : segments) {
            Direction damFacing = getDamFacing();
            String stage = "find_existing_entity";
            DamWheelContraption contraption = null;
            try {
                ControlledContraptionEntity existing = findContraptionEntity(segment);
                debugContraption(
                        "Processing dam segment {} ctrl={} axis={} shaft={} hatch={} facing={} hatchValid={} assembled={} contraptionId={} existing={}",
                        segment.index() + 1,
                        segment.controllerPos(),
                        segment.axisPos(),
                        segment.shaftPos(),
                        segment.hatchPos(),
                        damFacing,
                        segment.hatchValid(),
                        segment.assembled(),
                        segment.contraptionId(),
                        existing == null ? "null" : existing.getUUID()
                );
                if (existing != null) {
                    debugContraption("Reusing dam wheel entity {} for segment {} at {}",
                            existing.getUUID(), segment.index() + 1, segment.axisPos());
                    segment.setContraptionId(existing.getUUID());
                    existing.setPos(segment.axisPos().getX(), segment.axisPos().getY(), segment.axisPos().getZ());
                    existing.setRotationAxis(damFacing.getAxis());
                    existing.setAngle(rotationAngle);
                    purgeCapturedBladeBlocks(existing.getContraption(), segment.axisPos());
                    segment.setAssembled(true);
                    continue;
                }

                stage = "assemble_contraption";
                contraption = new DamWheelContraption(segment, damFacing);
                boolean assembled = contraption.assemble(getLevel(), segment.axisPos());
                debugContraption("Dam wheel assemble finished for segment {} at {} using facing {} -> assembled={}, capturedBlocks={}",
                        segment.index() + 1, segment.axisPos(), damFacing, assembled, contraption.getBlocks().size());
                if (!assembled) {
                    debugContraption("No dam blade blocks captured for segment {} at {} using facing {}",
                            segment.index() + 1, segment.axisPos(), damFacing);
                    structureIssues.add("Segment " + (segment.index() + 1) + " has no treated wood blade blocks to assemble.");
                    segment.setAssembled(false);
                    continue;
                }

                stage = "remove_blocks_from_world";
                debugContraption("Removing {} captured blade blocks from world for segment {} at {}",
                        contraption.getBlocks().size(), segment.index() + 1, segment.axisPos());
                contraption.removeBlocksFromWorld(getLevel(), BlockPos.ZERO);
                purgeCapturedBladeBlocks(contraption, segment.axisPos());

                stage = "create_controlled_entity";
                debugContraption("Creating controlled contraption entity for segment {} at {}",
                        segment.index() + 1, segment.axisPos());
                ControlledContraptionEntity entity = ControlledContraptionEntity.create(getLevel(), controller, contraption);
                entity.setPos(segment.axisPos().getX(), segment.axisPos().getY(), segment.axisPos().getZ());
                entity.setRotationAxis(damFacing.getAxis());
                entity.setAngle(rotationAngle);

                stage = "spawn_entity";
                boolean added = getLevel().addFreshEntity(entity);
                if (!added) {
                    debugContraption("Failed to spawn dam wheel entity for segment {} at {} (capturedBlocks={})",
                            segment.index() + 1, segment.axisPos(), contraption.getBlocks().size());
                    segment.setContraptionId(null);
                    segment.setAssembled(false);
                    continue;
                }

                debugContraption("Spawned dam wheel entity {} for segment {} at {} (capturedBlocks={})",
                        entity.getUUID(), segment.index() + 1, segment.axisPos(), contraption.getBlocks().size());
                segment.setContraptionId(entity.getUUID());
                segment.setAssembled(true);
            } catch (Throwable throwable) {
                logContraptionFailure(segment, damFacing, stage, contraption, throwable);
                structureIssues.add("Segment " + (segment.index() + 1) + " failed during " + stage + ": "
                        + throwable.getClass().getSimpleName());
                segment.setContraptionId(null);
                segment.setAssembled(false);
            }
        }
    }

    private void disassembleDamContraptions() {
        if (getLevel() == null) {
            return;
        }
        for (DamSegmentState segment : segments) {
            disassembleSegment(segment);
        }
        updateActiveWheelCount();
    }

    private void disassembleSegment(DamSegmentState segment) {
        ControlledContraptionEntity entity = findContraptionEntity(segment.contraptionId());
        if (entity != null) {
            entity.disassemble();
        }
        if (getLevel() != null && getLevel().getBlockEntity(segment.hatchPos()) instanceof StressOutputHatchBlockEntity hatch) {
            hatch.unlink();
            hatch.setChanged();
        }
        segment.setContraptionId(null);
        segment.setAssembled(false);
    }

    private void tickContraptions() {
        if (getLevel() == null) {
            return;
        }

        for (DamSegmentState segment : segments) {
            Direction damFacing = getDamFacing();
            ControlledContraptionEntity entity = findContraptionEntity(segment);
            if (entity == null) {
                if (segment.assembled() || segment.contraptionId() != null) {
                    debugContraption("Dam wheel entity missing for segment {} at {}; marking for reassembly.",
                            segment.index() + 1, segment.axisPos());
                }
                segment.setContraptionId(null);
                segment.setAssembled(false);
                continue;
            }
            segment.setContraptionId(entity.getUUID());
            entity.setPos(segment.axisPos().getX(), segment.axisPos().getY(), segment.axisPos().getZ());
            entity.setRotationAxis(damFacing.getAxis());
            entity.setAngle(rotationAngle);
            segment.setAssembled(true);
        }
        updateActiveWheelCount();
    }

    private void tickClientContraptions() {
        if (getLevel() == null || !getLevel().isClientSide()) {
            return;
        }

        int foundEntities = 0;
        Direction damFacing = getDamFacing();
        for (DamSegmentState segment : segments) {
            BlockPos axisPos = segment.axisPos();
            ControlledContraptionEntity entity = findContraptionEntityAt(axisPos);
            if (entity == null) {
                continue;
            }
            entity.setPos(axisPos.getX(), axisPos.getY(), axisPos.getZ());
            entity.setRotationAxis(damFacing.getAxis());
            entity.setAngle(rotationAngle);
            foundEntities++;
        }

        if (activeWheelCount <= 0 && foundEntities > 0) {
            activeWheelCount = foundEntities;
        }
    }

    private boolean matchesSegmentStructure(BlockPos segmentControllerPos, int segmentIndex, Direction facing) {
        boolean strictMatch = segmentIndex == 0
                ? DamMultiblockPatterns.matchesMain(getLevel(), segmentControllerPos, facing)
                : DamMultiblockPatterns.matchesStack(getLevel(), segmentControllerPos, facing);
        if (strictMatch) {
            return true;
        }

        BlockPos axisPos = locateAxisPos(segmentControllerPos);
        if (axisPos == null) {
            axisPos = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, facing);
        }
        if (findContraptionEntityAt(axisPos) == null) {
            return false;
        }

        return segmentIndex == 0
                ? DamMultiblockPatterns.matchesMainEntityized(getLevel(), segmentControllerPos, facing)
                : DamMultiblockPatterns.matchesStackEntityized(getLevel(), segmentControllerPos, facing);
    }

    private void normalizeOutputChain(DamSegmentState segment, Direction facing) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }

        Direction outputDirection = getSegmentOutputDirection(segment, facing);
        BlockState shaftState = getLevel().getBlockState(segment.shaftPos());
        if (shaftState.getBlock() instanceof DamShaftBlock && shaftState.getValue(RotatedPillarBlock.AXIS) != outputDirection.getAxis()) {
            getLevel().setBlock(segment.shaftPos(), shaftState.setValue(RotatedPillarBlock.AXIS, outputDirection.getAxis()), 3);
        }

        BlockState hatchState = getLevel().getBlockState(segment.hatchPos());
        if (hatchState.getBlock() instanceof StressOutputHatchBlock && hatchState.getValue(DirectionalBlock.FACING) != outputDirection) {
            getLevel().setBlock(segment.hatchPos(), hatchState.setValue(DirectionalBlock.FACING, outputDirection), 3);
        }
    }

    @Nullable
    private ControlledContraptionEntity findContraptionEntity(@Nullable UUID contraptionId) {
        if (contraptionId == null || getLevel() == null) {
            return null;
        }
        AABB searchBox = new AABB(getPos()).inflate(64);
        for (ControlledContraptionEntity entity : getLevel().getEntitiesOfClass(ControlledContraptionEntity.class, searchBox)) {
            if (contraptionId.equals(entity.getUUID()) && isDamWheelEntity(entity)) {
                return entity;
            }
        }
        return null;
    }

    @Nullable
    private ControlledContraptionEntity findContraptionEntity(DamSegmentState segment) {
        ControlledContraptionEntity byId = findContraptionEntity(segment.contraptionId());
        if (byId != null) {
            return byId;
        }
        return findContraptionEntityAt(segment.axisPos());
    }

    @Nullable
    private ControlledContraptionEntity findContraptionEntityAt(BlockPos axisPos) {
        if (getLevel() == null) {
            return null;
        }
        AABB searchBox = new AABB(axisPos).inflate(CONTRAPTION_SEARCH_RADIUS);
        return getLevel().getEntitiesOfClass(ControlledContraptionEntity.class, searchBox).stream()
                .filter(this::isDamWheelEntity)
                .min(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(
                        axisPos.getX(),
                        axisPos.getY(),
                        axisPos.getZ()
                )))
                .orElse(null);
    }

    private boolean isDamWheelEntity(@Nullable ControlledContraptionEntity entity) {
        return entity != null && entity.getContraption() instanceof DamWheelContraption;
    }

    private boolean isStackedSegmentGeometryValid(List<DamSegmentState> detected, BlockPos segmentControllerPos,
                                                  DamSegmentGeometry geometry, Direction facing) {
        BlockPos expectedAxis = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, facing);
        if (!geometry.axisPos().equals(expectedAxis)) {
            debugAxisProbe("Rejecting phantom dam segment at {}: expected axis {}, resolved axis {}",
                    segmentControllerPos, expectedAxis, geometry.axisPos());
            return false;
        }

        for (DamSegmentState existing : detected) {
            if (existing.axisPos().equals(geometry.axisPos())
                    || existing.shaftPos().equals(geometry.shaftPos())
                    || existing.hatchPos().equals(geometry.hatchPos())) {
                debugAxisProbe("Rejecting duplicate dam segment at {}: axis {}, shaft {}, hatch {}",
                        segmentControllerPos, geometry.axisPos(), geometry.shaftPos(), geometry.hatchPos());
                return false;
            }
        }

        return true;
    }

    private List<BlockPos> getKnownAxisPositions(Direction facing) {
        List<BlockPos> axisPositions = new ArrayList<>();
        if (!segments.isEmpty()) {
            for (DamSegmentState segment : segments) {
                axisPositions.add(segment.axisPos());
            }
            return axisPositions;
        }

        int knownSegments = Math.max(segmentCount, 1);
        for (int segmentIndex = 0; segmentIndex < knownSegments; segmentIndex++) {
            BlockPos segmentControllerPos = DamBlueprint.stackControllerPos(getPos(), facing, segmentIndex);
            axisPositions.add(DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, facing));
        }
        return axisPositions;
    }

    @Nullable
    private BlockPos findNearestKnownAxisPos(BlockPos referencePos, Direction facing) {
        return getKnownAxisPositions(facing).stream()
                .min(Comparator.comparingDouble(axisPos -> axisPos.distSqr(referencePos)))
                .orElse(null);
    }

    @Nullable
    private DamSegmentGeometry locateSegmentGeometry(BlockPos segmentControllerPos) {
        DamSegmentGeometry blueprintGeometry = locateGeometryFromBlueprintRotations(segmentControllerPos);
        if (blueprintGeometry != null) {
            return blueprintGeometry;
        }

        DamSegmentGeometry outputChainGeometry = locateGeometryFromOutputChain(segmentControllerPos);
        if (outputChainGeometry != null) {
            return outputChainGeometry;
        }

        BlockPos axisPos = locateAxisPos(segmentControllerPos);
        if (axisPos == null) {
            structureIssues.add("Segment axis could not be located from the iron hub.");
            return null;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!getLevel().getBlockState(axisPos.relative(direction)).is(Blocks.OAK_LOG)) {
                continue;
            }

            BlockPos shaftPos = axisPos.relative(direction, 2);
            BlockPos hatchPos = axisPos.relative(direction, 3);
            if (!(getLevel().getBlockState(shaftPos).getBlock() instanceof DamShaftBlock)) {
                continue;
            }
            if (!(getLevel().getBlockState(hatchPos).getBlock() instanceof StressOutputHatchBlock)) {
                continue;
            }
            return new DamSegmentGeometry(axisPos, shaftPos, hatchPos);
        }

        structureIssues.add("Segment output chain could not be located from the iron hub.");
        return null;
    }

    @Nullable
    private DamSegmentGeometry locateGeometryFromBlueprintRotations(BlockPos segmentControllerPos) {
        if (getLevel() == null) {
            return null;
        }

        BlockPos preferredAxis = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, getDamFacing());
        DamSegmentGeometry bestGeometry = null;
        int bestScore = Integer.MIN_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos axisPos = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, direction);
            BlockPos shaftPos = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.SHAFT_LOCAL, direction);
            BlockPos hatchPos = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.HATCH_LOCAL, direction);
            int score = scoreGeometryCandidate(axisPos, shaftPos, hatchPos);
            double distance = axisPos.distSqr(preferredAxis);
            if (score > bestScore || (score == bestScore && distance < bestDistance)) {
                bestScore = score;
                bestDistance = distance;
                bestGeometry = new DamSegmentGeometry(axisPos, shaftPos, hatchPos);
            }
        }

        if (bestGeometry != null && bestScore >= 22) {
            debugAxisProbe("Resolved dam segment geometry from blueprint rotations at {} -> axis {}, shaft {}, hatch {} (score={})",
                    segmentControllerPos, bestGeometry.axisPos(), bestGeometry.shaftPos(), bestGeometry.hatchPos(), bestScore);
            return bestGeometry;
        }
        if (bestGeometry != null) {
            debugAxisProbe("Blueprint rotation probe near {} was inconclusive: axis {}, shaft {}, hatch {} (score={})",
                    segmentControllerPos, bestGeometry.axisPos(), bestGeometry.shaftPos(), bestGeometry.hatchPos(), bestScore);
        }
        return null;
    }

    private int scoreGeometryCandidate(BlockPos axisPos, BlockPos shaftPos, BlockPos hatchPos) {
        if (getLevel() == null) {
            return Integer.MIN_VALUE;
        }

        BlockState axisState = getLevel().getBlockState(axisPos);
        int score = axisState.is(Blocks.IRON_BLOCK) ? 12 : -16;
        if (axisState.is(Blocks.IRON_BLOCK)) {
            score += 8;
        }
        BlockState shaftState = getLevel().getBlockState(shaftPos);
        if (shaftState.getBlock() instanceof DamShaftBlock) {
            score += 8;
        }
        BlockState hatchState = getLevel().getBlockState(hatchPos);
        if (hatchState.getBlock() instanceof StressOutputHatchBlock) {
            score += 6;
        }

        Direction outputDirection = horizontalDirectionFromDelta(hatchPos.subtract(axisPos));
        if (outputDirection == null) {
            return score;
        }

        if (axisPos.relative(outputDirection).equals(shaftPos.relative(outputDirection.getOpposite()))) {
            BlockPos connectorPos = axisPos.relative(outputDirection);
            if (getLevel().getBlockState(connectorPos).is(Blocks.OAK_LOG)) {
                score += 4;
            }
        }
        if (shaftPos.equals(axisPos.relative(outputDirection, 2)) && shaftState.getBlock() instanceof DamShaftBlock
                && shaftState.getValue(RotatedPillarBlock.AXIS) == outputDirection.getAxis()) {
            score += 2;
        }
        if (hatchPos.equals(shaftPos.relative(outputDirection)) && hatchState.getBlock() instanceof StressOutputHatchBlock
                && hatchState.getValue(DirectionalBlock.FACING).getAxis() == outputDirection.getAxis()) {
            score += 2;
        }

        return score;
    }

    @Nullable
    private DamSegmentGeometry locateGeometryFromOutputChain(BlockPos segmentControllerPos) {
        if (getLevel() == null) {
            return null;
        }

        BlockPos predictedAxis = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, getDamFacing());
        DamSegmentGeometry bestGeometry = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -AXIS_SEARCH_HORIZONTAL_RADIUS; dx <= AXIS_SEARCH_HORIZONTAL_RADIUS; dx++) {
            for (int dy = -AXIS_SEARCH_VERTICAL_DOWN; dy <= AXIS_SEARCH_VERTICAL_UP; dy++) {
                for (int dz = -AXIS_SEARCH_HORIZONTAL_RADIUS; dz <= AXIS_SEARCH_HORIZONTAL_RADIUS; dz++) {
                    BlockPos shaftPos = segmentControllerPos.offset(dx, dy, dz);
                    BlockState shaftState = getLevel().getBlockState(shaftPos);
                    if (!(shaftState.getBlock() instanceof DamShaftBlock)) {
                        continue;
                    }

                    Direction.Axis shaftAxis = shaftState.getValue(RotatedPillarBlock.AXIS);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (direction.getAxis() != shaftAxis) {
                            continue;
                        }

                        BlockPos hatchPos = shaftPos.relative(direction);
                        if (!(getLevel().getBlockState(hatchPos).getBlock() instanceof StressOutputHatchBlock)) {
                            continue;
                        }

                        BlockPos axisPos = shaftPos.relative(direction.getOpposite(), 2);
                        if (!getLevel().getBlockState(axisPos).is(Blocks.IRON_BLOCK)) {
                            continue;
                        }

                        double distance = axisPos.distSqr(predictedAxis);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestGeometry = new DamSegmentGeometry(axisPos, shaftPos, hatchPos);
                        }
                    }
                }
            }
        }

        return bestGeometry;
    }

    @Nullable
    private BlockPos locateAxisPos(BlockPos segmentControllerPos) {
        if (getLevel() == null) {
            return null;
        }

        BlockPos predictedAxis = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, getDamFacing());
        BlockPos bestPos = null;
        int bestScore = Integer.MIN_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, direction);
            int score = scoreAxisCandidate(candidate);
            double distance = candidate.distSqr(predictedAxis);
            if (score > bestScore || (score == bestScore && distance < bestDistance)) {
                bestScore = score;
                bestDistance = distance;
                bestPos = candidate;
            }
        }
        if (bestPos != null && bestScore > 0) {
            debugAxisProbe("Resolved dam axis from blueprint rotations at {} -> {} (score={})",
                    segmentControllerPos, bestPos, bestScore);
            return bestPos;
        }

        for (int dx = -AXIS_SEARCH_HORIZONTAL_RADIUS; dx <= AXIS_SEARCH_HORIZONTAL_RADIUS; dx++) {
            for (int dy = -AXIS_SEARCH_VERTICAL_DOWN; dy <= AXIS_SEARCH_VERTICAL_UP; dy++) {
                for (int dz = -AXIS_SEARCH_HORIZONTAL_RADIUS; dz <= AXIS_SEARCH_HORIZONTAL_RADIUS; dz++) {
                    BlockPos candidate = segmentControllerPos.offset(dx, dy, dz);
                    if (!getLevel().getBlockState(candidate).is(Blocks.IRON_BLOCK)) {
                        continue;
                    }

                    int score = scoreAxisCandidate(candidate);
                    double distance = candidate.distSqr(predictedAxis);
                    if (score > bestScore || (score == bestScore && distance < bestDistance)) {
                        bestScore = score;
                        bestDistance = distance;
                        bestPos = candidate;
                    }
                }
            }
        }

        if (bestPos != null && bestScore > 0) {
            debugAxisProbe("Resolved dam axis from world scan at {} -> {} (score={})",
                    segmentControllerPos, bestPos, bestScore);
            return bestPos;
        }
        debugAxisProbe("Failed to resolve dam axis near {} (bestPos={}, bestScore={})", segmentControllerPos, bestPos, bestScore);
        return bestPos;
    }

    private boolean hasOpposingOakLogs(BlockPos axisPos) {
        if (getLevel() == null) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (getLevel().getBlockState(axisPos.relative(direction)).is(Blocks.OAK_LOG)
                    && getLevel().getBlockState(axisPos.relative(direction.getOpposite())).is(Blocks.OAK_LOG)) {
                return true;
            }
        }
        return false;
    }

    private int scoreAxisCandidate(BlockPos axisPos) {
        if (getLevel() == null) {
            return Integer.MIN_VALUE;
        }

        BlockState axisState = getLevel().getBlockState(axisPos);
        int score = axisState.is(Blocks.IRON_BLOCK) ? 12 : -16;
        if (hasOpposingOakLogs(axisPos)) {
            score += 8;
        }
        if (hasOpposingBladeBlocks(axisPos)) {
            score += 6;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (getLevel().getBlockState(axisPos.relative(direction)).is(Blocks.OAK_LOG)) {
                score += 3;
            }
            if (DamStructureBlocks.isBladeBlock(getLevel().getBlockState(axisPos.relative(direction)))) {
                score += 2;
            }
            if (getLevel().getBlockState(axisPos.relative(direction, 2)).getBlock() instanceof DamShaftBlock) {
                score += 6;
            }
            if (getLevel().getBlockState(axisPos.relative(direction, 3)).getBlock() instanceof StressOutputHatchBlock) {
                score += 4;
            }
        }

        return score;
    }

    private boolean hasOpposingBladeBlocks(BlockPos axisPos) {
        if (getLevel() == null) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (DamStructureBlocks.isBladeBlock(getLevel().getBlockState(axisPos.relative(direction)))
                    && DamStructureBlocks.isBladeBlock(getLevel().getBlockState(axisPos.relative(direction.getOpposite())))) {
                return true;
            }
        }
        return false;
    }

    private void debugAxisProbe(String message, Object... args) {
        if (getLevel() != null && !getLevel().isClientSide()) {
            LOGGER.debug(message, args);
        }
    }

    private void debugContraption(String message, Object... args) {
        if (getLevel() != null && !getLevel().isClientSide()) {
            LOGGER.debug(message, args);
        }
    }

    private void logContraptionFailure(DamSegmentState segment, Direction damFacing, String stage,
                                       @Nullable DamWheelContraption contraption, Throwable throwable) {
        long gameTime = getLevel() != null ? getLevel().getGameTime() : Long.MIN_VALUE;
        String failureKey = stage + "|" + segment.index() + "|" + throwable.getClass().getName() + "|" + throwable.getMessage();
        if (failureKey.equals(lastContraptionFailureKey) && gameTime - lastContraptionFailureTick < LIGHT_REFRESH_INTERVAL * 5L) {
            debugContraption("Dam wheel pipeline repeated failure at {} for segment {}: {}: {}",
                    stage, segment.index() + 1, throwable.getClass().getSimpleName(), throwable.getMessage());
            return;
        }
        lastContraptionFailureKey = failureKey;
        lastContraptionFailureTick = gameTime;
        int capturedBlocks = contraption == null ? -1 : contraption.getBlocks().size();
        LOGGER.error(
                "Dam wheel pipeline failed at {} for segment {} ctrl={} axis={} shaft={} hatch={} facing={} contraptionId={} capturedBlocks={}",
                stage,
                segment.index() + 1,
                segment.controllerPos(),
                segment.axisPos(),
                segment.shaftPos(),
                segment.hatchPos(),
                damFacing,
                segment.contraptionId(),
                capturedBlocks,
                throwable
        );
    }

    private Direction getDamFacing() {
        Direction frontFacing = getFrontFacing();
        if (getLevel() == null) {
            return frontFacing;
        }

        Direction bestFacing = frontFacing;
        int bestScore = Integer.MIN_VALUE;
        int bestTieBreak = Integer.MIN_VALUE;
        for (Direction candidate : Direction.Plane.HORIZONTAL) {
            BlockPos axisPos = DamBlueprint.toWorld(getPos(), DamBlueprint.AXIS_LOCAL, candidate);
            BlockPos shaftPos = DamBlueprint.toWorld(getPos(), DamBlueprint.SHAFT_LOCAL, candidate);
            BlockPos hatchPos = DamBlueprint.toWorld(getPos(), DamBlueprint.HATCH_LOCAL, candidate);
            int score = scoreGeometryCandidate(axisPos, shaftPos, hatchPos);
            int tieBreak = candidate == frontFacing ? 2 : candidate == frontFacing.getOpposite() ? 1 : 0;
            if (score > bestScore || (score == bestScore && tieBreak > bestTieBreak)) {
                bestScore = score;
                bestTieBreak = tieBreak;
                bestFacing = candidate;
            }
        }

        return bestScore > Integer.MIN_VALUE ? bestFacing : frontFacing;
    }

    private void purgeCapturedBladeBlocks(@Nullable Contraption contraption, BlockPos anchorPos) {
        if (getLevel() == null || getLevel().isClientSide() || contraption == null) {
            return;
        }

        contraption.getBlocks().forEach((localPos, blockInfo) -> {
            BlockPos worldPos = anchorPos.offset(localPos);
            BlockState worldState = getLevel().getBlockState(worldPos);
            if (DamStructureBlocks.isBladeBlock(worldState)) {
                getLevel().setBlock(worldPos, Blocks.AIR.defaultBlockState(), 67);
            }
        });
    }

    private Direction getSegmentOutputDirection(DamSegmentState segment, Direction fallbackFacing) {
        Direction direction = horizontalDirectionFromDelta(segment.hatchPos().subtract(segment.axisPos()));
        if (direction != null) {
            return direction;
        }
        return fallbackFacing;
    }

    @Nullable
    private Direction horizontalDirectionFromDelta(BlockPos delta) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (Integer.signum(delta.getX()) == direction.getStepX()
                    && Integer.signum(delta.getZ()) == direction.getStepZ()) {
                return direction;
            }
        }
        return null;
    }

    private double sampleSegmentFlow(DamSegmentState segment, Direction fallbackFacing) {
        if (getLevel() == null) {
            return 0.0;
        }

        double bestFlow = 0.0;
        Direction preferredDirection = getSegmentOutputDirection(segment, fallbackFacing);
        bestFlow = Math.max(bestFlow, RiverCurrentSampler.sampleWheelFrontFlow(getLevel(), segment.axisPos(), preferredDirection));
        bestFlow = Math.max(bestFlow, RiverCurrentSampler.sampleWheelFrontFlow(getLevel(), segment.axisPos(), preferredDirection.getOpposite()));

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            bestFlow = Math.max(bestFlow, RiverCurrentSampler.sampleWheelFrontFlow(getLevel(), segment.axisPos(), direction));
        }
        return bestFlow;
    }

    private record DamSegmentGeometry(BlockPos axisPos, BlockPos shaftPos, BlockPos hatchPos) {
    }

    @Nullable
    private IControlContraption resolveContraptionController() {
        if (holder instanceof WaterDamMachineBlockEntity blockEntity) {
            return blockEntity;
        }
        if (getLevel() != null) {
            var blockEntity = getLevel().getBlockEntity(getPos());
            if (blockEntity instanceof IControlContraption controller) {
                return controller;
            }
        }
        return null;
    }

    private void updateRotation() {
        if (rotationSpeed <= 0.0F) {
            return;
        }
        rotationAngle += rotationSpeed * 0.3F;
        if (rotationAngle >= 360.0F) {
            rotationAngle -= 360.0F;
        }
    }

    private void updateActiveWheelCount() {
        int activeCount = 0;
        for (DamSegmentState segment : segments) {
            if (segment.assembled()) {
                activeCount++;
            }
        }
        activeWheelCount = activeCount;
    }

    private void markLastIssue() {
        lastIssue = structureIssues.isEmpty() ? "" : structureIssues.get(0);
    }

    private Component getTierDisplayComponent() {
        var component = getTier().getDisplayComponent().copy();
        if (!tierLocked) {
            component.append(Component.literal(" (auto)").withStyle(ChatFormatting.DARK_GRAY));
        }
        return component;
    }

    private void invalidateGtceuStructure() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        onStructureInvalid();
        getMultiblockState().setError(MultiblockState.UNLOAD_ERROR);
        MultiblockWorldSavedData savedData = MultiblockWorldSavedData.getOrCreate(serverLevel);
        savedData.removeMapping(getMultiblockState());
        savedData.addAsyncLogic(this);
    }

    private void resetOperationalState(boolean clearIssues) {
        disassembleDamContraptions();
        unlinkStressHatches();
        segments.clear();
        if (clearIssues) {
            structureIssues.clear();
        }
        riverFlowSpeed = 0.0;
        stressOutput = 0.0;
        rotationSpeed = 0.0F;
        segmentCount = 0;
        connectedDamCount = 0;
        activeWheelCount = 0;
        if (clearIssues) {
            lastIssue = "";
        }
    }
}
