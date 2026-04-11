package org.polaris2023.gtu.modpacks.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.polaris2023.gtu.core.init.CreativeTabRegistries;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;

@EventBusSubscriber(modid = GregtechUniverseModPacks.MOD_ID)
public class CreativesEvents {
    @SubscribeEvent
    public static void event(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeTabRegistries.MAIN.getKey())) {
            {
                ItemStack stack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_axe")).getDefaultInstance();
                stack.set(DataComponents.MAX_DAMAGE, (int) (stack.getMaxDamage() * 1.2F));
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_axe"));
                event.accept(stack);
            }
            {
                ItemStack stack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_pickaxe")).getDefaultInstance();
                stack.set(DataComponents.MAX_DAMAGE, (int) (stack.getMaxDamage() * 1.2F));
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_pickaxe"));
                event.accept(stack);
            }
            {
                ItemStack stack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_hoe")).getDefaultInstance();
                stack.set(DataComponents.DAMAGE, (int) (stack.getMaxDamage() * 1.2F));
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_hoe"));
                event.accept(stack);
            }
            {
                ItemStack stack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_sword")).getDefaultInstance();
                stack.set(DataComponents.DAMAGE, (int) (stack.getMaxDamage() * 1.2F));
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_sword"));
                event.accept(stack);
            }
            {
                ItemStack stack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", "flint_shovel")).getDefaultInstance();
                stack.set(DataComponents.DAMAGE, (int) (stack.getMaxDamage() * 1.2F));
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.gtu_modpacks.flint_rope_shovel"));
                event.accept(stack);
            }

        }
    }
}
