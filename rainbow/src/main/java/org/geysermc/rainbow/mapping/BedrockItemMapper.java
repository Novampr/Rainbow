package org.geysermc.rainbow.mapping;

import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.renderer.item.properties.numeric.BundleFullness;
import net.minecraft.client.renderer.item.properties.numeric.Count;
import net.minecraft.client.renderer.item.properties.numeric.Damage;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ArrayUtils;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.BedrockAnimationContext;
import org.geysermc.rainbow.mapping.attachable.AttachableMapper;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.geometry.GeometryMapper;
import org.geysermc.rainbow.definition.GeyserBaseDefinition;
import org.geysermc.rainbow.definition.GeyserItemDefinition;
import org.geysermc.rainbow.definition.GeyserLegacyDefinition;
import org.geysermc.rainbow.definition.GeyserSingleDefinition;
import org.geysermc.rainbow.definition.predicate.GeyserConditionPredicate;
import org.geysermc.rainbow.definition.predicate.GeyserMatchPredicate;
import org.geysermc.rainbow.definition.predicate.GeyserPredicate;
import org.geysermc.rainbow.definition.predicate.GeyserRangeDispatchPredicate;
import org.geysermc.rainbow.mixin.LateBoundIdMapperAccessor;
import org.geysermc.rainbow.mixin.RangeSelectItemModelAccessor;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;
import org.geysermc.rainbow.pack.BedrockItem;
import org.geysermc.rainbow.pack.BedrockTextures;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class BedrockItemMapper {
    private static final List<ResourceLocation> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(ResourceLocation::withDefaultNamespace)
            .toList();
    private static final List<ResourceLocation> TRIMMABLE_ARMOR_TAGS = Stream.of("is_armor", "trimmable_armors")
            .map(ResourceLocation::withDefaultNamespace)
            .toList();

    private static ResourceLocation getModelId(ItemModel.Unbaked model) {
        //noinspection unchecked
        return ((LateBoundIdMapperAccessor<ResourceLocation, ?>) ItemModels.ID_MAPPER).getIdToValue().inverse().get(model.type());
    }

    public static void tryMapStack(ItemStack stack, ResourceLocation modelLocation, ProblemReporter reporter, PackContext context) {
        context.assetResolver().getClientItem(modelLocation).map(ClientItem::model)
                .ifPresentOrElse(model -> mapItem(model, stack, reporter.forChild(() -> "client item definition " + modelLocation + " "), base -> new GeyserSingleDefinition(base, Optional.of(modelLocation)), context),
                        () -> reporter.report(() -> "missing client item definition " + modelLocation));
    }

    public static void tryMapStack(ItemStack stack, int customModelData, ProblemReporter reporter, PackContext context) {
        // TODO Improve this, use resouce log in problemreporter
        ItemModel.Unbaked vanillaModel = context.assetResolver().getClientItem(stack.get(DataComponents.ITEM_MODEL)).map(ClientItem::model).orElseThrow();
        ProblemReporter childReporter = reporter.forChild(() -> "item model " + vanillaModel + " with custom model data " + customModelData + " ");
        if (vanillaModel instanceof RangeSelectItemModel.Unbaked(RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries, Optional<ItemModel.Unbaked> fallback)) {
            // WHY, Mojang?
            if (property instanceof net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty(int index)) {
                if (index == 0) {
                    float scaledCustomModelData = customModelData * scale;

                    float[] thresholds = ArrayUtils.toPrimitive(entries.stream()
                            .map(RangeSelectItemModel.Entry::threshold)
                            .toArray(Float[]::new));
                    int modelIndex = RangeSelectItemModelAccessor.invokeLastIndexLessOrEqual(thresholds, scaledCustomModelData);
                    Optional<ItemModel.Unbaked> model = modelIndex == -1 ? fallback : Optional.of(entries.get(modelIndex).model());
                    model.ifPresentOrElse(present -> mapItem(present, stack, childReporter, base -> new GeyserLegacyDefinition(base, customModelData), context),
                            () -> childReporter.report(() -> "custom model data index lookup returned -1, and no fallback is present"));
                } else {
                    childReporter.report(() -> "range_dispatch custom model data property index is not zero, unable to apply custom model data");
                }
                return;
            }
        }
        childReporter.report(() -> "item model is not range_dispatch, unable to apply custom model data");
    }

    public static void mapItem(ItemModel.Unbaked model, ItemStack stack, ProblemReporter reporter,
                               Function<GeyserBaseDefinition, GeyserItemDefinition> definitionCreator, PackContext packContext) {
        mapItem(model, new MappingContext(List.of(), stack, reporter, definitionCreator, packContext));
    }

    private static void mapItem(ItemModel.Unbaked model, MappingContext context) {
        switch (model) {
            case BlockModelWrapper.Unbaked modelWrapper -> mapBlockModelWrapper(modelWrapper, context.child("plain model " + modelWrapper.model()));
            case ConditionalItemModel.Unbaked conditional -> mapConditionalModel(conditional, context.child("condition model "));
            case RangeSelectItemModel.Unbaked rangeSelect -> mapRangeSelectModel(rangeSelect, context.child("range select model "));
            case SelectItemModel.Unbaked select -> mapSelectModel(select, context.child("select model "));
            default -> context.reporter.report(() -> "unsupported item model " + getModelId(model));
        }
    }

    private static void mapBlockModelWrapper(BlockModelWrapper.Unbaked model, MappingContext context) {
        ResourceLocation itemModelLocation = model.model();

        context.packContext().assetResolver().getResolvedModel(itemModelLocation)
                .ifPresentOrElse(itemModel -> {
                    ResolvedModel parentModel = itemModel.parent();
                    // debugName() returns the resource location of the model as a string
                    boolean handheld = parentModel != null && HANDHELD_MODELS.contains(ResourceLocation.parse(parentModel.debugName()));

                    ResourceLocation bedrockIdentifier;
                    if (itemModelLocation.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                        bedrockIdentifier = ResourceLocation.fromNamespaceAndPath("geyser_mc", itemModelLocation.getPath());
                    } else {
                        bedrockIdentifier = itemModelLocation;
                    }

                    Material layer0Texture = itemModel.getTopTextureSlots().getMaterial("layer0");
                    Optional<ResourceLocation> texture;
                    Optional<ResolvedModel> customGeometry;
                    if (layer0Texture != null) {
                        texture = Optional.of(layer0Texture.texture());
                        customGeometry = Optional.empty();
                    } else {
                        // We can't stitch multiple textures together yet, so we just grab the first one we see
                        // This will only work properly for models with just one texture
                        texture = ((TextureSlotsAccessor) itemModel.getTopTextureSlots()).getResolvedValues().values().stream()
                                .map(Material::texture)
                                .findAny();
                        // Unknown texture (doesn't use layer0), so we immediately assume the geometry is custom
                        // This check should probably be done differently
                        customGeometry = Optional.of(itemModel);
                    }

                    texture.ifPresentOrElse(itemTexture -> {
                        // Not a problem, but just report to get the model printed in the report file
                        context.reporter.report(() -> "creating mapping for block model " + itemModelLocation);
                        context.create(bedrockIdentifier, itemTexture, handheld, customGeometry);
                    }, () -> context.reporter.report(() -> "not mapping block model " + itemModelLocation + " because it has no texture"));
                }, () -> context.reporter.report(() -> "missing block model " + itemModelLocation));
    }

    private static void mapConditionalModel(ConditionalItemModel.Unbaked model, MappingContext context) {
        ItemModelPropertyTest property = model.property();
        GeyserConditionPredicate.Property predicateProperty = switch (property) {
            case Broken ignored -> GeyserConditionPredicate.BROKEN;
            case Damaged ignored -> GeyserConditionPredicate.DAMAGED;
            case CustomModelDataProperty customModelData -> new GeyserConditionPredicate.CustomModelData(customModelData.index());
            case HasComponent hasComponent -> new GeyserConditionPredicate.HasComponent(hasComponent.componentType()); // ignoreDefault property not a thing, we should look into that in Geyser! TODO
            case FishingRodCast ignored -> GeyserConditionPredicate.FISHING_ROD_CAST;
            default -> null;
        };
        ItemModel.Unbaked onTrue = model.onTrue();
        ItemModel.Unbaked onFalse = model.onFalse();

        if (predicateProperty == null) {
            context.reporter.report(() -> "unsupported conditional model property " + property + ", only mapping on_false");
            mapItem(onFalse, context.child("condition on_false (unsupported property)"));
            return;
        }

        mapItem(onTrue, context.with(new GeyserConditionPredicate(predicateProperty, true), "condition on true "));
        mapItem(onFalse, context.with(new GeyserConditionPredicate(predicateProperty, false), "condition on false "));
    }

    private static void mapRangeSelectModel(RangeSelectItemModel.Unbaked model, MappingContext context) {
        RangeSelectItemModelProperty property = model.property();
        GeyserRangeDispatchPredicate.Property predicateProperty = switch (property) {
            case BundleFullness ignored -> GeyserRangeDispatchPredicate.BUNDLE_FULLNESS;
            case Count count -> new GeyserRangeDispatchPredicate.Count(count.normalize());
            // Mojang, why? :(
            case net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty customModelData -> new GeyserRangeDispatchPredicate.CustomModelData(customModelData.index());
            case Damage damage -> new GeyserRangeDispatchPredicate.Damage(damage.normalize());
            default -> null;
        };

        if (predicateProperty == null) {
            context.reporter.report(() -> "unsupported range dispatch model property " + property + ", only mapping fallback, if it is present");
        } else {
            for (RangeSelectItemModel.Entry entry : model.entries()) {
                mapItem(entry.model(), context.with(new GeyserRangeDispatchPredicate(predicateProperty, entry.threshold(), model.scale()), "threshold " + entry.threshold()));
            }
        }

        model.fallback().ifPresent(fallback -> mapItem(fallback, context.child("range dispatch fallback")));
    }

    @SuppressWarnings("unchecked")
    private static void mapSelectModel(SelectItemModel.Unbaked model, MappingContext context) {
        SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch = model.unbakedSwitch();
        Function<Object, GeyserMatchPredicate.MatchPredicateData> dataConstructor = switch (unbakedSwitch.property()) {
            case Charge ignored -> chargeType -> new GeyserMatchPredicate.ChargeType((CrossbowItem.ChargeType) chargeType);
            case TrimMaterialProperty ignored -> material -> new GeyserMatchPredicate.TrimMaterialData((ResourceKey<TrimMaterial>) material);
            case ContextDimension ignored -> dimension -> new GeyserMatchPredicate.ContextDimension((ResourceKey<Level>) dimension);
            // Why, Mojang?
            case net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty customModelData -> string -> new GeyserMatchPredicate.CustomModelData((String) string, customModelData.index());
            default -> null;
        };

        List<? extends SelectItemModel.SwitchCase<?>> cases = unbakedSwitch.cases();

        if (dataConstructor == null) {
            if (unbakedSwitch.property() instanceof DisplayContext) {
                context.reporter.report(() -> "unsupported select model property display_context, only mapping \"gui\" case, if it exists");
                for (SelectItemModel.SwitchCase<?> switchCase : cases) {
                    if (switchCase.values().contains(ItemDisplayContext.GUI)) {
                        mapItem(switchCase.model(), context.child("select GUI display_context case (unsupported property) "));
                        return;
                    }
                }
            }
            context.reporter.report(() -> "unsupported select model property " + unbakedSwitch.property() + ", only mapping fallback, if present");
            model.fallback().ifPresent(fallback -> mapItem(fallback, context.child("select fallback case (unsupported property) ")));
            return;
        }

        cases.forEach(switchCase -> {
            switchCase.values().forEach(value -> {
                mapItem(switchCase.model(), context.with(new GeyserMatchPredicate(dataConstructor.apply(value)), "select case " + value + " "));
            });
        });
        model.fallback().ifPresent(fallback -> mapItem(fallback, context.child("select fallback case ")));
    }

    private record MappingContext(List<GeyserPredicate> predicateStack, ItemStack stack, ProblemReporter reporter,
                                  Function<GeyserBaseDefinition, GeyserItemDefinition> definitionCreator, PackContext packContext) {

        public MappingContext with(GeyserPredicate predicate, String childName) {
            return new MappingContext(Stream.concat(predicateStack.stream(), Stream.of(predicate)).toList(), stack, reporter.forChild(() -> childName), definitionCreator, packContext);
        }

        public MappingContext child(String childName)  {
            return new MappingContext(predicateStack, stack, reporter.forChild(() -> childName), definitionCreator, packContext);
        }

        public void create(ResourceLocation bedrockIdentifier, ResourceLocation texture, boolean displayHandheld,
                           Optional<ResolvedModel> customModel) {
            List<ResourceLocation> tags;
            if (stack.is(ItemTags.TRIMMABLE_ARMOR)) {
                tags = TRIMMABLE_ARMOR_TAGS;
            } else {
                tags = List.of();
            }

            GeyserBaseDefinition base = new GeyserBaseDefinition(bedrockIdentifier, Optional.ofNullable(stack.getHoverName().tryCollapseToString()), predicateStack,
                    new GeyserBaseDefinition.BedrockOptions(Optional.empty(), true, displayHandheld, calculateProtectionValue(stack), tags),
                    stack.getComponentsPatch());
            try {
                packContext.mappings().map(stack.getItemHolder(), definitionCreator.apply(base));
            } catch (Exception exception) {
                reporter.forChild(() -> "mapping with bedrock identifier " + bedrockIdentifier + " ").report(() -> "failed to pass mapping: " + exception.getMessage());
                return;
            }

            // TODO Should probably get a better way to get geometry texture
            String safeIdentifier = base.textureName();
            String bone = "bone";
            ResourceLocation geometryTexture = texture;
            Optional<BedrockGeometryContext> bedrockGeometry = customModel.flatMap(model -> GeometryMapper.mapGeometry(safeIdentifier, bone, model, geometryTexture));
            Optional<BedrockAnimationContext> bedrockAnimation = customModel.map(model -> AnimationMapper.mapAnimation(safeIdentifier, bone, model.getTopTransforms()));

            boolean exportTexture = true;
            if (customModel.isPresent()) {
                texture = texture.withPath(path -> path + "_icon");
                // FIXME Bit of a hack, preferably render geometry at a later stage
                exportTexture = !packContext.geometryRenderer().render(stack, packContext.paths().packRoot().resolve(BedrockTextures.TEXTURES_FOLDER + texture.getPath() + ".png"));
                packContext.additionalTextureConsumer().accept(geometryTexture);
            }

            packContext.itemConsumer().accept(new BedrockItem(bedrockIdentifier, base.textureName(), texture, exportTexture,
                    AttachableMapper.mapItem(stack.getComponentsPatch(), bedrockIdentifier, bedrockGeometry, bedrockAnimation,
                            packContext.assetResolver(),
                            packContext.additionalTextureConsumer()),
                    bedrockGeometry.map(BedrockGeometryContext::geometry), bedrockAnimation.map(BedrockAnimationContext::animation)));
        }

        private static int calculateProtectionValue(ItemStack stack) {
            ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (modifiers != null) {
                return modifiers.modifiers().stream()
                        .filter(modifier -> modifier.attribute() == Attributes.ARMOR && modifier.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                        .mapToInt(entry -> (int) entry.modifier().amount())
                        .sum();
            }
            return 0;
        }
    }
}
