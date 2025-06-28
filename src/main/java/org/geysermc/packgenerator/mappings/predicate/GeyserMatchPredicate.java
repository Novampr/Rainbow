package org.geysermc.packgenerator.mappings.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CrossbowItem;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record GeyserMatchPredicate(MatchPredicateData data) implements GeyserPredicate {

    public static final MapCodec<GeyserMatchPredicate> CODEC = MatchPredicateData.CODEC.xmap(GeyserMatchPredicate::new, GeyserMatchPredicate::data);

    @Override
    public Type type() {
        return Type.MATCH;
    }

    public interface MatchPredicateData {

        MapCodec<MatchPredicateData> CODEC = Type.CODEC.dispatchMap("property", MatchPredicateData::type, Type::codec);

        Type type();

        enum Type implements StringRepresentable {
            CHARGE_TYPE("charge_type", ChargeType.CODEC),
            TRIM_MATERIAL("trim_material", TrimMaterial.CODEC),
            CONTEXT_DIMENSION("context_dimension", ContextDimension.CODEC),
            CUSTOM_MODEL_DATA("custom_model_data", CustomModelData.CODEC);

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            private final String name;
            private final MapCodec<? extends MatchPredicateData> codec;

            Type(String name, MapCodec<? extends MatchPredicateData> codec) {
                this.name = name;
                this.codec = codec;
            }

            public MapCodec<? extends MatchPredicateData> codec() {
                return codec;
            }

            @Override
            public @NotNull String getSerializedName() {
                return name;
            }
        }
    }

    public record ChargeType(CrossbowItem.ChargeType chargeType) implements MatchPredicateData {
        public static final MapCodec<ChargeType> CODEC = simpleCodec(CrossbowItem.ChargeType.CODEC, ChargeType::chargeType, ChargeType::new);

        @Override
        public Type type() {
            return Type.CHARGE_TYPE;
        }
    }

    // TODO might change into ResourceKey?
    public record TrimMaterial(ResourceLocation material) implements MatchPredicateData {
        public static final MapCodec<TrimMaterial> CODEC = simpleCodec(ResourceLocation.CODEC, TrimMaterial::material, TrimMaterial::new);

        @Override
        public Type type() {
            return Type.TRIM_MATERIAL;
        }
    }

    // TODO might change into ResourceKey?
    public record ContextDimension(ResourceLocation dimension) implements MatchPredicateData {
        public static final MapCodec<ContextDimension> CODEC = simpleCodec(ResourceLocation.CODEC, ContextDimension::dimension, ContextDimension::new);

        @Override
        public Type type() {
            return Type.CONTEXT_DIMENSION;
        }
    }

    public record CustomModelData(String string, int index) implements MatchPredicateData {
        public static final MapCodec<CustomModelData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("value").forGetter(CustomModelData::string),
                        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelData::index)
                ).apply(instance, CustomModelData::new)
        );

        @Override
        public Type type() {
            return Type.CUSTOM_MODEL_DATA;
        }
    }

    private static <P, T> MapCodec<P> simpleCodec(Codec<T> codec, Function<P, T> getter, Function<T, P> constructor) {
        return codec.fieldOf("value").xmap(constructor, getter);
    }
}
