package org.geysermc.packgenerator.mapping.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public interface GeyserMapping {

    Codec<GeyserMapping> CODEC = Type.CODEC.dispatch(GeyserMapping::type, Type::codec);

    Type type();

    enum Type implements StringRepresentable {
        SINGLE("definition", GeyserSingleDefinition.CODEC),
        GROUP("group", GeyserGroupDefinition.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends GeyserMapping> codec;

        Type(String name, MapCodec<? extends GeyserMapping> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends GeyserMapping> codec() {
            return codec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
