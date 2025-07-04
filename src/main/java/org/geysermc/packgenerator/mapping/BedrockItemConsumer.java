package org.geysermc.packgenerator.mapping;

import org.geysermc.packgenerator.pack.BedrockItem;

@FunctionalInterface
public interface BedrockItemConsumer {

    void accept(BedrockItem item);
}
