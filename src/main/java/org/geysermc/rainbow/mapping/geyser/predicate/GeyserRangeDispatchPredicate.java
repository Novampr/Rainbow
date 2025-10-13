package org.geysermc.rainbow.mapping.geyser.predicate;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record GeyserRangeDispatchPredicate(Property property, float threshold, float scale, boolean normalise) implements GeyserPredicate {

    public static final MapCodec<GeyserRangeDispatchPredicate> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Property.CODEC.forGetter(GeyserRangeDispatchPredicate::property),
                    Codec.FLOAT.fieldOf("threshold").forGetter(GeyserRangeDispatchPredicate::threshold),
                    Codec.FLOAT.fieldOf("scale").forGetter(GeyserRangeDispatchPredicate::scale),
                    Codec.BOOL.fieldOf("normalize").forGetter(GeyserRangeDispatchPredicate::normalise)
            ).apply(instance, GeyserRangeDispatchPredicate::new)
    );

    public static final Property BUNDLE_FULLNESS = unit(Property.Type.BUNDLE_FULLNESS);
    public static final Property DAMAGE = unit(Property.Type.DAMAGE);
    public static final Property COUNT = unit(Property.Type.COUNT);

    @Override
    public Type type() {
        return null;
    }

    public interface Property {

        MapCodec<Property> CODEC = Type.CODEC.dispatchMap("property", Property::type, Type::codec);

        Type type();

        enum Type implements StringRepresentable {
            BUNDLE_FULLNESS("bundle_fullness", () -> MapCodec.unit(GeyserRangeDispatchPredicate.BUNDLE_FULLNESS)),
            DAMAGE("damage", () -> MapCodec.unit(GeyserRangeDispatchPredicate.DAMAGE)),
            COUNT("count", () -> MapCodec.unit(GeyserRangeDispatchPredicate.COUNT)),
            CUSTOM_MODEL_DATA("custom_model_data", () -> CustomModelData.CODEC);

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            private final String name;
            private final Supplier<MapCodec<? extends Property>> codec;

            Type(String name, Supplier<MapCodec<? extends Property>> codec) {
                this.name = name;
                this.codec = Suppliers.memoize(codec::get);
            }

            public MapCodec<? extends Property> codec() {
                return codec.get();
            }

            @Override
            public @NotNull String getSerializedName() {
                return name;
            }
        }
    }

    public record CustomModelData(int index) implements Property {
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

    private static Property unit(Property.Type type) {
        return () -> type;
    }
}
