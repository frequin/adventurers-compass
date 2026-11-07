# Adventurer's Compass

Adventurer's Compass is a Minecraft (Fabric) mod for nomadic players: a compass that points at the nearest structure worth reaching, and remembers where you have already been.

## Features

- **Selection**: tick several structures and groups (all villages, an outpost, a modded tower) in a tree screen. A single search finds the nearest one of any of them in your current dimension. Every registered structure works, from mods and datapacks too.
- **Visited**: an Instance, one occurrence of a structure in your world, becomes Visited as soon as you walk into one of its pieces, or with "Mark Visited". Searches skip Visited Instances. Your Visited list is per player and per world, survives death, and you can unmark entries one by one or all at once in its tab of the screen.
- **Pass**: skip the current Target and search for the next one.
- **HUD**: one line, "Plains Village · 342 m", and optionally the coordinates. The needle spins while searching and when the Target is in another dimension.
- **Cost and wear**: a found Target costs XP (5 levels in the Overworld, 10 elsewhere, by default) and wears the compass (3 finds by default). Crafting a worn compass with a compass repairs it without losing its Selection or Target. Creative players pay nothing.
- **Server friendly**: searches are spread over ticks and shared between players. Each player has their own Visited and searches.

## Requirements

Minecraft 26.3 with [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api). On a server, install the mod on the server and on every client.

The mod is fully translated in English and French. Other languages translate the item, Group and structure names only; the rest shows in English.

## How to use

### Get the compass

Craft it from a compass in the middle, cracked stone bricks on the four sides and cobwebs in the four corners. The recipe shows in the recipe book once you carry a compass.

### Compose a Selection

Use the compass (right-click, crouching or not) to open its screen. The **Search** tab lists, in a tree, the Structures of your current dimension, by Group (Villages, Ruined Portals…):

- Tick a Group to search all its Structures, or expand it and tick some of them. Unticking one Structure in a ticked Group keeps the others ticked.
- Type in the filter box to narrow the tree, or `@mod` to show one mod's Structures. **Select all** ticks everything the tree shows, filter included; **Clear Selection** unticks everything.
- A Structure ticked in another dimension stays in the Selection. The **N ignored here** count, with its tooltip, names what the compass cannot search here: Structures that don't generate in this dimension, and Structures Hidden by the server.

The Selection is stored on the compass item: it survives death, chests and logouts. Each compass has its own, so carrying several compasses works like having several profiles.

### Search

- **Search** closes the screen and finds the nearest Instance of any Structure in the Selection, in your current dimension, within `maxRadius` blocks. Instances you have Visited or Skipped are left out. Pressing Enter in the empty filter box does the same.
- While searching, the needle spins and the HUD reads "Searching…". Reopen the screen to **Cancel the search**: the old Target is kept and nothing is paid. Changing the Selection or the dimension also cancels it.
- Once found, the Instance is the **Target**: the needle points at it and the HUD shows its name, distance and coordinates.
- **Pass** sets the current Target aside (Skipped) and searches for the next one.
- If nothing is found, the HUD says so for 5 seconds, counting the Visited and Skipped Instances it left out, then the previous Target comes back.

Search and Pass need as many levels as the most expensive Structure the Selection can find here, and are greyed with "Needs N levels" otherwise. The levels are taken only when a Target is found: a cancelled search or one that finds nothing is free. Each Target found, by Search or Pass, wears the compass; a worn-out compass keeps its Target but cannot search. Repair it by crafting it with a compass: the Selection, Target and Skipped list are kept. Creative players pay nothing and never wear it.

### The Target

Under the Selection, the screen shows the Target with these buttons:

- **Mark Visited**: mark the Target Visited without walking to it.
- **Clear Target**: forget the Target, at no cost. The Selection and Skipped list stay.
- **Teleport**: only when the server allows it (`allowTeleport`), for ops and creative players, and only to a Target in your current dimension.

A Target in another dimension is kept: the needle spins and the HUD reads "Village · other dimension (Overworld)". Back in its dimension, it is active again without a new search. Searching in the other dimension replaces it.

### Visited and Skipped

You mark an Instance Visited by walking into one of its pieces (a village house, a stronghold corridor), with a message above the hotbar. Walking into your Target also plays a soft sound; the HUD then invites you to search for the next one. Spectators never mark anything.

The **Visited** and **Skipped** tabs list these Instances with their day of visit (Visited only), their X and Z, and their distance, or their dimension when it isn't yours, so two villages of the same kind can be told apart:

- Filter the rows, and sort them by most recent, nearest or name.
- Click the red ✕ on a row to unmark it: the Instance can be found again.
- **Unmark all** and **Remove all** empty the list, after a confirmation.

The Visited list belongs to the player, for each world, and survives death. The Skipped list belongs to the compass, and a new Search or any change to the Selection empties it.

### Keyboard

The whole screen works without a mouse:

- **Tab** moves between the tabs, the filter box, the list and the buttons; **Up** and **Down** move between rows. A white frame shows the focus.
- **Space** or **Enter** ticks the focused row of the tree; **Right** expands a Group and **Left** collapses it.
- **Space**, **Enter** or **Delete** on a Visited or Skipped row acts as its ✕.

## Configuration

The config file is `config/adventurerscompass.json`, created with the defaults on first launch. It has two blocks: `common` for the server rules and `client` for the HUD. Each option comes with a `…Comment` field that explains it; the mod rewrites those fields on every launch, so editing them does nothing.

```json
{
  "common": {
    "allowTeleport": false,
    "displayCoordinates": true,
    "maxRadius": 2500,
    "compassDurability": 3,
    "defaultXpLevel": 5,
    "perStructureXpLevels": {},
    "defaultDimensionXpMultiplier": 2.0,
    "dimensionXpMultipliers": { "minecraft:overworld": 1.0 },
    "hiddenStructures": [],
    "visitedMessage": true,
    "disabledDimensions": [],
    "targetVisitedSound": true
  },
  "client": {
    "hudEnabled": true,
    "displayWithChatOpen": true,
    "translateStructureNames": true,
    "overlayLineOffset": 1,
    "overlaySide": "LEFT"
  }
}
```
(comment fields left out)

- The file is read when the game starts: restart the game or the server after editing it.
- An option, or a whole block, missing from the file gets its default value, and the mod writes it back into the file.
- In multiplayer, the server's `common` block applies to everyone: clients get its values, so the HUD coordinates and the wear bar follow the server. A client's own `common` block only applies to its single-player worlds. A dedicated server ignores the `client` block.

### Server options (`common`)

| Option | Default | |
|---|---|---|
| `allowTeleport` | `false` | Shows a Teleport button to ops (or the single player with cheats on) and creative players. Teleporting never crosses dimensions, does not wear the compass and does not mark the Target Visited |
| `displayCoordinates` | `true` | Shows the Target's X and Z on a second HUD line. The distance is always shown |
| `maxRadius` | `2500` | Maximum horizontal distance, in blocks, from where the search started to a Structure it can find; about half a day's walk. A larger radius makes searches that find nothing more expensive for the server |
| `compassDurability` | `3` | Targets a compass can find, by Search or Pass, before it is worn out. `0` disables wear |
| `defaultXpLevel` | `5` | Levels a Structure costs when `perStructureXpLevels` has no entry for it, before the dimension multiplier. `0` makes searches free |
| `perStructureXpLevels` | `{}` | Levels per Structure id, replacing `defaultXpLevel`. Wildcards allowed; the first matching entry wins. `0` makes the Structure free |
| `defaultDimensionXpMultiplier` | `2` | Cost multiplier in a dimension that `dimensionXpMultipliers` does not list |
| `dimensionXpMultipliers` | `{"minecraft:overworld": 1}` | Cost multiplier per dimension id. Decimals allowed |
| `hiddenStructures` | `[]` | Structures never listed or searched: ids with wildcards, or tags starting with `#` |
| `visitedMessage` | `true` | Message above the hotbar when an Instance becomes Visited |
| `disabledDimensions` | `[]` | Dimension ids where the compass does not work: the screen lists nothing, Search and Pass are refused and no XP is taken. A Target found elsewhere is kept |
| `targetVisitedSound` | `true` | Soft sound when the Target of a compass the player carries becomes Visited |

In ids, `*` matches any number of characters and `?` matches one: `minecraft:*village*` matches every vanilla village, `*:*tower*` every tower from any mod. Run `/locate structure` and press Tab to list the Structure ids of a world.

### Client options (`client`)

| Option | Default | |
|---|---|---|
| `hudEnabled` | `true` | Shows the Target, or the search state, on the HUD while the compass is in either hand |
| `displayWithChatOpen` | `true` | Keeps the HUD shown while the chat is open |
| `translateStructureNames` | `true` | Uses the language files for Structure and Group names. `false` shows readable ids instead |
| `overlaySide` | `LEFT` | Side of the screen for the HUD: `LEFT` or `RIGHT` |
| `overlayLineOffset` | `1` | How many text lines down from the top of the screen the HUD starts |

### XP cost

A search costs `ceil(Structure cost × multiplier)` levels:

- **Structure cost**: its `perStructureXpLevels` entry, else `defaultXpLevel`.
- **Multiplier**: the `dimensionXpMultipliers` entry of the dimension the player searches in, else `defaultDimensionXpMultiplier`.

With the defaults, a village costs 5 levels in the Overworld (5 × 1) and a fortress 10 in the Nether (5 × 2). With a multiplier of `1.5`, 5 levels become 8 (7.5 rounded up).

To start a Search or a Pass, the player needs the cost of the most expensive Structure the Selection can find in this dimension; the screen shows "(≤ N levels)" when the costs differ. Levels are taken only when a Target is found, at the cost of the Structure found. A cancelled search, or one that finds nothing, is free. If the player spends levels during the search, only the levels left are taken. Creative players pay nothing.

`dimensionXpMultipliers` replaces the default map as a whole. If you set it, keep the `minecraft:overworld` entry, or the Overworld falls back to `defaultDimensionXpMultiplier`:

```json
"dimensionXpMultipliers": { "minecraft:overworld": 1, "minecraft:the_nether": 2, "minecraft:the_end": 3 }
```

### Hiding Structures

A Hidden Structure is never listed in the screen and never searched. There are two ways to hide one:

- **Config**: list ids (wildcards allowed) or tags in `hiddenStructures`:
  ```json
  "hiddenStructures": ["minecraft:stronghold", "minecraft:*village*", "#minecraft:village"]
  ```
- **Datapack or mod**: add the Structure to the common tag `#c:hidden_from_locator_selection`, which always hides. The file is `data/c/tags/worldgen/structure/hidden_from_locator_selection.json` in a datapack or a mod's resources:
  ```json
  { "values": ["minecraft:ancient_city", "examplemod:secret_lab"] }
  ```

Hiding a Structure that players already have in a Selection keeps it there: the screen counts it in "N ignored here", with a "Hidden on this server" tooltip, and Search is greyed if nothing else in the Selection can be searched. A Target found before the change is kept.

### Groups and names

The tree groups Structures by their **structure set**, the registry entry that spaces them in the world: `minecraft:villages` holds the five village kinds, `minecraft:ruined_portals` the seven ruined portals. A Structure in no set lands in "Other Structures". A Group with a single Structure shows as that Structure.

Names come from the language files:

- Structure: `structure.<namespace>.<path>`, e.g. `"structure.examplemod.sky_tower": "Sky Tower"`.
- Group: `group.<namespace>.<path>`, with the structure set's id, e.g. `"group.examplemod.towers": "Towers"`.

When a key is missing, a Structure shows its id made readable (`examplemod:sky_tower` → "Sky Tower"), and a Group takes the name of the Structure with the same id, else its own id made readable. A mod, or a resource pack for someone else's mod, can name its Structures and Groups by adding these keys to `assets/<its namespace>/lang/<language>.json`.

### Examples

**Plain locator**: free, no wear, teleport for ops, large radius.
```json
"common": {
  "allowTeleport": true,
  "maxRadius": 5000,
  "compassDurability": 0,
  "defaultXpLevel": 0
}
```

**Friendly places only**: villages and trail ruins are cheap, strongholds and ancient cities are hidden, and the End is closed to the compass.
```json
"common": {
  "perStructureXpLevels": { "minecraft:village_*": 2, "minecraft:trail_ruins": 2 },
  "hiddenStructures": ["minecraft:stronghold", "minecraft:ancient_city"],
  "disabledDimensions": ["minecraft:the_end"]
}
```

**Expensive Nether**: everything costs 3 levels in the Overworld and 9 in the Nether, and a compass lasts 10 finds.
```json
"common": {
  "compassDurability": 10,
  "defaultXpLevel": 3,
  "dimensionXpMultipliers": { "minecraft:overworld": 1, "minecraft:the_nether": 3 }
}
```

Each example lists only the options to change in the `common` block of your file; the others keep their values.

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
