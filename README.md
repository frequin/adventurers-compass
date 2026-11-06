# Adventurer's Compass

Adventurer's Compass is a Minecraft (Fabric) mod for nomadic players: a compass that points at the nearest structure worth reaching, and remembers where you have already been.

## Features

- **Selection**: tick several structures and groups (all villages, an outpost, a modded tower) in a tree screen. A single search finds the nearest one of any of them in your current dimension. Every registered structure works, from mods and datapacks too.
- **Visited**: an Instance, one occurrence of a structure in your world, becomes Visited as soon as you walk into one of its pieces, or with "Mark Visited". Searches skip Visited Instances. Your Visited list is per player and per world, survives death, and you can unmark entries one by one or all at once in its tab of the screen.
- **Pass**: skip the current Target and search for the next one.
- **HUD**: one line, "Plains Village · 342 m", and optionally the coordinates. The needle spins while searching and when the Target is in another dimension.
- **Cost and wear**: a found Target costs XP (5 levels in the Overworld, 10 elsewhere, by default) and wears the compass (3 finds by default). Crafting a worn compass with a compass repairs it without losing its Selection or Target. Creative players pay nothing.
- **Server friendly**: searches are spread over ticks and shared between players. Each player has their own Visited and searches.

## Configuration

The config file is `config/adventurerscompass.json`. Every option is explained by a comment next to it in the file.

Server (`common`):

| Option | Default | |
|---|---|---|
| `allowTeleport` | `false` | Ops and creative players can teleport to the Target (same dimension only) |
| `displayCoordinates` | `true` | Show the Target's X and Z on the HUD |
| `maxRadius` | `2500` | Search radius in blocks, about half a day's walk |
| `compassDurability` | `3` | Targets found before the compass breaks; `0` disables wear |
| `defaultXpLevel` | `5` | Levels a Structure costs, before the dimension multiplier |
| `perStructureXpLevels` | `{}` | Per-structure levels, by id with `*` and `?` wildcards; `0` is free |
| `defaultDimensionXpMultiplier` | `2` | Multiplier for dimensions not listed below |
| `dimensionXpMultipliers` | `{"minecraft:overworld": 1}` | Multiplier per dimension; the cost is rounded up |
| `disabledDimensions` | `[]` | Dimensions where the compass does not search |
| `hiddenStructures` | `[]` | Structures never listed or searched: ids with wildcards, or `#tag` entries. The tag `#c:hidden_from_locator_selection` always hides |
| `visitedMessage` | `true` | Message above the hotbar when an Instance becomes Visited |
| `targetVisitedSound` | `true` | Soft sound when your Target becomes Visited |

Client (`client`): `hudEnabled`, `displayWithChatOpen`, `translateStructureNames`, `overlaySide`, `overlayLineOffset`.

### Coming from Explorer's Compass

Adventurer's Compass installs alongside Explorer's Compass and shares nothing with it. Its config is not compatible and nothing is migrated: the defaults are nomadic (radius 2500, no teleport, XP cost and wear). `structureBlacklist` became `hiddenStructures`, and `maxNextSearches` and `maxSamples` are gone. If you used an earlier build of this mod, delete `config/adventurerscompass.json` to get the new defaults; old compasses lose their search data.

## Credits

This mod is based on [Explorer's Compass](https://github.com/MattCzyr/ExplorersCompass) by ChaosTheDude (MattCzyr), used under the [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) license. Textures and translations come from the original mod.

## Develop

### Setup

Clone the repository:
```
git clone https://github.com/frequin/adventurers-compass.git
```

Then open the project in IntelliJ IDEA as a Gradle project, or generate IDE files:
```
./gradlew genSources
./gradlew idea
```

### Build

```
./gradlew build
```

The jar is written to `build/libs`.

### Publish to Modrinth

1. Create the project `adventurers-compass` on [Modrinth](https://modrinth.com).
2. Create a personal access token with the *Create versions* and *Write projects* scopes.
3. Update `mod_version` in `gradle.properties` and add an entry at the top of `CHANGELOG.md`.
4. Run:
```
MODRINTH_TOKEN=<token> ./gradlew modrinth
```

## License

This mod is available under the [Creative Commons Attribution-NonCommercial ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode), like the original mod.
