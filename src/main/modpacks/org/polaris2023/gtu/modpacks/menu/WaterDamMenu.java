package org.polaris2023.gtu.modpacks.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.polaris2023.gtu.modpacks.blockentity.WaterDamControllerBlockEntity;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.init.MenuRegistries;

/**
 * 水坝控制器 GUI 菜单。
 * <p>
 * 采用 GTCEu 风格的信息展示界面，无物品槽位。
 * 通过 {@link ContainerData} 同步服务端数据到客户端。
 * </p>
 *
 * <h3>同步数据 (ContainerData, 8 int slots)</h3>
 * <ol>
 *   <li>[0] tier index (0-13)</li>
 *   <li>[1] formed (0 or 1)</li>
 *   <li>[2] connected dam count</li>
 *   <li>[3] flow speed × 100 (int representation)</li>
 *   <li>[4] stress output high bits (int)</li>
 *   <li>[5] stress output low bits (int)</li>
 *   <li>[6] rotation speed × 10 (int representation)</li>
 *   <li>[7] reserved</li>
 * </ol>
 */
public class WaterDamMenu extends AbstractContainerMenu {

    public static final int DATA_SIZE = 8;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final BlockPos controllerPos;

    // ---- 客户端构造 (从网络包) ----
    public WaterDamMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos(), ContainerLevelAccess.NULL, new SimpleContainerData(DATA_SIZE));
    }

    // ---- 标准构造 (两端可用) ----
    public WaterDamMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO, ContainerLevelAccess.NULL, new SimpleContainerData(DATA_SIZE));
    }

    // ---- 服务端构造 (带Access和Data) ----
    public WaterDamMenu(int containerId, Inventory playerInventory, BlockPos controllerPos, ContainerLevelAccess access, ContainerData data) {
        super(MenuRegistries.WATER_DAM_MENU.get(), containerId);
        this.controllerPos = controllerPos;
        this.access = access;
        this.data = data;
        this.addDataSlots(data);
    }

    /**
     * 从 BlockEntity 创建服务端菜单。
     */
    public static WaterDamMenu create(int containerId, Inventory playerInventory,
                                       WaterDamControllerBlockEntity blockEntity) {
        ContainerLevelAccess access = ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos());
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getTier().getIndex();
                    case 1 -> blockEntity.isFormed() ? 1 : 0;
                    case 2 -> blockEntity.getConnectedDamCount();
                    case 3 -> (int) (blockEntity.getRiverFlowSpeed() * 100);
                    case 4 -> (int) (((long) blockEntity.getStressOutput()) >> 32);
                    case 5 -> (int) ((long) blockEntity.getStressOutput());
                    case 6 -> (int) (blockEntity.getRotationSpeed() * 10);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 服务端只读
            }

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
        return new WaterDamMenu(containerId, playerInventory, blockEntity.getBlockPos(), access, data);
    }

    // ---- 数据访问器 (客户端使用) ----

    public int getTierIndex() {
        return data.get(0);
    }

    public boolean isFormed() {
        return data.get(1) != 0;
    }

    public int getConnectedDamCount() {
        return data.get(2);
    }

    public double getFlowSpeed() {
        return data.get(3) / 100.0;
    }

    public double getStressOutput() {
        long high = ((long) data.get(4)) << 32;
        long low = data.get(5) & 0xFFFFFFFFL;
        return (double) (high | low);
    }

    public float getRotationSpeed() {
        return data.get(6) / 10.0f;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    // ---- AbstractContainerMenu ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 没有物品槽位
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BlockRegistries.WATER_DAM_CONTROLLER.get());
    }
}
