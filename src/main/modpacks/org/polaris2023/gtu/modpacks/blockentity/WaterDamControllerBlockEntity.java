package org.polaris2023.gtu.modpacks.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.polaris2023.gtu.modpacks.block.DamShaftBlock;
import org.polaris2023.gtu.modpacks.block.StressOutputHatchBlock;
import org.polaris2023.gtu.modpacks.block.WaterDamControllerBlock;
import org.polaris2023.gtu.modpacks.contraption.DamWheelContraption;
import org.polaris2023.gtu.modpacks.dam.DamBlueprint;
import org.polaris2023.gtu.modpacks.dam.DamMultiblockPatterns;
import org.polaris2023.gtu.modpacks.dam.DamSegmentState;
import org.polaris2023.gtu.modpacks.dam.DamStructureBlocks;
import org.polaris2023.gtu.modpacks.dam.DamTier;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.worldgen.river.RiverCurrentSampler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 水坝控制器方块实体。
 * <p>
 * 负责管理多方块成型检测、横向堆叠、应力计算、旋转状态和护目镜信息。
 * </p>
 */
public class WaterDamControllerBlockEntity extends BlockEntity implements IHaveGoggleInformation, IControlContraption, IUIHolder {

    // ---- 多方块状态 ----
    private boolean formed = false;
    private DamTier tier = DamTier.PRIMITIVE;
    private int connectedDamCount = 0;
    private double riverFlowSpeed = 0.0;
    private double stressOutput = 0.0;
    private float rotationAngle = 0.0f;
    private float rotationSpeed = 0.0f; // RPM
    private final List<DamSegmentState> segments = new ArrayList<>();
    private final List<String> structureIssues = new ArrayList<>();

    // 堆叠的水坝位置 (铁块轴心位置)
    private final List<BlockPos> stackedDamAxes = new ArrayList<>();

    // ---- 蓝图结构定义 (7x7x3) ----
    // 相对于控制器位置的结构偏移
    // 控制器在蓝图中的位置是 [6,6,1], 所以偏移 = blockPos - [6,6,1]

    /**
     * 表示蓝图中的一个方块检查点。
     */
    private record StructureBlock(int dx, int dy, int dz, StructureBlockType type) {}

    private enum StructureBlockType {
        STONE_BRICKS,           // 石砖（外框）
        TREATED_WOOD_PLANKS,    // 防腐木板（叶片）
        TREATED_WOOD_STAIRS,    // 防腐木楼梯（叶片）
        TREATED_WOOD_FRAME,     // 防腐木框架（叶片）
        IRON_BLOCK,             // 铁块（轴心）
        OAK_LOG,                // 橡木原木（轴承）
        ANDESITE_WALL,          // 安山岩墙（装饰/连接）
        CONTROLLER,             // 控制器自身
        AIR                     // 空气
    }

    // 所有方块相对于控制器的偏移 (controllerPos 在蓝图的 [6,6,1])
    // 所以结构块坐标 [x,y,z] 对应偏移 [x-6, y-6, z-1]
    // 但实际上要根据控制器朝向旋转这些偏移

    public WaterDamControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // ---- Tick 逻辑 ----
    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterDamControllerBlockEntity be) {
        if (level.getGameTime() % 20 == 0) {
            // 每秒更新一次
            be.updateFlowSpeed();
            if (be.formed) {
                be.updateStressOutput();
            }
        }
        if (be.formed) {
            if (level.getGameTime() % 20 == 0) {
                be.refreshMachineState();
            }
            be.tickContraptions();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WaterDamControllerBlockEntity be) {
        if (be.formed) {
            be.tickContraptions();
        }
    }

    // ---- 多方块成型检测 ----

    /**
     * 尝试成型多方块结构。
     *
     * @return true 如果结构匹配成功
     */
    public boolean tryForm() {
        if (level == null) return false;
        structureIssues.clear();
        Direction facing = getBlockState().getValue(WaterDamControllerBlock.FACING);

        // 检测主体结构
        if (!matchStructure(level, worldPosition, facing)) {
            disassemble();
            return false;
        }

        segments.clear();
        segments.addAll(collectSegments(facing));
        if (segments.isEmpty()) {
            structureIssues.add("未识别到任何有效坝段");
            disassemble();
            return false;
        }

        formed = true;

        // 检测堆叠
        detectStackedDams(facing);

        // 更新状态
        updateFlowSpeed();
        rebuildAxisCache();
        refreshMachineState();
        assembleDamContraptions();
        syncToClient();
        return true;
    }

    /**
     * 拆解多方块。
     */
    public void disassemble() {
        formed = false;
        connectedDamCount = 0;
        stressOutput = 0.0;
        rotationSpeed = 0.0f;
        disassembleDamContraptions();
        segments.clear();
        unlinkStressHatches();
        stackedDamAxes.clear();
        syncToClient();
    }

    /**
     * 匹配7x7x3蓝图结构。
     * 控制器在蓝图的 [6,6,1]，铁块在 [3,3,1]。
     * 需要根据控制器朝向旋转结构。
     */
    private boolean matchStructure(Level level, BlockPos controllerPos, Direction facing) {
        if (DamMultiblockPatterns.matchesMain(level, controllerPos, facing)) {
            return true;
        }
        // 从控制器位置推算蓝图原点
        // 蓝图中控制器在 [6,6,1]
        // facing = NORTH 时: 蓝图X轴 → 世界X轴正方向（east/right）, 蓝图Z轴 → 世界Z轴正方向
        // 我们需要根据facing旋转坐标系

        for (int bx = 0; bx < 7; bx++) {
            for (int by = 0; by < 7; by++) {
                for (int bz = 0; bz < 3; bz++) {
                    // 相对于控制器的偏移 (蓝图坐标 - 控制器蓝图坐标)
                    int dx = bx - 6;
                    int dy = by - 6;
                    int dz = bz - 1;

                    // 根据facing旋转水平偏移
                    BlockPos worldPos = rotateOffset(controllerPos, dx, dy, dz, facing);
                    StructureBlockType expected = getExpectedBlockType(bx, by, bz);

                    if (expected == null) {
                        // 此位置允许任意方块
                        continue;
                    }

                    if (expected == StructureBlockType.CONTROLLER) {
                        // 控制器位置 — 跳过检测（就是自己）
                        continue;
                    }

                    if (expected == StructureBlockType.AIR) {
                        if (!level.getBlockState(worldPos).isAir()) {
                            return false;
                        }
                        continue;
                    }

                    BlockState worldState = level.getBlockState(worldPos);
                    if (!matchesBlockType(worldState, expected)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 获取蓝图指定位置期望的方块类型。
     * 基于从NBT解析出的结构数据。
     */
    private StructureBlockType getExpectedBlockType(int x, int y, int z) {
        // 控制器 [6,6,1]
        if (x == 6 && y == 6 && z == 1) return StructureBlockType.CONTROLLER;

        // 铁块轴心 [3,3,1]
        if (x == 3 && y == 3 && z == 1) return StructureBlockType.IRON_BLOCK;

        // 橡木原木 (轴承) [3,3,0] 和 [3,3,2]
        if (x == 3 && y == 3 && (z == 0 || z == 2)) return StructureBlockType.OAK_LOG;

        // 防腐木板 (z=1 中间层叶片外圈)
        if (z == 1) {
            // 底部横排: [2,1,1], [3,1,1], [4,1,1]
            if (y == 1 && x >= 2 && x <= 4) return StructureBlockType.TREATED_WOOD_PLANKS;
            // 顶部横排: [2,5,1], [3,5,1], [4,5,1]
            if (y == 5 && x >= 2 && x <= 4) return StructureBlockType.TREATED_WOOD_PLANKS;
            // 左侧竖排: [1,2,1], [1,3,1], [1,4,1]
            if (x == 1 && y >= 2 && y <= 4) return StructureBlockType.TREATED_WOOD_PLANKS;
            // 右侧竖排: [5,2,1], [5,3,1], [5,4,1]
            if (x == 5 && y >= 2 && y <= 4) return StructureBlockType.TREATED_WOOD_PLANKS;

            // 防腐木楼梯
            if (x == 2 && y == 2) return StructureBlockType.TREATED_WOOD_STAIRS; // [2,2,1] west bottom
            if (x == 4 && y == 2) return StructureBlockType.TREATED_WOOD_STAIRS; // [4,2,1] east bottom
            if (x == 2 && y == 4) return StructureBlockType.TREATED_WOOD_STAIRS; // [2,4,1] west top
            if (x == 4 && y == 4) return StructureBlockType.TREATED_WOOD_STAIRS; // [4,4,1] east top

            // 防腐木框架
            if (x == 3 && y == 2) return StructureBlockType.TREATED_WOOD_FRAME; // [3,2,1]
            if (x == 2 && y == 3) return StructureBlockType.TREATED_WOOD_FRAME; // [2,3,1]
            if (x == 4 && y == 3) return StructureBlockType.TREATED_WOOD_FRAME; // [4,3,1]
            if (x == 3 && y == 4) return StructureBlockType.TREATED_WOOD_FRAME; // [3,4,1]
        }

        // 安山岩墙
        if (x == 3 && y == 1 && (z == 0 || z == 2)) return StructureBlockType.ANDESITE_WALL;
        if (x == 3 && y == 2 && (z == 0 || z == 2)) return StructureBlockType.ANDESITE_WALL;
        if (x == 3 && y == 4 && (z == 0 || z == 2)) return StructureBlockType.ANDESITE_WALL;
        if (x == 3 && y == 5 && (z == 0 || z == 2)) return StructureBlockType.ANDESITE_WALL;

        // 石砖外框 — 查找所有在NBT中标记为state=0的位置
        if (isStoneBrickPosition(x, y, z)) return StructureBlockType.STONE_BRICKS;

        // 空气/不关心的位置
        return null;
    }

    /**
     * 判断是否是石砖位置（基于NBT蓝图数据）。
     */
    private boolean isStoneBrickPosition(int x, int y, int z) {
        // Y=0层: z=0,2 x=1..5
        if (y == 0 && (z == 0 || z == 2) && x >= 1 && x <= 5) return true;
        // Y=1层: z=0,2 x=0,1,5,6
        if (y == 1 && (z == 0 || z == 2) && (x == 0 || x == 1 || x == 5 || x == 6)) return true;
        // Y=2层: z=0,2 x=0,6
        if (y == 2 && (z == 0 || z == 2) && (x == 0 || x == 6)) return true;
        // Y=3层: z=0,2 x=0,6
        if (y == 3 && (z == 0 || z == 2) && (x == 0 || x == 6)) return true;
        // Y=4层: z=0,2 x=0,6
        if (y == 4 && (z == 0 || z == 2) && (x == 0 || x == 6)) return true;
        // Y=5层: z=0,2 x=0,1,5,6
        if (y == 5 && (z == 0 || z == 2) && (x == 0 || x == 1 || x == 5 || x == 6)) return true;
        // Y=6层: z=0,2 x=1..5; z=1 x=0 (特殊的石砖)
        if (y == 6 && (z == 0 || z == 2) && x >= 1 && x <= 5) return true;
        if (y == 6 && z == 1 && x == 0) return true;
        return false;
    }

    /**
     * 根据控制器朝向旋转蓝图偏移到世界坐标。
     */
    private BlockPos rotateOffset(BlockPos controllerPos, int dx, int dy, int dz, Direction facing) {
        int worldDx, worldDz;
        switch (facing) {
            case NORTH -> {
                worldDx = -dx;
                worldDz = -dz;
            }
            case SOUTH -> {
                worldDx = dx;
                worldDz = dz;
            }
            case WEST -> {
                worldDx = dz;
                worldDz = -dx;
            }
            case EAST -> {
                worldDx = -dz;
                worldDz = dx;
            }
            default -> {
                worldDx = dx;
                worldDz = dz;
            }
        }
        return controllerPos.offset(worldDx, dy, worldDz);
    }

    /**
     * 检查世界方块状态是否匹配期望的类型。
     */
    private boolean matchesBlockType(BlockState state, StructureBlockType expected) {
        return switch (expected) {
            case STONE_BRICKS -> state.is(Blocks.STONE_BRICKS);
            case IRON_BLOCK -> state.is(Blocks.IRON_BLOCK);
            case OAK_LOG -> state.is(Blocks.OAK_LOG);
            case ANDESITE_WALL -> state.is(Blocks.ANDESITE_WALL);
            case TREATED_WOOD_PLANKS -> isGTBlock(state, "treated_wood_planks");
            case TREATED_WOOD_STAIRS -> isGTBlock(state, "treated_wood_stairs");
            case TREATED_WOOD_FRAME -> state.is(DamStructureBlocks.treatedWoodFrame());
            case CONTROLLER, AIR -> true; // 已在上层处理
        };
    }

    private boolean isGTBlock(BlockState state, String name) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key != null && "gtceu".equals(key.getNamespace()) && name.equals(key.getPath());
    }

    // ---- 横向堆叠检测 ----

    /**
     * 检测控制器左右两侧是否有额外的水坝结构（不含控制器方块）。
     * 堆叠方向：沿控制器朝向的垂直方向（蓝图的z轴方向的左右扩展）。
     */
    private void detectStackedDams(Direction facing) {
        stackedDamAxes.clear();

        // 蓝图宽度(z方向)为3, 所以堆叠间隔为3
        Direction stackDir = facing.getClockWise(); // 左方向
        Direction stackDirOpposite = facing.getCounterClockWise(); // 右方向

        // 向一侧扫描
        scanStackedDams(facing, stackDir);
        // 向另一侧扫描
        scanStackedDams(facing, stackDirOpposite);

        connectedDamCount = stackedDamAxes.size();
    }

    private void scanStackedDams(Direction facing, Direction stackDir) {
        for (int offset = 1; offset <= 16; offset++) {
            // 每个堆叠单元宽度为3（蓝图z方向）
            int shift = offset * 3;

            // 偏移控制器位置得到假想的新控制器位置
            BlockPos virtualControllerPos = worldPosition.relative(stackDir, shift);

            // 检测此位置是否有有效的水坝结构（无需控制器方块本身）
            if (matchStackedStructure(level, virtualControllerPos, facing)) {
                // 计算铁块轴心位置: 蓝图[3,3,1] 相对于控制器[6,6,1] 偏移 [-3,-3,0]
                BlockPos axisPos = rotateOffset(virtualControllerPos, -3, -3, 0, facing);
                stackedDamAxes.add(axisPos);
            } else {
                break; // 断开则停止扫描
            }
        }
    }

    /**
     * 检测堆叠的水坝结构（跳过控制器位置检测）。
     */
    private boolean matchStackedStructure(Level level, BlockPos virtualControllerPos, Direction facing) {
        if (DamMultiblockPatterns.matchesStack(level, virtualControllerPos, facing)) {
            return true;
        }
        for (int bx = 0; bx < 7; bx++) {
            for (int by = 0; by < 7; by++) {
                for (int bz = 0; bz < 3; bz++) {
                    int dx = bx - 6;
                    int dy = by - 6;
                    int dz = bz - 1;

                    BlockPos worldPos = rotateOffset(virtualControllerPos, dx, dy, dz, facing);
                    StructureBlockType expected = getExpectedBlockType(bx, by, bz);

                    if (expected == null || expected == StructureBlockType.CONTROLLER) {
                        continue; // 堆叠结构不需要控制器方块
                    }

                    if (expected == StructureBlockType.AIR) {
                        if (!level.getBlockState(worldPos).isAir()) {
                            return false;
                        }
                        continue;
                    }

                    BlockState worldState = level.getBlockState(worldPos);
                    if (!matchesBlockType(worldState, expected)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ---- 流速和应力更新 ----

    private void updateFlowSpeed() {
        if (level == null) return;
        Direction facing = getBlockState().getValue(WaterDamControllerBlock.FACING);
        riverFlowSpeed = RiverCurrentSampler.sampleWheelFrontFlow(level, getAxisPosition(), facing);
    }

    private void updateStressOutput() {
        stressOutput = tier.calculateStress(connectedDamCount, riverFlowSpeed);
        // 旋转速度与流速和等级相关
        rotationSpeed = (float) (riverFlowSpeed * 16.0 * (1 + tier.getIndex() * 0.5));
    }

    private void updateRotation() {
        if (rotationSpeed > 0) {
            // 每tick增加的角度 = RPM / 60 * 360 / 20 = RPM * 0.3
            rotationAngle += rotationSpeed * 0.3f;
            if (rotationAngle >= 360.0f) {
                rotationAngle -= 360.0f;
            }
        }
    }

    // ---- 护目镜信息 (Create Goggles) ----

    private List<DamSegmentState> collectSegments(Direction facing) {
        List<DamSegmentState> detected = new ArrayList<>();
        for (int segmentIndex = 0; segmentIndex < DamBlueprint.MAX_SEGMENTS; segmentIndex++) {
            BlockPos segmentControllerPos = DamBlueprint.stackControllerPos(worldPosition, facing, segmentIndex);
            boolean valid = segmentIndex == 0
                    ? matchStructure(level, segmentControllerPos, facing)
                    : matchStackedStructure(level, segmentControllerPos, facing);
            if (!valid) {
                if (segmentIndex == 0) {
                    structureIssues.add("主控段结构不匹配蓝图");
                }
                break;
            }

            detected.add(new DamSegmentState(
                    segmentIndex,
                    segmentControllerPos,
                    DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.AXIS_LOCAL, facing),
                    DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.SHAFT_LOCAL, facing),
                    DamBlueprint.toWorld(segmentControllerPos, DamBlueprint.HATCH_LOCAL, facing)
            ));
        }
        connectedDamCount = Math.max(0, detected.size() - 1);
        return detected;
    }

    private void refreshMachineState() {
        if (level == null || !formed) {
            return;
        }

        Direction facing = getBlockState().getValue(WaterDamControllerBlock.FACING);
        if (!validateSegments(facing)) {
            disassemble();
            return;
        }

        double totalFlow = 0.0;
        for (DamSegmentState segment : segments) {
            double flow = RiverCurrentSampler.sampleWheelFrontFlow(level, segment.axisPos(), facing);
            segment.setFlowSpeed(flow);
            totalFlow += flow;
        }

        riverFlowSpeed = segments.isEmpty() ? 0.0 : totalFlow / segments.size();
        stressOutput = tier.calculateTotalStress(segments.size(), riverFlowSpeed);
        rotationSpeed = (float) tier.calculateRpm(riverFlowSpeed);

        double segmentStress = tier.calculateSegmentStress(segments.size(), riverFlowSpeed);
        for (DamSegmentState segment : segments) {
            segment.setStressShare(segmentStress);
        }

        assignOutputsToHatches();
        rebuildAxisCache();
    }

    private boolean validateSegments(Direction facing) {
        structureIssues.clear();
        for (int i = 0; i < segments.size(); i++) {
            DamSegmentState segment = segments.get(i);
            boolean structureValid = i == 0
                    ? matchStructure(level, segment.controllerPos(), facing)
                    : matchStackedStructure(level, segment.controllerPos(), facing);
            if (!structureValid) {
                structureIssues.add("坝段 " + (i + 1) + " 结构与蓝图不匹配");
                return false;
            }

            if (!validateOutputChain(segment)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateOutputChain(DamSegmentState segment) {
        if (!(level.getBlockState(segment.shaftPos()).getBlock() instanceof DamShaftBlock)) {
            structureIssues.add("坝段 " + (segment.index() + 1) + " 缺少传动杆");
            segment.setHatchValid(false);
            return false;
        }

        BlockState hatchState = level.getBlockState(segment.hatchPos());
        if (!(hatchState.getBlock() instanceof StressOutputHatchBlock hatchBlock) || hatchBlock.getTier() != tier) {
            structureIssues.add("坝段 " + (segment.index() + 1) + " 的应力输出仓等级不匹配");
            segment.setHatchValid(false);
            return false;
        }

        segment.setHatchValid(true);
        return true;
    }

    private void assignOutputsToHatches() {
        if (level == null) {
            return;
        }

        for (DamSegmentState segment : segments) {
            BlockEntity be = level.getBlockEntity(segment.hatchPos());
            if (be instanceof StressOutputHatchBlockEntity hatch) {
                hatch.setOutputParameters(
                        rotationSpeed,
                        segment.stressShare(),
                        worldPosition,
                        segment.index(),
                        segments.size(),
                        segment.hatchValid()
                );
                hatch.setChanged();
            }
        }
    }

    private void unlinkStressHatches() {
        if (level == null) {
            return;
        }
        for (DamSegmentState segment : segments) {
            BlockEntity be = level.getBlockEntity(segment.hatchPos());
            if (be instanceof StressOutputHatchBlockEntity hatch) {
                hatch.unlink();
                hatch.setChanged();
            }
        }
    }

    private void assembleDamContraptions() {
        if (level == null || level.isClientSide) {
            return;
        }

        Direction facing = getBlockState().getValue(WaterDamControllerBlock.FACING);
        for (DamSegmentState segment : segments) {
            if (findContraptionEntity(segment.contraptionId()) != null) {
                segment.setAssembled(true);
                continue;
            }

            DamWheelContraption contraption = new DamWheelContraption(segment, facing);
            try {
                if (!contraption.assemble(level, segment.axisPos())) {
                    structureIssues.add("坝段 " + (segment.index() + 1) + " 没有可实体化的叶轮");
                    segment.setAssembled(false);
                    continue;
                }
            } catch (Exception exception) {
                structureIssues.add("坝段 " + (segment.index() + 1) + " 组装失败: " + exception.getClass().getSimpleName());
                segment.setAssembled(false);
                continue;
            }

            contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
            ControlledContraptionEntity entity = ControlledContraptionEntity.create(level, this, contraption);
            entity.setPos(segment.axisPos().getX(), segment.axisPos().getY(), segment.axisPos().getZ());
            entity.setRotationAxis(facing.getAxis());
            entity.setAngle(rotationAngle);
            level.addFreshEntity(entity);

            segment.setContraptionId(entity.getUUID());
            segment.setAssembled(true);
        }
    }

    private void disassembleDamContraptions() {
        if (level == null) {
            return;
        }
        for (DamSegmentState segment : segments) {
            ControlledContraptionEntity entity = findContraptionEntity(segment.contraptionId());
            if (entity != null) {
                entity.disassemble();
            }
            segment.setContraptionId(null);
            segment.setAssembled(false);
        }
    }

    private void tickContraptions() {
        updateRotation();
        if (level == null) {
            return;
        }
        Direction.Axis axis = getBlockState().getValue(WaterDamControllerBlock.FACING).getAxis();
        for (DamSegmentState segment : segments) {
            ControlledContraptionEntity entity = findContraptionEntity(segment.contraptionId());
            if (entity == null) {
                segment.setAssembled(false);
                continue;
            }
            entity.setRotationAxis(axis);
            entity.setAngle(rotationAngle);
            segment.setAssembled(true);
        }
    }

    private ControlledContraptionEntity findContraptionEntity(UUID contraptionId) {
        if (contraptionId == null || level == null) {
            return null;
        }
        AABB searchBox = new AABB(worldPosition).inflate(64);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox)) {
            if (entity instanceof ControlledContraptionEntity controlled && contraptionId.equals(controlled.getUUID())) {
                return controlled;
            }
        }
        return null;
    }

    private void rebuildAxisCache() {
        stackedDamAxes.clear();
        for (DamSegmentState segment : segments) {
            stackedDamAxes.add(segment.axisPos());
        }
        connectedDamCount = Math.max(0, segments.size() - 1);
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity entity) {
        if (!(entity instanceof ControlledContraptionEntity controlled)) {
            return false;
        }
        UUID uuid = controlled.getUUID();
        for (DamSegmentState segment : segments) {
            if (uuid.equals(segment.contraptionId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void attach(ControlledContraptionEntity entity) {
        DamSegmentState nearest = segments.stream()
                .min(Comparator.comparingDouble(segment -> segment.axisPos().distSqr(BlockPos.containing(entity.position()))))
                .orElse(null);
        if (nearest == null) {
            return;
        }
        nearest.setContraptionId(entity.getUUID());
        nearest.setAssembled(true);
        entity.setPos(nearest.axisPos().getX(), nearest.axisPos().getY(), nearest.axisPos().getZ());
        entity.setRotationAxis(getBlockState().getValue(WaterDamControllerBlock.FACING).getAxis());
        entity.setAngle(rotationAngle);
    }

    @Override
    public void onStall() {
        structureIssues.add("坝轮实体化旋转被卡住");
    }

    @Override
    public boolean isValid() {
        return !isRemoved() && formed && level != null && level.getBlockEntity(worldPosition) == this;
    }

    @Override
    public BlockPos getBlockPosition() {
        return worldPosition;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!formed) {
            tooltip.add(Component.literal("水坝未成型").withStyle(ChatFormatting.RED));
            return true;
        }

        tooltip.add(Component.literal("水坝控制器").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  等级: ").withStyle(ChatFormatting.GRAY)
                .append(tier.getDisplayComponent()));
        tooltip.add(Component.literal(String.format("  应力输出: %.0f SU", stressOutput))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(String.format("  旋转速度: %.1f RPM", rotationSpeed))
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(String.format("  河流流速: %.2f", riverFlowSpeed))
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal(String.format("  连接水坝: %d", connectedDamCount + 1))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(String.format("  Active Wheels: %d / %d", segments.stream().filter(DamSegmentState::assembled).count(), segments.size()))
                .withStyle(ChatFormatting.AQUA));
        if (!structureIssues.isEmpty()) {
            tooltip.add(Component.literal("  Structure Warnings Present").withStyle(ChatFormatting.RED));
        }

        if (isPlayerSneaking && !segments.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("  堆叠轴心:").withStyle(ChatFormatting.GRAY));
            for (BlockPos axis : stackedDamAxes) {
                tooltip.add(Component.literal(String.format("    [%d, %d, %d]", axis.getX(), axis.getY(), axis.getZ()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        return true;
    }

    // ---- NBT 持久化 ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putInt("Tier", tier.getIndex());
        tag.putInt("ConnectedDams", connectedDamCount);
        tag.putDouble("FlowSpeed", riverFlowSpeed);
        tag.putDouble("StressOutput", stressOutput);
        tag.putFloat("RotationAngle", rotationAngle);
        tag.putFloat("RotationSpeed", rotationSpeed);
        tag.put("Segments", DamSegmentState.saveAll(segments, registries));
        ListTag issueTags = new ListTag();
        for (String issue : structureIssues) {
            issueTags.add(net.minecraft.nbt.StringTag.valueOf(issue));
        }
        tag.put("StructureIssues", issueTags);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        tier = DamTier.byIndex(tag.getInt("Tier"));
        connectedDamCount = tag.getInt("ConnectedDams");
        riverFlowSpeed = tag.getDouble("FlowSpeed");
        stressOutput = tag.getDouble("StressOutput");
        rotationAngle = tag.getFloat("RotationAngle");
        rotationSpeed = tag.getFloat("RotationSpeed");
        segments.clear();
        if (tag.contains("Segments", net.minecraft.nbt.Tag.TAG_LIST)) {
            segments.addAll(DamSegmentState.loadAll(tag.getList("Segments", net.minecraft.nbt.Tag.TAG_COMPOUND)));
        }
        structureIssues.clear();
        if (tag.contains("StructureIssues", net.minecraft.nbt.Tag.TAG_LIST)) {
            for (net.minecraft.nbt.Tag issueTag : tag.getList("StructureIssues", net.minecraft.nbt.Tag.TAG_STRING)) {
                structureIssues.add(issueTag.getAsString());
            }
        }
        rebuildAxisCache();
    }

    // ---- 网络同步 ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }

    // ---- Getter ----

    public boolean isFormed() {
        return formed;
    }

    public DamTier getTier() {
        return tier;
    }

    public void setTier(DamTier tier) {
        this.tier = tier;
        if (formed) {
            refreshMachineState();
        }
        syncToClient();
    }

    public int getConnectedDamCount() {
        return connectedDamCount;
    }

    public double getRiverFlowSpeed() {
        return riverFlowSpeed;
    }

    public double getStressOutput() {
        return stressOutput;
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public List<BlockPos> getStackedDamAxes() {
        return stackedDamAxes;
    }

    public List<DamSegmentState> getSegments() {
        return segments;
    }

    public List<String> getStructureIssues() {
        return structureIssues;
    }

    public int getSegmentCount() {
        return segments.size();
    }

    public boolean hasActiveContraptions() {
        return segments.stream().anyMatch(DamSegmentState::assembled);
    }

    /**
     * 获取铁块轴心在世界中的位置。
     */
    public BlockPos getAxisPosition() {
        if (!segments.isEmpty()) {
            return segments.getFirst().axisPos();
        }
        Direction facing = getBlockState().getValue(WaterDamControllerBlock.FACING);
        return rotateOffset(worldPosition, -3, -3, 0, facing);
    }

    @Override
    public ModularUI createUI(Player player) {
        ModularUI ui = new ModularUI(228, 196, this, player);
        ui.background(
                new ResourceTexture("gtceu:textures/gui/base/background.png"),
                new ColorRectTexture(0xE0101010)
        );
        ui.widget(new LabelWidget(8, 8, () -> "GTCEu Water Dam Controller"));
        ui.widget(new LabelWidget(8, 22, () -> "Tier: " + tier.getDisplayName()));
        ui.widget(new LabelWidget(8, 34, () -> "Formed: " + formed));
        ui.widget(new LabelWidget(8, 46, () -> String.format("Segments: %d", getSegmentCount())));
        ui.widget(new LabelWidget(8, 58, () -> String.format("Flow: %.2f", riverFlowSpeed)));
        ui.widget(new LabelWidget(8, 70, () -> String.format("Total Stress: %.0f SU", stressOutput)));
        ui.widget(new LabelWidget(8, 82, () -> String.format("RPM: %.1f", rotationSpeed)));
        ui.widget(new LabelWidget(8, 94, () -> String.format("Formula: %.0f * %.2f * %.2f",
                tier.getBaseStress(), tier.getFlowFactor(riverFlowSpeed), tier.getStackFactor(getSegmentCount()))));

        int y = 110;
        for (int i = 0; i < Math.min(5, segments.size()); i++) {
            int index = i;
            ui.widget(new LabelWidget(8, y, () -> {
                DamSegmentState segment = segments.get(index);
                return String.format("S%d Axis[%d,%d,%d] Hatch=%s Wheel=%s",
                        segment.index() + 1,
                        segment.axisPos().getX(), segment.axisPos().getY(), segment.axisPos().getZ(),
                        segment.hatchValid(), segment.assembled());
            }));
            y += 12;
        }

        if (!structureIssues.isEmpty()) {
            ui.widget(new LabelWidget(8, y + 4, () -> "Warnings: " + structureIssues.getFirst()).setTextColor(0xFFFF8080));
        }
        return ui;
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    @Override
    public boolean isRemote() {
        return level != null && level.isClientSide;
    }

    @Override
    public void markAsDirty() {
        setChanged();
    }
}
