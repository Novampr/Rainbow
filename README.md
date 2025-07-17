# Rainbow

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

Rainbow is a client-side Minecraft mod for the Fabric modloader to generate Geyser item mappings and bedrock resourcepacks
for use with Geyser's [custom item API (v2)](https://github.com/geyserMC/geyser/pull/5189).

Rainbow is currently experimental and capable of the following:

- Generating Geyser item mappings complete with data components and proper bedrock options, by detecting items with a custom `minecraft:item_model` component and analysing their components.
  - Also includes generating mappings with predicates for more complicated Java item model definitions, such as checks for if an item is broken.
    - Does not support range dispatch predicates yet.
  - Also includes detecting if an item should be displayed handheld by looking at the item's model.
- Generating a simple bedrock resourcepack for simple 2D items, as well as:
  - Simple custom armour items, by analysing an item's `minecraft:equippable` component and loaded equipment assets.
  - 3D Java items, by converting the model to a bedrock one, and generating an attachable and animations for it, as well as rendering a custom GUI icon (unlikely to work well as of now).

Rainbow works by detecting custom items in your inventory, or a container/inventory menu you have opened. It analyses
the components of detected items, and uses assets from loaded Java resourcepacks to gather information about item models, textures,
and more.

Commands:

- `/rainbow create <name>` - starts a new pack with the given name. The files will be exported in `.minecraft/geyser/<name>` when finished. Anything in this directory can be overwritten!
- `/rainbow map` - creates Geyser mappings for the item stack you are currently holding. The stack must have a custom model, and a pack must have been created.
- `/rainbow mapinventory` - creates Geyser mappings for every applicable item in your inventory. A pack must have been created.
- `/rainbow finish` - finishes the pack, and writes Geyser mappings to disk.
