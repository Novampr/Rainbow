package org.geysermc.packgenerator.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.geysermc.packgenerator.CodecUtil;

import java.util.List;
import java.util.UUID;

// TODO metadata
public record PackManifest(Header header, List<Module> modules) {

    public static final Codec<PackManifest> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 2),
                    Header.CODEC.fieldOf("header").forGetter(PackManifest::header),
                    Module.CODEC.listOf().fieldOf("modules").forGetter(PackManifest::modules)
            ).apply(instance, (formatVersion, header, modules) -> new PackManifest(header, modules))
    );

    public record Header(String name, String description, UUID uuid, String version) {
        public static final MapCodec<Header> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Header::name),
                        Codec.STRING.fieldOf("description").forGetter(Header::description),
                        UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Header::uuid),
                        Codec.STRING.fieldOf("version").forGetter(Header::version)
                ).apply(instance, Header::new)
        );
        public static final Codec<Header> CODEC = MAP_CODEC.codec();
    }

    public record Module(Header header) {
        public static final Codec<Module> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        CodecUtil.unitVerifyCodec(Codec.STRING, "type", "resources"),
                        Header.MAP_CODEC.forGetter(Module::header)
                ).apply(instance, (type, header) -> new Module(header))
        );
    }
}

