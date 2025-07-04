package org.geysermc.packgenerator.mapping.geyser;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import org.geysermc.packgenerator.accessor.BlockModelWrapperLocationAccessor;
import org.geysermc.packgenerator.accessor.ResolvedModelAccessor;
import org.geysermc.packgenerator.accessor.SelectItemModelCasesAccessor;
import org.geysermc.packgenerator.mapping.geyser.predicate.GeyserConditionPredicate;
import org.geysermc.packgenerator.mapping.geyser.predicate.GeyserMatchPredicate;
import org.geysermc.packgenerator.mapping.geyser.predicate.GeyserPredicate;
import org.geysermc.packgenerator.mixin.ConditionalItemModelAccessor;
import org.geysermc.packgenerator.mixin.SelectItemModelAccessor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class GeyserItemMapper {
    private static final List<ResourceLocation> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(ResourceLocation::withDefaultNamespace)
            .toList();

    public static void mapItem(ResourceLocation modelLocation, String displayName, int protectionValue, DataComponentPatch componentPatch, ProblemReporter reporter,
                               BiConsumer<GeyserSingleDefinition, ResourceLocation> mappingTextureConsumer) {
        ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(modelLocation);
        MappingContext context = new MappingContext(List.of(), modelLocation, displayName, protectionValue, componentPatch, reporter.forChild(() -> "client item definition " + modelLocation + " "), mappingTextureConsumer);
        mapItem(model, context);
    }

    private static void mapItem(ItemModel model, MappingContext context) {
        switch (model) {
            case BlockModelWrapper modelWrapper -> {
                ResourceLocation itemModelLocation = ((BlockModelWrapperLocationAccessor) modelWrapper).geyser_mappings_generator$getModelOrigin();

                ((ResolvedModelAccessor) Minecraft.getInstance().getModelManager()).geyser_mappings_generator$getResolvedModel(itemModelLocation)
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
                            if (layer0Texture != null) {
                                texture = layer0Texture.texture();
                            }
                            context.create(bedrockIdentifier, texture, handheld);
                        }, () -> context.reporter.report(() -> "missing block model " + itemModelLocation));
            }
            case ConditionalItemModel conditional -> mapConditionalModel(conditional, context.child("condition " + conditional + " "));
            case SelectItemModel<?> select -> mapSelectModel(select, context.child("select " + select + " "));
            default -> context.reporter.report(() -> "unable to map item model " + model.getClass());
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
        if (predicateProperty == null) {
            context.reporter.report(() -> "unsupported conditional model property " + property);
            return;
        }

        ItemModel onTrue = ((ConditionalItemModelAccessor) model).getOnTrue();
        ItemModel onFalse = ((ConditionalItemModelAccessor) model).getOnFalse();

        mapItem(onTrue, context.with(new GeyserConditionPredicate(predicateProperty, true), "condition on true "));
        mapItem(onFalse, context.with(new GeyserConditionPredicate(predicateProperty, false), "condition on false "));
    }

    private static <T> void mapSelectModel(SelectItemModel<T> model, MappingContext context) {
        //noinspection unchecked
        SelectItemModelProperty<T> property = ((SelectItemModelAccessor<T>) model).getProperty();
        Function<T, GeyserMatchPredicate.MatchPredicateData> dataConstructor = switch (property) {
            case Charge ignored -> chargeType -> new GeyserMatchPredicate.ChargeType((CrossbowItem.ChargeType) chargeType);
            case TrimMaterialProperty ignored -> material -> new GeyserMatchPredicate.TrimMaterialData((ResourceKey<TrimMaterial>) material);
            case ContextDimension ignored -> dimension -> new GeyserMatchPredicate.ContextDimension((ResourceKey<Level>) dimension);
            // Why, Mojang?
            case net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty customModelData -> string -> new GeyserMatchPredicate.CustomModelData((String) string, customModelData.index());
            default -> null;
        };
        if (dataConstructor == null) {
            context.reporter.report(() -> "unsupported select model property " + property);
            return;
        }

        //noinspection unchecked
        Object2ObjectMap<T, ItemModel> cases = ((SelectItemModelCasesAccessor<T>) model).geyser_mappings_generator$getCases();

        cases.entrySet().forEach(caze -> mapItem(caze.getValue(), context.with(new GeyserMatchPredicate(dataConstructor.apply(caze.getKey())), "select case " + caze.getKey() + " ")));
        mapItem(cases.defaultReturnValue(), context.child("default case "));
    }

    private record MappingContext(List<GeyserPredicate> predicateStack, ResourceLocation model, String displayName, int protectionValue, DataComponentPatch componentPatch, ProblemReporter reporter,
                                  BiConsumer<GeyserSingleDefinition, ResourceLocation> mappingTextureConsumer) {

        public MappingContext with(GeyserPredicate predicate, String childName) {
            return new MappingContext(Stream.concat(predicateStack.stream(), Stream.of(predicate)).toList(), model, displayName, protectionValue, componentPatch,
                    reporter.forChild(() -> childName), mappingTextureConsumer);
        }

        public MappingContext child(String childName)  {
            return new MappingContext(predicateStack, model, displayName, protectionValue, componentPatch, reporter.forChild(() -> childName), mappingTextureConsumer);
        }

        public void create(ResourceLocation bedrockIdentifier, ResourceLocation texture, boolean displayHandheld) {
            mappingTextureConsumer.accept(new GeyserSingleDefinition(Optional.of(model), bedrockIdentifier, Optional.of(displayName), predicateStack,
                    new GeyserSingleDefinition.BedrockOptions(Optional.empty(), true, displayHandheld, protectionValue), componentPatch), texture);
        }
    }
}
