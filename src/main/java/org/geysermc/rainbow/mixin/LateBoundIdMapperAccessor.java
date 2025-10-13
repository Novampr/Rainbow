package org.geysermc.rainbow.mixin;

import com.google.common.collect.BiMap;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExtraCodecs.LateBoundIdMapper.class)
public interface LateBoundIdMapperAccessor<I, V> {

    @Accessor
    BiMap<I, V> getIdToValue();
}
