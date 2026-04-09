package org.polaris2023.gtu.core.menu;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.polaris2023.gtu.core.init.AttachmentRegistries;
import org.polaris2023.gtu.core.init.BlockRegistries;
import org.polaris2023.gtu.core.init.MenuRegistries;
import org.polaris2023.gtu.core.init.tag.ItemTags;

import java.util.Optional;

public class FlintCraftingMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    private static final int CRAFT_DURATION = 100;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data = new SimpleContainerData(2);
    private boolean placingRecipe;
    private ItemStack pendingResult = ItemStack.EMPTY;

    public FlintCraftingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public FlintCraftingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(MenuRegistries.FLINT_CRAFTING_MENU.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.addDataSlots(this.data);
        this.addSlot(new ResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 124, 35));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 3; ++column) {
                this.addSlot(new Slot(this.craftSlots, column + row * 3, 30 + column * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int slot = 0; slot < 9; ++slot) {
            this.addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 142));
        }
    }

    @Override
    public void broadcastChanges() {
        this.access.execute((level, pos) -> this.tickCrafting());
        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (!this.placingRecipe) {
            this.access.execute((level, pos) -> this.updateCraftingResult(level, null));
        }
    }

    @Override
    public void beginPlacingRecipe() {
        this.placingRecipe = true;
    }

    @Override
    public void finishPlacingRecipe(RecipeHolder<CraftingRecipe> recipe) {
        this.placingRecipe = false;
        this.access.execute((level, pos) -> this.updateCraftingResult(level, recipe));
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents contents) {
        this.craftSlots.fillStackedContents(contents);
    }

    @Override
    public void clearCraftingContent() {
        this.craftSlots.clearContent();
        this.resultSlots.clearContent();
        this.pendingResult = ItemStack.EMPTY;
        this.data.set(DATA_PROGRESS, 0);
        this.data.set(DATA_MAX_PROGRESS, 0);
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(this.craftSlots.asCraftInput(), this.player.level());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.pendingResult = ItemStack.EMPTY;
        this.resultSlots.clearContent();
        this.data.set(DATA_PROGRESS, 0);
        this.data.set(DATA_MAX_PROGRESS, 0);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BlockRegistries.FLINT_CRAFTING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            if (index == RESULT_SLOT) {
                this.access.execute((level, pos) -> slotStack.getItem().onCraftedBy(slotStack, level, player));
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(slotStack, result);
            } else if (index >= INV_SLOT_START && index < USE_ROW_SLOT_END) {
                if (!this.moveItemStackTo(slotStack, CRAFT_SLOT_START, CRAFT_SLOT_END, false)) {
                    if (index < INV_SLOT_END) {
                        if (!this.moveItemStackTo(slotStack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
            if (index == RESULT_SLOT) {
                player.drop(slotStack, false);
            }
        }

        return result;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public int getResultSlotIndex() {
        return RESULT_SLOT;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return CRAFT_SLOT_END;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.getResultSlotIndex();
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    public boolean isCrafting() {
        return this.getMaxProgress() > 0;
    }

    public int getScaledProgress(int pixels) {
        int maxProgress = this.getMaxProgress();
        if (maxProgress <= 0 || pixels <= 0) {
            return 0;
        }

        return Math.min(pixels, this.getProgress() * pixels / maxProgress);
    }

    private void updateCraftingResult(Level level, RecipeHolder<CraftingRecipe> recipe) {
        if (level.isClientSide()) {
            return;
        }

        CraftingInput craftingInput = this.craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) this.player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftingInput, level, recipe);

        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeHolder = optional.get();
            CraftingRecipe craftingRecipe = recipeHolder.value();
            if (this.resultSlots.setRecipeUsed(level, serverPlayer, recipeHolder)) {
                ItemStack assembled = craftingRecipe.assemble(craftingInput, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = this.applyStageBonus(assembled);
                }
            }
        }

        this.pendingResult = result.copy();
        this.data.set(DATA_PROGRESS, 0);
        this.data.set(DATA_MAX_PROGRESS, result.isEmpty() ? 0 : CRAFT_DURATION);
        this.setVisibleResult(ItemStack.EMPTY);
    }

    private ItemStack applyStageBonus(ItemStack stack) {
        if (!stack.is(ItemTags.STAGE)) {
            return stack;
        }

        int stageLevel = this.player.getData(AttachmentRegistries.STAGE_LEVEL);
        if (stageLevel > 0) {
            stack.setCount(stack.getCount() + stageLevel * 2 / 3);
        }

        return stack;
    }

    private void tickCrafting() {
        if (this.pendingResult.isEmpty() || !this.resultSlots.getItem(0).isEmpty()) {
            return;
        }

        int maxProgress = this.data.get(DATA_MAX_PROGRESS);
        if (maxProgress <= 0) {
            return;
        }

        int progress = this.data.get(DATA_PROGRESS) + 1;
        this.data.set(DATA_PROGRESS, progress);
        if (progress >= maxProgress) {
            this.data.set(DATA_PROGRESS, maxProgress);
            this.setVisibleResult(this.pendingResult.copy());
        }
    }

    private void setVisibleResult(ItemStack stack) {
        this.resultSlots.setItem(0, stack);
        if (this.player instanceof ServerPlayer serverPlayer) {
            this.setRemoteSlot(0, stack);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, stack));
        }
    }
}
