package org.polaris2023.gtu.core.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import org.polaris2023.gtu.core.init.ItemRegistries;
import org.polaris2023.gtu.core.init.tag.ItemTags;


public class GrassFiberModifier extends LootModifier {

    public static final MapCodec<GrassFiberModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance)
                    .apply(instance, GrassFiberModifier::new)
    );

    /**
     * 掉落概率 70%
     */
    private static final float DROP_CHANCE = 0.70f;

    public GrassFiberModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            @NotNull ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        // 检查是否使用了工具
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty()) {
            return generatedLoot;
        }

        if (!tool.is(ItemTags.GRASS_FIBER)) {
            return generatedLoot;
        }

        if (context.getRandom().nextFloat() < DROP_CHANCE) {
            generatedLoot.add(new ItemStack(ItemRegistries.PLANT_FIBER.get()));
        }

        return generatedLoot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
