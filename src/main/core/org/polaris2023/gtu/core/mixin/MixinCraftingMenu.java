package org.polaris2023.gtu.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.polaris2023.gtu.core.init.AttachmentRegistries;
import org.polaris2023.gtu.core.init.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public class MixinCraftingMenu {
    @Inject(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static void setLevel(AbstractContainerMenu menu,
                                 Level level,
                                 Player player,
                                 CraftingContainer craftSlots,
                                 ResultContainer resultSlots,
                                 RecipeHolder<CraftingRecipe> recipe,
                                 CallbackInfo ci,
                                 @Local ItemStack itemStack) {
        if (!itemStack.is(ItemTags.STAGE)) return;//当有这个tag的物品才适用于阶段等级附加
        int data = player.getData(AttachmentRegistries.STAGE_LEVEL);
        if (data > 0) {
            itemStack.setCount(itemStack.getCount() + data * 2 / 3);
        }//当玩家存在阶段数据时，给玩家一个合成物品增加的情况

    }
}
