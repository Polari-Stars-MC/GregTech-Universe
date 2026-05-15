package com.gregtechceu.gtceu.api.registry.registrate;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.RotationState;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.steam.SteamMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.kind.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.client.renderer.BlockEntityWithBERModelRenderer;
import com.gregtechceu.gtceu.client.renderer.ItemWithBERModelRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.datagen.model.builder.MachineModelBuilder;
import com.gregtechceu.gtceu.data.recipe.GTRecipeModifiers;
import com.gregtechceu.gtceu.data.recipe.GTRecipeTypes;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.experimental.Tolerate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.*;
import static com.gregtechceu.gtceu.data.model.GTMachineModels.*;

@SuppressWarnings("unused")
@RemapPrefixForJS("kjs$")
public class MachineBuilder<DEFINITION extends MachineDefinition> {
    protected final GTRegistrate registrate;
    protected final String name;
    protected final BiFunction<BlockBehaviour.Properties, DEFINITION, IMachineBlock> blockFactory;
    protected final BiFunction<IMachineBlock, Item.Properties, MetaMachineItem> itemFactory;
    protected final TriFunction<BlockEntityType<?>, BlockPos, BlockState, IMachineBlockEntity> blockEntityFactory;
    protected Function<ResourceLocation, DEFINITION> definition;
    protected Function<IMachineBlockEntity, MetaMachine> machine;
    @Nullable
    private MachineBuilder.ModelInitializer model = null;
    @Nullable
    private NonNullBiConsumer<DataGenContext<Block, ? extends Block>, GTBlockstateProvider> blockModel = null;
    protected final Map<Property<?>, @Nullable Comparable<?>> modelProperties = new IdentityHashMap<>();
    private VoxelShape shape = Shapes.block();
    private RotationState rotationState = RotationState.NON_Y_AXIS;
    /**
     * Whether this machine can be rotated or face upwards.
     */
    private boolean allowExtendedFacing = false;
    private boolean hasBER;
    private boolean renderMultiblockWorldPreview = true;
    private boolean renderMultiblockXEIPreview = true;
    private NonNullUnaryOperator<BlockBehaviour.Properties> blockProp = p -> p;
    private NonNullUnaryOperator<Item.Properties> itemProp = p -> p;
    @Nullable
    private Consumer<BlockBuilder<? extends Block, ?>> blockBuilder;
    @Nullable
    private Consumer<ItemBuilder<? extends MetaMachineItem, ?>> itemBuilder;
    private NonNullConsumer<BlockEntityType<BlockEntity>> onBlockEntityRegister = NonNullConsumer.noop();
    // getter for KJS
    @NotNull
    private GTRecipeType @NotNull [] recipeTypes = new GTRecipeType[0];
    // getter for KJS
    private int tier;
    private Object2IntMap<RecipeCapability<?>> recipeOutputLimits = new Object2IntOpenHashMap<>();
    private int paintingColor = ConfigHolder.INSTANCE.client.getDefaultPaintingColor();
    private BiFunction<ItemStack, Integer, Integer> itemColor = ((itemStack, tintIndex) -> tintIndex == 2 ? GTValues.VC[tier] : tintIndex == 1 ? paintingColor : -1);
    private PartAbility[] abilities = new PartAbility[0];
    private final List<Component> tooltips = new ArrayList<>();
    @Nullable
    private BiConsumer<ItemStack, List<Component>> tooltipBuilder;
    private RecipeModifier recipeModifier = new RecipeModifierList(GTRecipeModifiers.OC_NON_PERFECT);
    private boolean alwaysTryModifyRecipe;
    @NotNull
    private BiPredicate<IRecipeLogicMachine, GTRecipe> beforeWorking = (machine, recipe) -> true;
    @NotNull
    private Predicate<IRecipeLogicMachine> onWorking = machine -> true;
    @NotNull
    private Consumer<IRecipeLogicMachine> onWaiting = machine -> {
    };
    @NotNull
    private Consumer<IRecipeLogicMachine> afterWorking = machine -> {
    };
    private boolean regressWhenWaiting = true;
    private boolean allowCoverOnFront = false;
    @Nullable
    private Supplier<BlockState> appearance;
    // getter for KJS
    @Nullable
    private EditableMachineUI editableUI;
    // getter for KJS
    @Nullable
    private String langValue = null;

    public MachineBuilder(GTRegistrate registrate, String name, Function<ResourceLocation, DEFINITION> definition, Function<IMachineBlockEntity, MetaMachine> machine, BiFunction<BlockBehaviour.Properties, DEFINITION, IMachineBlock> blockFactory, BiFunction<IMachineBlock, Item.Properties, MetaMachineItem> itemFactory, TriFunction<BlockEntityType<?>, BlockPos, BlockState, IMachineBlockEntity> blockEntityFactory) {
        this.registrate = registrate;
        this.name = name;
        this.machine = machine;
        this.blockFactory = blockFactory;
        this.itemFactory = itemFactory;
        this.blockEntityFactory = blockEntityFactory;
        this.definition = definition;
    }

    public MachineBuilder<DEFINITION> recipeType(GTRecipeType type) {
        // noinspection ConstantValue
        if (type == null) {
            GTCEu.LOGGER.error("Tried to set null recipe type on machine {}. Did you create the recipe type before this machine?", this.registrate.makeResourceLocation(this.name));
            return this;
        }
        this.recipeTypes = ArrayUtils.add(this.recipeTypes, type);
        initRecipeMachineModelProperties(type);
        return this;
    }

    @Tolerate
    public MachineBuilder<DEFINITION> recipeTypes(GTRecipeType... types) {
        List<GTRecipeType> typeList = new ArrayList<>();
        Collections.addAll(typeList, this.recipeTypes);
        for (int i = 0; i < types.length; i++) {
            GTRecipeType type = types[i];
            if (type != null) {
                initRecipeMachineModelProperties(type);
                typeList.add(type);
            } else {
                GTCEu.LOGGER.error("Tried to set null recipe type on machine {} (index {}). Did you create the recipe type before this machine?", this.registrate.makeResourceLocation(this.name), i);
            }
        }
        this.recipeTypes = typeList.toArray(GTRecipeType[]::new);
        return this;
    }

    protected void initRecipeMachineModelProperties(GTRecipeType type) {
        if (type == GTRecipeTypes.DUMMY_RECIPES) {
            return;
        }
        if (!modelProperties.containsKey(RecipeLogic.STATUS_PROPERTY)) {
            modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE);
        }
    }

    public MachineBuilder<DEFINITION> simpleModel(ResourceLocation modelName) {
        return model(createBasicMachineModel(modelName));
    }

    public MachineBuilder<DEFINITION> defaultModel() {
        return simpleModel(registrate.makeResourceLocation("block/machine/template/" + name));
    }

    public MachineBuilder<DEFINITION> tieredHullModel(ResourceLocation model) {
        return model(createTieredHullMachineModel(model));
    }

    public MachineBuilder<DEFINITION> overlayTieredHullModel(String name) {
        return overlayTieredHullModel(registrate.makeResourceLocation("block/machine/part/" + name));
    }

    public MachineBuilder<DEFINITION> overlayTieredHullModel(ResourceLocation overlayModel) {
        return model(createOverlayTieredHullMachineModel(overlayModel));
    }

    public MachineBuilder<DEFINITION> colorOverlayTieredHullModel(String overlay) {
        return colorOverlayTieredHullModel(overlay, null, null);
    }

    public MachineBuilder<DEFINITION> colorOverlayTieredHullModel(String overlay, @Nullable String pipeOverlay, @Nullable String emissiveOverlay) {
        ResourceLocation overlayTex = registrate.makeResourceLocation("block/overlay/machine/" + overlay);
        ResourceLocation pipeOverlayTex = pipeOverlay == null ? null : registrate.makeResourceLocation("block/overlay/machine/" + pipeOverlay);
        ResourceLocation emissiveOverlayTex = emissiveOverlay == null ? null : registrate.makeResourceLocation("block/overlay/machine/" + emissiveOverlay);
        return colorOverlayTieredHullModel(overlayTex, pipeOverlayTex, emissiveOverlayTex);
    }

    public MachineBuilder<DEFINITION> colorOverlayTieredHullModel(ResourceLocation overlay) {
        return colorOverlayTieredHullModel(overlay, null, null);
    }

    public MachineBuilder<DEFINITION> colorOverlayTieredHullModel(ResourceLocation overlay, @Nullable ResourceLocation pipeOverlay, @Nullable ResourceLocation emissiveOverlay) {
        modelProperty(IPaintable.IS_PAINTED_PROPERTY, false);
        return model(createColorOverlayTieredHullMachineModel(overlay, pipeOverlay, emissiveOverlay));
    }

    public MachineBuilder<DEFINITION> overlaySteamHullModel(String name) {
        return overlaySteamHullModel(registrate.makeResourceLocation("block/machine/part/" + name));
    }

    public MachineBuilder<DEFINITION> overlaySteamHullModel(ResourceLocation overlayModel) {
        modelProperty(SteamMachine.STEEL_PROPERTY, ConfigHolder.INSTANCE.machines.steelSteamMultiblocks);
        return model(createOverlaySteamHullMachineModel(overlayModel));
    }

    public MachineBuilder<DEFINITION> colorOverlaySteamHullModel(String overlay) {
        return colorOverlaySteamHullModel(overlay, null, null);
    }

    public MachineBuilder<DEFINITION> colorOverlaySteamHullModel(String overlay, @Nullable ResourceLocation pipeOverlay, @Nullable String emissiveOverlay) {
        ResourceLocation overlayTex = registrate.makeResourceLocation("block/overlay/machine/" + overlay);
        ResourceLocation pipeOverlayTex = pipeOverlay == null ? null : registrate.makeResourceLocation("block/overlay/machine/" + pipeOverlay);
        ResourceLocation emissiveOverlayTex = emissiveOverlay == null ? null : registrate.makeResourceLocation("block/overlay/machine/" + emissiveOverlay);
        return colorOverlaySteamHullModel(overlayTex, pipeOverlayTex, emissiveOverlayTex);
    }

    public MachineBuilder<DEFINITION> colorOverlaySteamHullModel(ResourceLocation overlay) {
        return colorOverlaySteamHullModel(overlay, null, null);
    }

    public MachineBuilder<DEFINITION> colorOverlaySteamHullModel(ResourceLocation overlay, @Nullable ResourceLocation pipeOverlay, @Nullable ResourceLocation emissiveOverlay) {
        modelProperty(IPaintable.IS_PAINTED_PROPERTY, false);
        return model(createColorOverlaySteamHullMachineModel(overlay, pipeOverlay, emissiveOverlay));
    }

    public MachineBuilder<DEFINITION> workableTieredHullModel(ResourceLocation workableModel) {
        modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE);
        return model(createWorkableTieredHullMachineModel(workableModel));
    }

    public MachineBuilder<DEFINITION> simpleGeneratorModel(ResourceLocation workableModel) {
        modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE);
        return model(createSimpleGeneratorModel(workableModel));
    }

    public MachineBuilder<DEFINITION> workableSteamHullModel(boolean isHighPressure, ResourceLocation workableModel) {
        modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE);
        return model(createWorkableSteamHullMachineModel(isHighPressure, workableModel));
    }

    public MachineBuilder<DEFINITION> workableCasingModel(ResourceLocation baseCasing, ResourceLocation workableModel) {
        modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE);
        return model(createWorkableCasingMachineModel(baseCasing, workableModel));
    }

    public MachineBuilder<DEFINITION> sidedOverlayCasingModel(ResourceLocation baseCasing, ResourceLocation workableModel) {
        return model(createSidedOverlayCasingMachineModel(baseCasing, workableModel));
    }

    public MachineBuilder<DEFINITION> sidedWorkableCasingModel(ResourceLocation baseCasing, ResourceLocation workableModel) {
        modelProperty(RecipeLogic.STATUS_PROPERTY, RecipeLogic.Status.IDLE);
        return model(createSidedWorkableCasingMachineModel(baseCasing, workableModel));
    }

    public MachineBuilder<DEFINITION> appearanceBlock(Supplier<? extends Block> block) {
        appearance = () -> block.get().defaultBlockState();
        return this;
    }

    public MachineBuilder<DEFINITION> tooltips(@Nullable Component... components) {
        return tooltips(Arrays.asList(components));
    }

    public MachineBuilder<DEFINITION> tooltips(List<? extends @Nullable Component> components) {
        tooltips.addAll(components.stream().filter(Objects::nonNull).toList());
        return this;
    }

    public MachineBuilder<DEFINITION> conditionalTooltip(Component component, BooleanSupplier condition) {
        return conditionalTooltip(component, condition.getAsBoolean());
    }

    public MachineBuilder<DEFINITION> conditionalTooltip(Component component, boolean condition) {
        if (condition) tooltips.add(component);
        return this;
    }

    public MachineBuilder<DEFINITION> abilities(PartAbility... abilities) {
        this.abilities = abilities;
        return this;
    }

    public MachineBuilder<DEFINITION> modelProperty(Property<?> property) {
        return modelProperty(property, null);
    }

    public <T extends Comparable<T>> MachineBuilder<DEFINITION> modelProperty(Property<T> property, @Nullable T defaultValue) {
        this.modelProperties.put(property, defaultValue);
        return this;
    }

    // KJS helpers for model property defaults
    // These don't need to be copied to the multiblock builder because KJS doesn't care about the return type downgrade
    public MachineBuilder<DEFINITION> kjs$modelPropertyBool(Property<Boolean> property, boolean defaultValue) {
        return modelProperty(property, defaultValue);
    }

    public MachineBuilder<DEFINITION> kjs$modelPropertyInt(Property<Integer> property, int defaultValue) {
        return modelProperty(property, defaultValue);
    }

    public <T extends Enum<T> & Comparable<T>> MachineBuilder<DEFINITION> kjs$modelPropertyEnum(Property<T> property, T defaultValue) {
        return modelProperty(property, defaultValue);
    }

    @Tolerate
    public MachineBuilder<DEFINITION> modelProperties(Property<?>... properties) {
        return this.modelProperties(List.of(properties));
    }

    @Tolerate
    public MachineBuilder<DEFINITION> modelProperties(Collection<Property<?>> properties) {
        for (Property<?> prop : properties) {
            this.modelProperties.put(prop, null);
        }
        return this;
    }

    @Tolerate
    public MachineBuilder<DEFINITION> modelProperties(Map<Property<?>, ? extends Comparable<?>> properties) {
        this.modelProperties.putAll(properties);
        return this;
    }

    public MachineBuilder<DEFINITION> removeModelProperty(Property<?> property) {
        this.modelProperties.remove(property);
        return this;
    }

    public MachineBuilder<DEFINITION> clearModelProperties() {
        this.modelProperties.clear();
        return this;
    }

    public MachineBuilder<DEFINITION> recipeModifier(RecipeModifier recipeModifier) {
        this.recipeModifier = recipeModifier instanceof RecipeModifierList list ? list : new RecipeModifierList(recipeModifier);
        return this;
    }

    public MachineBuilder<DEFINITION> recipeModifier(RecipeModifier recipeModifier, boolean alwaysTryModifyRecipe) {
        this.alwaysTryModifyRecipe = alwaysTryModifyRecipe;
        return this.recipeModifier(recipeModifier);
    }

    public MachineBuilder<DEFINITION> recipeModifiers(RecipeModifier... recipeModifiers) {
        this.recipeModifier = new RecipeModifierList(recipeModifiers);
        return this;
    }

    public MachineBuilder<DEFINITION> recipeModifiers(boolean alwaysTryModifyRecipe, RecipeModifier... recipeModifiers) {
        return this.recipeModifier(new RecipeModifierList(recipeModifiers), alwaysTryModifyRecipe);
    }

    public MachineBuilder<DEFINITION> noRecipeModifier() {
        this.recipeModifier = new RecipeModifierList(RecipeModifier.NO_MODIFIER);
        this.alwaysTryModifyRecipe = false;
        return this;
    }

    public MachineBuilder<DEFINITION> addOutputLimit(RecipeCapability<?> capability, int limit) {
        this.recipeOutputLimits.put(capability, limit);
        return this;
    }

    public MachineBuilder<DEFINITION> multiblockPreviewRenderer(boolean multiBlockWorldPreview, boolean multiBlockXEIPreview) {
        this.renderMultiblockWorldPreview = multiBlockWorldPreview;
        this.renderMultiblockXEIPreview = multiBlockXEIPreview;
        return this;
    }

    protected DEFINITION createDefinition() {
        return definition.apply(registrate.makeResourceLocation(name));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void setupStateDefinition(MachineDefinition definition) {
        StateDefinition.Builder<MachineDefinition, MachineRenderState> builder = new StateDefinition.Builder<>(definition);
        this.modelProperties.keySet().forEach(builder::add);
        definition.setStateDefinition(builder.create(MachineDefinition::defaultRenderState, MachineRenderState::new));
        MachineRenderState defaultState = definition.getStateDefinition().any();
        for (var entry : this.modelProperties.entrySet()) {
            if (entry.getValue() == null) continue;
            defaultState = defaultState.setValue((Property) entry.getKey(), (Comparable) entry.getValue());
        }
        definition.registerDefaultState(defaultState);
    }

    @HideFromJS
    @NotNull
    public DEFINITION register() {
        this.registrate.object(name);
        var definition = createDefinition();
        definition.setRotationState(rotationState);
        setupStateDefinition(definition);
        if (model == null && blockModel == null) {
            simpleModel(registrate.makeResourceLocation("block/machine/template/" + name));
        }
        var blockBuilder = BlockBuilderWrapper.makeBlockBuilder(this, definition);
        if (this.langValue != null) {
            blockBuilder.lang(langValue);
            definition.setLangValue(langValue);
        }
        if (this.blockBuilder != null) {
            this.blockBuilder.accept(blockBuilder);
        }
        var block = blockBuilder.register();
        var itemBuilder = ItemBuilderWrapper.makeItemBuilder(this, block);
        if (this.itemBuilder != null) {
            this.itemBuilder.accept(itemBuilder);
        }
        var item = itemBuilder.register();
        var blockEntityBuilder = registrate.blockEntity((type, pos, state) -> blockEntityFactory.apply(type, pos, state).self()).onRegister(onBlockEntityRegister).validBlock(block);
        if (hasBER) {
            blockEntityBuilder = blockEntityBuilder.renderer(() -> BlockEntityWithBERModelRenderer::new);
        }
        var blockEntity = blockEntityBuilder.register();
        definition.setRecipeTypes(recipeTypes);
        definition.setBlockSupplier(block);
        definition.setItemSupplier(item);
        definition.setTier(tier);
        definition.setRecipeOutputLimits(recipeOutputLimits);
        definition.setBlockEntityTypeSupplier(blockEntity::get);
        definition.setMachineSupplier(machine);
        definition.setTooltipBuilder((itemStack, components) -> {
            components.addAll(tooltips);
            if (tooltipBuilder != null) tooltipBuilder.accept(itemStack, components);
        });
        definition.setRecipeModifier(recipeModifier);
        definition.setAlwaysTryModifyRecipe(alwaysTryModifyRecipe);
        definition.setBeforeWorking(this.beforeWorking);
        definition.setOnWorking(this.onWorking);
        definition.setOnWaiting(this.onWaiting);
        definition.setAfterWorking(this.afterWorking);
        definition.setRegressWhenWaiting(this.regressWhenWaiting);
        definition.setAllowCoverOnFront(this.allowCoverOnFront);
        for (GTRecipeType type : recipeTypes) {
            if (type.getIconSupplier() == null) {
                type.setIconSupplier(definition::asStack);
            }
        }
        if (appearance == null) {
            appearance = block::getDefaultState;
        }
        if (editableUI != null) {
            definition.setEditableUI(editableUI);
        }
        definition.setAppearance(appearance);
        definition.setAllowExtendedFacing(allowExtendedFacing);
        definition.setShape(shape);
        definition.setDefaultPaintingColor(paintingColor);
        definition.setRenderXEIPreview(renderMultiblockXEIPreview);
        definition.setRenderWorldPreview(renderMultiblockWorldPreview);
        GTRegistries.register(GTRegistries.MACHINES, definition.getId(), definition);
        return definition;
    }


    @FunctionalInterface
    public interface ModelInitializer {
        void configureModel(@NotNull DataGenContext<Block, ? extends Block> context, @NotNull GTBlockstateProvider provider, @NotNull MachineModelBuilder<BlockModelBuilder> builder);

        default ModelInitializer andThen(ModelInitializer after) {
            Objects.requireNonNull(after);
            return (ctx, prov, builder) -> {
                this.configureModel(ctx, prov, builder);
                after.configureModel(ctx, prov, builder);
            };
        }

        default ModelInitializer andThen(Consumer<MachineModelBuilder<BlockModelBuilder>> after) {
            Objects.requireNonNull(after);
            return (ctx, prov, builder) -> {
                this.configureModel(ctx, prov, builder);
                after.accept(builder);
            };
        }

        default ModelInitializer compose(ModelInitializer before) {
            Objects.requireNonNull(before);
            return (ctx, prov, builder) -> {
                before.configureModel(ctx, prov, builder);
                this.configureModel(ctx, prov, builder);
            };
        }

        default ModelInitializer compose(UnaryOperator<MachineModelBuilder<BlockModelBuilder>> before) {
            Objects.requireNonNull(before);
            return (ctx, prov, builder) -> {
                this.configureModel(ctx, prov, before.apply(builder));
            };
        }
    }

    // spotless:off
    protected static class BlockBuilderWrapper {
        public static <DEFINITION extends MachineDefinition> BlockBuilder<Block, ? extends AbstractRegistrate<?>> makeBlockBuilder(MachineBuilder<DEFINITION> builder, DEFINITION definition) {
            return builder.registrate.block(properties -> makeBlock(builder, definition, properties)).color(() -> () -> IMachineBlock::colorTinted).initialProperties(() -> Blocks.DISPENSER).properties(BlockBehaviour.Properties::noLootTable).addLayer(() -> RenderType::cutout).exBlockstate(builder.blockModel != null ? builder.blockModel : createMachineModel(builder.model)).properties(builder.blockProp).onRegister(b -> Arrays.stream(builder.abilities).forEach(a -> a.register(builder.tier, b)));
        }

        private static <DEFINITION extends MachineDefinition> Block makeBlock(MachineBuilder<DEFINITION> builder, DEFINITION definition, BlockBehaviour.Properties properties) {
            MachineDefinition.setBuilt(definition);
            var b = builder.blockFactory.apply(properties, definition);
            MachineDefinition.clearBuilt();
            return b.self();
        }
    }


    protected static class ItemBuilderWrapper {
        public static <DEFINITION extends MachineDefinition> ItemBuilder<MetaMachineItem, ? extends AbstractRegistrate<?>> makeItemBuilder(MachineBuilder<DEFINITION> builder, BlockEntry<Block> block) {
            return  // do not gen any lang keys
            // copied from BlockBuilder#item
            builder.registrate.item(properties -> builder.itemFactory.apply((IMachineBlock) block.get(), properties)).setData(ProviderType.LANG, NonNullBiConsumer.noop()).model((ctx, prov) -> {
                prov.withExistingParent(ctx.getName(), ResourceLocation.fromNamespaceAndPath(builder.registrate.getModid(), "block/machine/" + ctx.getName()));
            }).clientExtension(() -> () -> new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return ItemWithBERModelRenderer.INSTANCE;
                }
            }).color(() -> () -> builder.itemColor::apply).properties(builder.itemProp);
        }
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> definition(final Function<ResourceLocation, DEFINITION> definition) {
        this.definition = definition;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> machine(final Function<IMachineBlockEntity, MetaMachine> machine) {
        this.machine = machine;
        return this;
    }

    @Nullable
    public MachineBuilder.ModelInitializer model() {
        return this.model;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> model(@Nullable final MachineBuilder.ModelInitializer model) {
        this.model = model;
        return this;
    }

    @Nullable
    public NonNullBiConsumer<DataGenContext<Block, ? extends Block>, GTBlockstateProvider> blockModel() {
        return this.blockModel;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> blockModel(@Nullable final NonNullBiConsumer<DataGenContext<Block, ? extends Block>, GTBlockstateProvider> blockModel) {
        this.blockModel = blockModel;
        return this;
    }

    public Map<Property<?>, @Nullable Comparable<?>> modelProperties() {
        return this.modelProperties;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> shape(final VoxelShape shape) {
        this.shape = shape;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> rotationState(final RotationState rotationState) {
        this.rotationState = rotationState;
        return this;
    }

    /**
     * Whether this machine can be rotated or face upwards.
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> allowExtendedFacing(final boolean allowExtendedFacing) {
        this.allowExtendedFacing = allowExtendedFacing;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> hasBER(final boolean hasBER) {
        this.hasBER = hasBER;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> renderMultiblockWorldPreview(final boolean renderMultiblockWorldPreview) {
        this.renderMultiblockWorldPreview = renderMultiblockWorldPreview;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> renderMultiblockXEIPreview(final boolean renderMultiblockXEIPreview) {
        this.renderMultiblockXEIPreview = renderMultiblockXEIPreview;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> blockProp(final NonNullUnaryOperator<BlockBehaviour.Properties> blockProp) {
        this.blockProp = blockProp;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> itemProp(final NonNullUnaryOperator<Item.Properties> itemProp) {
        this.itemProp = itemProp;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> blockBuilder(@Nullable final Consumer<BlockBuilder<? extends Block, ?>> blockBuilder) {
        this.blockBuilder = blockBuilder;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> itemBuilder(@Nullable final Consumer<ItemBuilder<? extends MetaMachineItem, ?>> itemBuilder) {
        this.itemBuilder = itemBuilder;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> onBlockEntityRegister(final NonNullConsumer<BlockEntityType<BlockEntity>> onBlockEntityRegister) {
        this.onBlockEntityRegister = onBlockEntityRegister;
        return this;
    }

    @NotNull
    public GTRecipeType @NotNull [] recipeTypes() {
        return this.recipeTypes;
    }

    public int tier() {
        return this.tier;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> tier(final int tier) {
        this.tier = tier;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> recipeOutputLimits(final Object2IntMap<RecipeCapability<?>> recipeOutputLimits) {
        this.recipeOutputLimits = recipeOutputLimits;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> paintingColor(final int paintingColor) {
        this.paintingColor = paintingColor;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> itemColor(final BiFunction<ItemStack, Integer, Integer> itemColor) {
        this.itemColor = itemColor;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> tooltipBuilder(@Nullable final BiConsumer<ItemStack, List<Component>> tooltipBuilder) {
        this.tooltipBuilder = tooltipBuilder;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> alwaysTryModifyRecipe(final boolean alwaysTryModifyRecipe) {
        this.alwaysTryModifyRecipe = alwaysTryModifyRecipe;
        return this;
    }

    @NotNull
    public BiPredicate<IRecipeLogicMachine, GTRecipe> beforeWorking() {
        return this.beforeWorking;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> beforeWorking(@NotNull final BiPredicate<IRecipeLogicMachine, GTRecipe> beforeWorking) {
        if (beforeWorking == null) {
            throw new NullPointerException("beforeWorking is marked non-null but is null");
        }
        this.beforeWorking = beforeWorking;
        return this;
    }

    @NotNull
    public Predicate<IRecipeLogicMachine> onWorking() {
        return this.onWorking;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> onWorking(@NotNull final Predicate<IRecipeLogicMachine> onWorking) {
        if (onWorking == null) {
            throw new NullPointerException("onWorking is marked non-null but is null");
        }
        this.onWorking = onWorking;
        return this;
    }

    @NotNull
    public Consumer<IRecipeLogicMachine> onWaiting() {
        return this.onWaiting;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> onWaiting(@NotNull final Consumer<IRecipeLogicMachine> onWaiting) {
        if (onWaiting == null) {
            throw new NullPointerException("onWaiting is marked non-null but is null");
        }
        this.onWaiting = onWaiting;
        return this;
    }

    @NotNull
    public Consumer<IRecipeLogicMachine> afterWorking() {
        return this.afterWorking;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> afterWorking(@NotNull final Consumer<IRecipeLogicMachine> afterWorking) {
        if (afterWorking == null) {
            throw new NullPointerException("afterWorking is marked non-null but is null");
        }
        this.afterWorking = afterWorking;
        return this;
    }

    public boolean regressWhenWaiting() {
        return this.regressWhenWaiting;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> regressWhenWaiting(final boolean regressWhenWaiting) {
        this.regressWhenWaiting = regressWhenWaiting;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> allowCoverOnFront(final boolean allowCoverOnFront) {
        this.allowCoverOnFront = allowCoverOnFront;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> appearance(@Nullable final Supplier<BlockState> appearance) {
        this.appearance = appearance;
        return this;
    }

    @Nullable
    public EditableMachineUI editableUI() {
        return this.editableUI;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> editableUI(@Nullable final EditableMachineUI editableUI) {
        this.editableUI = editableUI;
        return this;
    }

    @Nullable
    public String langValue() {
        return this.langValue;
    }

    /**
     * @return {@code this}.
     */
    @org.jetbrains.annotations.NotNull
    public MachineBuilder<DEFINITION> langValue(@Nullable final String langValue) {
        this.langValue = langValue;
        return this;
    }
    // spotless:on
}
