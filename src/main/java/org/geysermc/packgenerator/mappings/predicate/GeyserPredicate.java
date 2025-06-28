package org.geysermc.packgenerator.mappings.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface GeyserPredicate {

    Codec<GeyserPredicate> CODEC = Type.CODEC.dispatch(GeyserPredicate::type, Type::codec);
    Codec<List<GeyserPredicate>> LIST_CODEC = ExtraCodecs.compactListCodec(CODEC);

    Type type();

    enum Type implements StringRepresentable {
        CONDITION("condition", GeyserConditionPredicate.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends GeyserPredicate> codec;

        Type(String name, MapCodec<? extends GeyserPredicate> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends GeyserPredicate> codec() {
            return codec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
