package org.polaris2023.gtu.core.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.polaris2023.gtu.core.menu.FlintCraftingMenu;

@OnlyIn(Dist.CLIENT)
public class FlintCraftingScreen extends AbstractContainerScreen<FlintCraftingMenu> implements RecipeUpdateListener {
    private static final ResourceLocation CRAFTING_TABLE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final int PROGRESS_X = 90;
    private static final int PROGRESS_Y = 36;
    private static final int PROGRESS_WIDTH = 22;
    private static final int PROGRESS_HEIGHT = 14;
    private static final int PROGRESS_BG_COLOR = 0xFF4B4B4B;
    private static final int PROGRESS_FILL_COLOR = 0xFF8B8B8B;
    private static final int PROGRESS_BORDER_COLOR = 0xFF1F1F1F;

    private final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
    private boolean widthTooNarrow;

    public FlintCraftingScreen(FlintCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, (RecipeBookMenu<?, ?>) this.menu);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        this.addRenderableWidget(new ImageButton(this.leftPos + 5, this.height / 2 - 49, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            button.setPosition(this.leftPos + 5, this.height / 2 - 49);
        }));
        this.addWidget(this.recipeBookComponent);
        this.titleLabelX = 29;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.recipeBookComponent.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookComponent.renderGhostRecipe(guiGraphics, this.leftPos, this.topPos, true, partialTick);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.recipeBookComponent.renderTooltip(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CRAFTING_TABLE_LOCATION, left, top, 0, 0, this.imageWidth, this.imageHeight);

        int barX = left + PROGRESS_X;
        int barY = top + PROGRESS_Y;
        guiGraphics.fill(barX - 1, barY - 1, barX + PROGRESS_WIDTH + 1, barY + PROGRESS_HEIGHT + 1, PROGRESS_BORDER_COLOR);
        guiGraphics.fill(barX, barY, barX + PROGRESS_WIDTH, barY + PROGRESS_HEIGHT, PROGRESS_BG_COLOR);

        int progressWidth = this.menu.getScaledProgress(PROGRESS_WIDTH);
        if (progressWidth > 0) {
            guiGraphics.fill(barX, barY, barX + progressWidth, barY + PROGRESS_HEIGHT, PROGRESS_FILL_COLOR);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        int barX = this.leftPos + PROGRESS_X;
        int barY = this.topPos + PROGRESS_Y;
        if (this.menu.isCrafting() && x >= barX && x < barX + PROGRESS_WIDTH && y >= barY && y < barY + PROGRESS_HEIGHT) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("gui.gtu_core.flint_crafting.progress", this.menu.getProgress(), this.menu.getMaxProgress()),
                    x,
                    y
            );
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.recipeBookComponent.keyPressed(keyCode, scanCode, modifiers)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.recipeBookComponent.charTyped(codePoint, modifiers)
                || super.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible())
                && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.recipeBookComponent.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.recipeBookComponent);
            return true;
        }

        return this.widthTooNarrow && this.recipeBookComponent.isVisible()
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos, int button) {
        boolean outside = mouseX < leftPos || mouseY < topPos || mouseX >= leftPos + this.imageWidth || mouseY >= topPos + this.imageHeight;
        return this.recipeBookComponent.hasClickedOutside(mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, button)
                && outside;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        super.slotClicked(slot, slotId, mouseButton, clickType);
        this.recipeBookComponent.slotClicked(slot);
    }

    @Override
    public void recipesUpdated() {
        this.recipeBookComponent.recipesUpdated();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return this.recipeBookComponent;
    }
}
