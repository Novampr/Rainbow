package org.geysermc.rainbow.mapping;

import org.geysermc.rainbow.pack.BedrockItem;

@FunctionalInterface
public interface BedrockItemConsumer {

    void accept(BedrockItem item);
}
