package org.polaris2023.gtu.core.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.polaris2023.gtu.core.init.MenuRegistries;

public class TestMultiblockMenu extends AbstractContainerMenu {
    private final Container container;

    public TestMultiblockMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new net.minecraft.world.SimpleContainer(9));
    }

    public TestMultiblockMenu(int containerId, Inventory inventory, Container container) {
        super(MenuRegistries.TEST_MULTIBLOCK_MENU.get(), containerId);
        checkContainerSize(container, 9);
        this.container = container;
        container.startOpen(inventory.player);

        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(container, slot, 8 + slot * 18, 20));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 51 + row * 18));
            }
        }

        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 109));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
