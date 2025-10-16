package org.geysermc.rainbow.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;

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

    public PackManifest increment() {
        return new PackManifest(header.increment(), modules.stream().map(Module::increment).toList());
    }

    public record Header(String name, String description, UUID uuid, BedrockVersion version, BedrockVersion minEngineVersion) {
        public static final MapCodec<Header> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Header::name),
                        Codec.STRING.fieldOf("description").forGetter(Header::description),
                        UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Header::uuid),
                        BedrockVersion.CODEC.fieldOf("version").forGetter(Header::version),
                        BedrockVersion.CODEC.fieldOf("min_engine_version").forGetter(Header::minEngineVersion)
                ).apply(instance, Header::new)
        );
        public static final Codec<Header> CODEC = MAP_CODEC.codec();

        public Header increment() {
            return new Header(name, description, uuid, version.increment(), minEngineVersion);
        }
    }

    public record Module(String name, String description, UUID uuid, BedrockVersion version) {
        public static final Codec<Module> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        CodecUtil.unitVerifyCodec(Codec.STRING, "type", "resources"),
                        Codec.STRING.fieldOf("name").forGetter(Module::name),
                        Codec.STRING.fieldOf("description").forGetter(Module::description),
                        UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Module::uuid),
                        BedrockVersion.CODEC.fieldOf("version").forGetter(Module::version)
                ).apply(instance, (type, name, description, uuid, version) -> new Module(name, description, uuid, version))
        );

        public Module increment() {
            return new Module(name, description, uuid, version.increment());
        }
    }

    public static PackManifest create(String name, String description, UUID uuid, BedrockVersion version) {
        return new PackManifest(new PackManifest.Header(name, description, uuid, version, PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, description, uuid, version)));
    }
}

