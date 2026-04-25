package org.polaris2023.gtu.core.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.polaris2023.gtu.core.init.ItemRegistries;
import org.polaris2023.gtu.core.init.ModProperties;

public class GravelFlintModifier extends LootModifier {
    private static final float FLINT_SHARD_CHANCE = 0.2F;

    public static final MapCodec<GravelFlintModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance)
                    .apply(instance, GravelFlintModifier::new)
    );

    public GravelFlintModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                 LootContext context) {
        BlockState bs = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (bs != null && bs.hasProperty(ModProperties.PLACE) && bs.getValue(ModProperties.PLACE)) {
            return generatedLoot;
        }

        if (context.getRandom().nextFloat() < FLINT_SHARD_CHANCE) {

            generatedLoot.add(ItemRegistries.FLINT_SHARD.toStack());
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
