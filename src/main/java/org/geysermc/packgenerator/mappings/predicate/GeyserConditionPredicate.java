package org.geysermc.packgenerator.mappings.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public record GeyserConditionPredicate(ConditionProperty property, boolean expected) implements GeyserPredicate {

    public static final MapCodec<GeyserConditionPredicate> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ConditionProperty.CODEC.forGetter(GeyserConditionPredicate::property),
                    Codec.BOOL.optionalFieldOf("expected", true).forGetter(GeyserConditionPredicate::expected)
            ).apply(instance, GeyserConditionPredicate::new)
    );

    public static final ConditionProperty BROKEN = unit(ConditionProperty.Type.BROKEN);
    public static final ConditionProperty DAMAGED = unit(ConditionProperty.Type.DAMAGED);
    public static final ConditionProperty FISHING_ROD_CAST = unit(ConditionProperty.Type.FISHING_ROD_CAST);

    @Override
    public Type type() {
        return Type.CONDITION;
    }

    public interface ConditionProperty {

        MapCodec<ConditionProperty> CODEC = Type.CODEC.dispatchMap("property", ConditionProperty::type, Type::codec);

        Type type();

        enum Type implements StringRepresentable {
            BROKEN("broken", MapCodec.unit(GeyserConditionPredicate.BROKEN)),
            DAMAGED("damaged", MapCodec.unit(GeyserConditionPredicate.DAMAGED)),
            CUSTOM_MODEL_DATA("custom_model_data", CustomModelData.CODEC),
            HAS_COMPONENT("has_component", HasComponent.CODEC),
            FISHING_ROD_CAST("fishing_rod_cast", MapCodec.unit(GeyserConditionPredicate.FISHING_ROD_CAST));

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            private final String name;
            private final MapCodec<? extends ConditionProperty> codec;

            Type(String name, MapCodec<? extends ConditionProperty> codec) {
                this.name = name;
                this.codec = codec;
            }

            public MapCodec<? extends ConditionProperty> codec() {
                return codec;
            }

            @Override
            public @NotNull String getSerializedName() {
                return name;
            }
        }
    }

    public record CustomModelData(int index) implements ConditionProperty {
        public static final MapCodec<CustomModelData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelData::index)
                ).apply(instance, CustomModelData::new)
        );

        @Override
        public Type type() {
            return Type.CUSTOM_MODEL_DATA;
        }
    }

    public record HasComponent(DataComponentType<?> component) implements ConditionProperty {
        public static final MapCodec<HasComponent> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        DataComponentType.CODEC.fieldOf("component").forGetter(HasComponent::component)
                ).apply(instance, HasComponent::new)
        );

        @Override
        public Type type() {
            return Type.HAS_COMPONENT;
        }
    }

    private static ConditionProperty unit(ConditionProperty.Type type) {
        return () -> type;
    }
}
