package org.geysermc.rainbow.mapping;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import org.geysermc.rainbow.accessor.BlockModelWrapperLocationAccessor;
import org.geysermc.rainbow.accessor.ResolvedModelAccessor;
import org.geysermc.rainbow.accessor.SelectItemModelCasesAccessor;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.BedrockAnimationContext;
import org.geysermc.rainbow.mapping.attachable.AttachableMapper;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.geometry.GeometryMapper;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.geyser.GeyserBaseDefinition;
import org.geysermc.rainbow.mapping.geyser.GeyserItemDefinition;
import org.geysermc.rainbow.mapping.geyser.GeyserLegacyDefinition;
import org.geysermc.rainbow.mapping.geyser.GeyserSingleDefinition;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserConditionPredicate;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserMatchPredicate;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserPredicate;
import org.geysermc.rainbow.mixin.ConditionalItemModelAccessor;
import org.geysermc.rainbow.mixin.RangeSelectItemModelAccessor;
import org.geysermc.rainbow.mixin.SelectItemModelAccessor;
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

    public static void tryMapStack(ItemStack stack, ResourceLocation modelLocation, ProblemReporter reporter, PackContext context) {
        ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(modelLocation);
        mapItem(model, stack, reporter.forChild(() -> "client item definition " + modelLocation + " "), base -> new GeyserSingleDefinition(base, Optional.of(modelLocation)), context);
    }

    public static void tryMapStack(ItemStack stack, int customModelData, ProblemReporter reporter, PackContext context) {
        ItemModel vanillaModel = Minecraft.getInstance().getModelManager().getItemModel(stack.get(DataComponents.ITEM_MODEL));
        reporter = reporter.forChild(() -> "item model " + vanillaModel + " with custom model data " + customModelData + " ");
        if (vanillaModel instanceof RangeSelectItemModel rangeModel) {
            RangeSelectItemModelAccessor accessor = (RangeSelectItemModelAccessor) rangeModel;
            RangeSelectItemModelProperty property = accessor.getProperty();
            // WHY, Mojang?
            if (property instanceof net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty(int index)) {
                if (index == 0) {
                    float scaledCustomModelData = customModelData * accessor.getScale();

                    int modelIndex = RangeSelectItemModelAccessor.invokeLastIndexLessOrEqual(accessor.getThresholds(), scaledCustomModelData);
                    ItemModel model = modelIndex == -1 ? accessor.getFallback() : accessor.getModels()[modelIndex];
                    mapItem(model, stack, reporter, base -> new GeyserLegacyDefinition(base, customModelData), context);
                } else {
                    reporter.report(() -> "range_dispatch custom model data property index is not zero, unable to apply custom model data");
                }
            } else {
                reporter.report(() -> "range_dispatch model property is not custom model data, unable to apply custom model data");
            }
        } else {
            reporter.report(() -> "item model is not range_dispatch, unable to apply custom model data");
        }
    }

    public static void mapItem(ItemModel model, ItemStack stack, ProblemReporter reporter,
                               Function<GeyserBaseDefinition, GeyserItemDefinition> definitionCreator, PackContext packContext) {
        mapItem(model, new MappingContext(List.of(), stack, reporter, definitionCreator, packContext));
    }

    private static void mapItem(ItemModel model, MappingContext context) {
        switch (model) {
            case BlockModelWrapper modelWrapper -> {
                ResourceLocation itemModelLocation = ((BlockModelWrapperLocationAccessor) modelWrapper).rainbow$getModelOrigin();

                ((ResolvedModelAccessor) Minecraft.getInstance().getModelManager()).rainbow$getResolvedModel(itemModelLocation)
                        .ifPresentOrElse(itemModel -> {
                            ResolvedModel parentModel = itemModel.parent();
                            boolean handheld = false;
                            if (parentModel != null) {
                                // debugName() returns the resource location of the model as a string
                                handheld = HANDHELD_MODELS.contains(ResourceLocation.parse(parentModel.debugName()));
                            }

                            ResourceLocation bedrockIdentifier = itemModelLocation;
                            if (bedrockIdentifier.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                                bedrockIdentifier = ResourceLocation.fromNamespaceAndPath("geyser_mc", itemModelLocation.getPath());
                            }

                            ResourceLocation texture = itemModelLocation;
                            Material layer0Texture = itemModel.getTopTextureSlots().getMaterial("layer0");
                            Optional<ResolvedModel> customGeometry = Optional.empty();
                            if (layer0Texture != null) {
                                texture = layer0Texture.texture();
                            } else {
                                // Unknown texture (doesn't use layer0), so we immediately assume the geometry is custom
                                // This check should probably be done differently
                                customGeometry = Optional.of(itemModel);
                            }

                            // Not a problem, but just report to get the model printed in the report file
                            context.reporter.report(() -> "creating mapping for block model " + itemModelLocation);
                            context.create(bedrockIdentifier, texture, handheld, customGeometry);
                        }, () -> context.reporter.report(() -> "missing block model " + itemModelLocation));
            }
            case ConditionalItemModel conditional -> mapConditionalModel(conditional, context.child("condition model "));
            case SelectItemModel<?> select -> mapSelectModel(select, context.child("select model "));
            default -> context.reporter.report(() -> "unsupported item model " + model.getClass()); // TODO intermediary stuff
        }
    }

    private static void mapConditionalModel(ConditionalItemModel model, MappingContext context) {
        ItemModelPropertyTest property = ((ConditionalItemModelAccessor) model).getProperty();
        GeyserConditionPredicate.Property predicateProperty = switch (property) {
            case Broken ignored -> GeyserConditionPredicate.BROKEN;
            case Damaged ignored -> GeyserConditionPredicate.DAMAGED;
            case CustomModelDataProperty customModelData -> new GeyserConditionPredicate.CustomModelData(customModelData.index());
            case HasComponent hasComponent -> new GeyserConditionPredicate.HasComponent(hasComponent.componentType()); // ignoreDefault property not a thing, we should look into that in Geyser! TODO
            case FishingRodCast ignored -> GeyserConditionPredicate.FISHING_ROD_CAST;
            default -> null;
        };
        ItemModel onTrue = ((ConditionalItemModelAccessor) model).getOnTrue();
        ItemModel onFalse = ((ConditionalItemModelAccessor) model).getOnFalse();

        if (predicateProperty == null) {
            context.reporter.report(() -> "unsupported conditional model property " + property + ", only mapping on_false");
            mapItem(onFalse, context.child("condition on_false (unsupported property)"));
            return;
        }

        mapItem(onTrue, context.with(new GeyserConditionPredicate(predicateProperty, true), "condition on true "));
        mapItem(onFalse, context.with(new GeyserConditionPredicate(predicateProperty, false), "condition on false "));
    }

    @SuppressWarnings("unchecked")
    private static <T> void mapSelectModel(SelectItemModel<T> model, MappingContext context) {
        SelectItemModelProperty<T> property = ((SelectItemModelAccessor<T>) model).getProperty();
        Function<T, GeyserMatchPredicate.MatchPredicateData> dataConstructor = switch (property) {
            case Charge ignored -> chargeType -> new GeyserMatchPredicate.ChargeType((CrossbowItem.ChargeType) chargeType);
            case TrimMaterialProperty ignored -> material -> new GeyserMatchPredicate.TrimMaterialData((ResourceKey<TrimMaterial>) material);
            case ContextDimension ignored -> dimension -> new GeyserMatchPredicate.ContextDimension((ResourceKey<Level>) dimension);
            // Why, Mojang?
            case net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty customModelData -> string -> new GeyserMatchPredicate.CustomModelData((String) string, customModelData.index());
            default -> null;
        };

        Object2ObjectMap<T, ItemModel> cases = ((SelectItemModelCasesAccessor<T>) model).rainbow$getCases();

        if (dataConstructor == null) {
            if (property instanceof DisplayContext) {
                ItemModel gui = cases.get(ItemDisplayContext.GUI);
                if (gui != null) {
                    context.reporter.report(() -> "unsupported select model property display_context, only mapping \"gui\" case");
                    mapItem(gui, context.child("select GUI display_context case (unsupported property) "));
                    return;
                }
            }
            context.reporter.report(() -> "unsupported select model property " + property + ", only mapping fallback");
            mapItem(cases.defaultReturnValue(), context.child("select fallback case (unsupported property) "));
            return;
        }

        cases.forEach((key, value) -> mapItem(value, context.with(new GeyserMatchPredicate(dataConstructor.apply(key)), "select case " + key + " ")));
        mapItem(cases.defaultReturnValue(), context.child("select fallback case "));
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
            GeyserBaseDefinition base = new GeyserBaseDefinition(bedrockIdentifier, Optional.of(stack.getHoverName().getString()), predicateStack,
                    new GeyserBaseDefinition.BedrockOptions(Optional.empty(), true, displayHandheld, calculateProtectionValue(stack)), stack.getComponentsPatch());
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
            Optional<BedrockGeometryContext> bedrockGeometry = customModel.map(model -> GeometryMapper.mapGeometry(safeIdentifier, bone, model, geometryTexture));
            Optional<BedrockAnimationContext> bedrockAnimation = customModel.map(model -> AnimationMapper.mapAnimation(safeIdentifier, bone, model.getTopTransforms()));

            boolean exportTexture = true;
            if (customModel.isPresent()) {
                texture = texture.withPath(path -> path + "_icon");
                GeometryRenderer.render(stack, packContext.packPath().resolve(BedrockTextures.TEXTURES_FOLDER + texture.getPath() + ".png"));
                exportTexture = false;
                packContext.additionalTextureConsumer().accept(geometryTexture);
            }

            packContext.itemConsumer().accept(new BedrockItem(bedrockIdentifier, base.textureName(), texture, exportTexture,
                    AttachableMapper.mapItem(stack.getComponentsPatch(), bedrockIdentifier, bedrockGeometry, bedrockAnimation, packContext.additionalTextureConsumer()),
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
