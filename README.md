# geyser-mappings-generator

This is a Minecraft mod for the Fabric modloader to generate Geyser mappings (and potentially in the future, full bedrock packs)
for use with the custom item API (v2).

Commands:

- `/packgenerator create <name>` - starts a new pack with the given name. The files will be exported in `.minecraft/geyser/<name>` when finished. Anything in this directory can be overwritten!
- `/packgenerator map` - creates Geyser mappings for the item stack you are currently holding. The stack must have a custom model, and a pack must have been created.
- `/packgenerator mapinventory` - creates Geyser mappings for every applicable item in your inventory. A pack must have been created.
- `/packgenerator finish` - finishes the pack, and writes Geyser mappings to disk.
