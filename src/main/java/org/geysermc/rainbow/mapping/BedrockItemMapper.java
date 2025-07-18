package org.geysermc.rainbow.mapping;

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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import org.geysermc.rainbow.mapping.geyser.GeyserMappings;
import org.geysermc.rainbow.mapping.geyser.GeyserSingleDefinition;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserConditionPredicate;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserMatchPredicate;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserPredicate;
import org.geysermc.rainbow.mixin.ConditionalItemModelAccessor;
import org.geysermc.rainbow.mixin.SelectItemModelAccessor;
import org.geysermc.rainbow.pack.BedrockItem;
import org.geysermc.rainbow.pack.BedrockTextures;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class BedrockItemMapper {
    private static final List<ResourceLocation> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(ResourceLocation::withDefaultNamespace)
            .toList();

    public static void tryMapStack(ItemStack stack, ResourceLocation model, ProblemReporter reporter,
                                   GeyserMappings mappings, Path packPath, BedrockItemConsumer itemConsumer, Consumer<ResourceLocation> additionalTextureConsumer) {
        String displayName = stack.getHoverName().getString();
        int protectionValue = 0; // TODO check the attributes

        mapItem(model, displayName, protectionValue, stack.getComponentsPatch(), reporter,
                mapping -> mappings.map(stack.getItemHolder(), mapping), packPath, itemConsumer, additionalTextureConsumer);
    }

    public static void mapItem(ResourceLocation modelLocation, String displayName, int protectionValue, DataComponentPatch componentPatch, ProblemReporter reporter,
                               Consumer<GeyserSingleDefinition> mappingConsumer, Path packPath, BedrockItemConsumer itemConsumer, Consumer<ResourceLocation> additionalTextureConsumer) {
        ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(modelLocation);
        MappingContext context = new MappingContext(List.of(), modelLocation, displayName, protectionValue, componentPatch,
                reporter.forChild(() -> "client item definition " + modelLocation + " "), mappingConsumer, itemConsumer, packPath, additionalTextureConsumer);
        mapItem(model, context);
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
                            context.create(bedrockIdentifier, texture, handheld, customGeometry);
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
        Object2ObjectMap<T, ItemModel> cases = ((SelectItemModelCasesAccessor<T>) model).rainbow$getCases();

        cases.forEach((key, value) -> mapItem(value, context.with(new GeyserMatchPredicate(dataConstructor.apply(key)), "select case " + key + " ")));
        mapItem(cases.defaultReturnValue(), context.child("default case "));
    }

    private record MappingContext(List<GeyserPredicate> predicateStack, ResourceLocation model, String displayName, int protectionValue, DataComponentPatch componentPatch, ProblemReporter reporter,
                                  Consumer<GeyserSingleDefinition> mappingConsumer, BedrockItemConsumer itemConsumer, Path packPath,
                                  Consumer<ResourceLocation> additionalTextureConsumer) {

        public MappingContext with(GeyserPredicate predicate, String childName) {
            return new MappingContext(Stream.concat(predicateStack.stream(), Stream.of(predicate)).toList(), model, displayName, protectionValue, componentPatch,
                    reporter.forChild(() -> childName), mappingConsumer, itemConsumer, packPath, additionalTextureConsumer);
        }

        public MappingContext child(String childName)  {
            return new MappingContext(predicateStack, model, displayName, protectionValue, componentPatch, reporter.forChild(() -> childName),
                    mappingConsumer, itemConsumer, packPath, additionalTextureConsumer);
        }

        public void create(ResourceLocation bedrockIdentifier, ResourceLocation texture, boolean displayHandheld,
                           Optional<ResolvedModel> customModel) {
            GeyserSingleDefinition definition = new GeyserSingleDefinition(Optional.of(model), bedrockIdentifier, Optional.of(displayName), predicateStack,
                    new GeyserSingleDefinition.BedrockOptions(Optional.empty(), true, displayHandheld, protectionValue), componentPatch);
            try {
                mappingConsumer.accept(definition);
            } catch (Exception exception) {
                reporter.forChild(() -> "mapping with bedrock identifier " + bedrockIdentifier + " ").report(() -> "failed to pass mapping: " + exception.getMessage());
                return;
            }

            // TODO Should probably get a better way to get geometry texture
            String safeIdentifier = definition.textureName();
            String bone = "bone";
            ResourceLocation geometryTexture = texture;
            Optional<BedrockGeometryContext> bedrockGeometry = customModel.map(model -> GeometryMapper.mapGeometry(safeIdentifier, bone, model, geometryTexture));
            Optional<BedrockAnimationContext> bedrockAnimation = customModel.map(model -> AnimationMapper.mapAnimation(safeIdentifier, bone, model.getTopTransforms()));

            boolean exportTexture = true;
            if (customModel.isPresent()) {
                ItemStack fakeItem = new ItemStack(Items.FLINT);
                fakeItem.set(DataComponents.ITEM_MODEL, model);

                texture = texture.withPath(path -> path + "_icon");
                GeometryRenderer.render(fakeItem, packPath.resolve(BedrockTextures.TEXTURES_FOLDER + texture.getPath() + ".png"));
                exportTexture = false;
                additionalTextureConsumer.accept(geometryTexture);
            }

            itemConsumer.accept(new BedrockItem(bedrockIdentifier, definition.textureName(), texture, exportTexture,
                    AttachableMapper.mapItem(componentPatch, bedrockIdentifier, bedrockGeometry, bedrockAnimation, additionalTextureConsumer),
                    bedrockGeometry.map(BedrockGeometryContext::geometry), bedrockAnimation.map(BedrockAnimationContext::animation)));
        }
    }
}
