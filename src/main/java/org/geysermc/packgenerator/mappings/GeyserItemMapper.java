package org.geysermc.packgenerator.mappings;

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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import org.geysermc.packgenerator.accessor.BlockModelWrapperLocationAccessor;
import org.geysermc.packgenerator.accessor.SelectItemModelCasesAccessor;
import org.geysermc.packgenerator.mappings.predicate.GeyserConditionPredicate;
import org.geysermc.packgenerator.mappings.predicate.GeyserMatchPredicate;
import org.geysermc.packgenerator.mappings.predicate.GeyserPredicate;
import org.geysermc.packgenerator.mixin.ConditionalItemModelAccessor;
import org.geysermc.packgenerator.mixin.SelectItemModelAccessor;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class GeyserItemMapper {

    public static Stream<GeyserMapping> mapItem(ResourceLocation modelLocation, String displayName, int protectionValue, DataComponentPatch componentPatch) {
        MappingContext context = new MappingContext(List.of(), modelLocation, displayName, protectionValue, componentPatch);
        ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(modelLocation);
        return mapItem(model, context);
    }

    private static Stream<GeyserMapping> mapItem(ItemModel model, MappingContext context) {
        switch (model) {
            case BlockModelWrapper modelWrapper -> {
                ResourceLocation itemModel = ((BlockModelWrapperLocationAccessor) modelWrapper).geyser_mappings_generator$getModelOrigin();
                return Stream.of(context.create(itemModel));
            }
            case ConditionalItemModel conditional -> {
                return mapConditionalModel(conditional, context);
            }
            case SelectItemModel<?> select -> {
                return mapSelectModel(select, context);
            }
            default -> {}
        }
        throw new UnsupportedOperationException("Unable to map item model " + model.getClass());
    }

    private static Stream<GeyserMapping> mapConditionalModel(ConditionalItemModel model, MappingContext context) {
        ItemModelPropertyTest property = ((ConditionalItemModelAccessor) model).getProperty();
        GeyserConditionPredicate.Property predicateProperty = switch (property) {
            case Broken ignored -> GeyserConditionPredicate.BROKEN;
            case Damaged ignored -> GeyserConditionPredicate.DAMAGED;
            case CustomModelDataProperty customModelData -> new GeyserConditionPredicate.CustomModelData(customModelData.index());
            case HasComponent hasComponent -> new GeyserConditionPredicate.HasComponent(hasComponent.componentType()); // ignoreDefault property not a thing, we should look into that in Geyser! TODO
            case FishingRodCast ignored -> GeyserConditionPredicate.FISHING_ROD_CAST;
            default -> throw new UnsupportedOperationException("Unsupported conditional model property " + property.getClass());
        };

        ItemModel onTrue = ((ConditionalItemModelAccessor) model).getOnTrue();
        ItemModel onFalse = ((ConditionalItemModelAccessor) model).getOnFalse();

        return Stream.concat(
                mapItem(onTrue, context.with(new GeyserConditionPredicate(predicateProperty, true))),
                mapItem(onFalse, context.with(new GeyserConditionPredicate(predicateProperty, false)))
        );
    }

    private static <T> Stream<GeyserMapping> mapSelectModel(SelectItemModel<T> model, MappingContext context) {
        //noinspection unchecked
        SelectItemModelProperty<T> property = ((SelectItemModelAccessor<T>) model).getProperty();
        Function<T, GeyserMatchPredicate.MatchPredicateData> dataConstructor = switch (property) {
            case Charge ignored -> chargeType -> new GeyserMatchPredicate.ChargeType((CrossbowItem.ChargeType) chargeType);
            case TrimMaterialProperty ignored -> material -> new GeyserMatchPredicate.TrimMaterialData((ResourceKey<TrimMaterial>) material);
            case ContextDimension ignored -> dimension -> new GeyserMatchPredicate.ContextDimension((ResourceKey<Level>) dimension);
            // Why, Mojang?
            case net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty customModelData -> string -> new GeyserMatchPredicate.CustomModelData((String) string, customModelData.index());
            default -> throw new UnsupportedOperationException("Unsupported select model property " + property.getClass());
        };

        //noinspection unchecked
        Object2ObjectMap<T, ItemModel> cases = ((SelectItemModelCasesAccessor<T>) model).geyser_mappings_generator$getCases();
        return Stream.concat(
                cases.entrySet().stream()
                        .flatMap(caze -> mapItem(caze.getValue(), context.with(new GeyserMatchPredicate(dataConstructor.apply(caze.getKey()))))),
                mapItem(cases.defaultReturnValue(), context)
        );
    }

    private record MappingContext(List<GeyserPredicate> predicateStack, ResourceLocation model, String displayName, int protectionValue, DataComponentPatch componentPatch) {

        public MappingContext with(GeyserPredicate predicate) {
            return new MappingContext(Stream.concat(predicateStack.stream(), Stream.of(predicate)).toList(), model, displayName, protectionValue, componentPatch);
        }

        public GeyserMapping create(ResourceLocation bedrockIdentifier) {
            return new GeyserMapping(model, bedrockIdentifier, Optional.of(displayName), predicateStack,
                    new GeyserMapping.BedrockOptions(Optional.empty(), true, false, protectionValue), // TODO handheld prediction
                    componentPatch);
        }
    }
}
