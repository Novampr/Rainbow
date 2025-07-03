package org.geysermc.packgenerator.mapping.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public interface GeyserMappingNew {

    Codec<GeyserMappingNew> CODEC = Type.CODEC.dispatch(GeyserMappingNew::type, Type::codec);

    Type type();

    enum Type implements StringRepresentable {
        SINGLE("definition", GeyserMapping_.CODEC),
        GROUP("group", GroupDefinition.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends GeyserMappingNew> codec;

        Type(String name, MapCodec<? extends GeyserMappingNew> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends GeyserMappingNew> codec() {
            return codec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
