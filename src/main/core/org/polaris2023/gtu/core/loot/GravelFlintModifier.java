package org.polaris2023.gtu.core.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.polaris2023.gtu.core.init.ItemRegistries;

import java.util.stream.Stream;

public class GravelFlintModifier extends LootModifier {

    public static final MapCodec<GravelFlintModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance)
                    .apply(instance, GravelFlintModifier::new)
    );

    private static final float DROP_CHANGE = 0.2F;

    public GravelFlintModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                 LootContext context) {
        var flint = generatedLoot.stream().filter(stack -> stack.is(Items.FLINT)).findFirst();
        if (flint.isEmpty()) {
            generatedLoot.add(ItemRegistries.FLINT_SHARE.toStack());
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
