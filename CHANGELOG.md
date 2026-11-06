## Version 1.0.0
#### Fabric 26.3
- First release of Adventurer's Compass, based on Explorer's Compass 2.5.2 by ChaosTheDude
- Renamed the mod (mod id `adventurerscompass`), so it can be installed alongside Explorer's Compass
- Nomad mode: the compass holds a Selection of several Structures and Groups, composed in a tree screen (text and `@mod` filter, removable chips, Clear). Search finds the nearest Instance of the whole Selection in the Current dimension
- Visited: an Instance becomes Visited when you stand in one of its pieces, or with "Mark Visited" on the Target. Searches skip Visited Instances. Visited is held per player and per world, kept on death, listed in the screen and can be unmarked one by one
- Pass: skips the Target for now and searches again. Skipped Instances are cleared by a new Search or a Selection change
- Changing the Selection cancels a running search and keeps the Target. "Clear Target" in the screen removes the Target for free, without wear, even on a broken compass, and leaves the Selection, Skipped and a running search alone
- The Target keeps its dimension: in another dimension the needle spins and the HUD says so; back in its dimension it points again with no new search
- One-line HUD: "Structure · distance" or the search state, with an optional coordinates line
- The "nothing found" HUD message lasts 5 seconds, then the previous Target is back. It also goes for good when the compass leaves the hand or the player disconnects
- XP is paid only when a Target is found, at the found Structure's cost times the Current dimension's multiplier. Cancelled searches and searches that find nothing are free
- Wear: each found Target (Pass included) wears the compass. Crafting a worn compass with a compass repairs it and keeps its Selection, Target and Skipped
- Searches are spread over server ticks (about 5 ms per tick shared between players), one search per player, so dedicated servers keep their TPS
- Structures can be Hidden by the common tag `#c:hidden_from_locator_selection` or by the `hiddenStructures` config
- New translations: complete English and French. Other languages only keep the strings that still exist

#### Breaking changes
- The config file is not compatible with Explorer's Compass and is not migrated: delete `config/adventurerscompass.json` to get the new defaults. Unknown old keys are ignored and missing keys take their default
- Compasses from Explorer's Compass or earlier builds are not migrated: their search data is dropped
- New nomadic defaults: `maxRadius` 2500 (was 10000), about half a day's walk, `allowTeleport` false (was true; when on, only ops and creative players can teleport, never across dimensions), `compassDurability` 3 (was 0), `defaultXpLevel` 5 (was 0, and the 3-level cap is gone), `defaultDimensionXpMultiplier` 2 with `dimensionXpMultipliers` `{"minecraft:overworld": 1}`
- `structureBlacklist` is renamed `hiddenStructures` and also accepts `#tag` entries
- Removed options: `maxNextSearches`, `maxSamples`
- The screen's "Sort By" button is gone: the tree lists Groups and their Structures alphabetically. The old sorts by source, Group and dimension no longer fit a tree that already nests Structures in their Groups and shows only the Current dimension; filtering by source is done with `@mod` in the search box
- Crouch-using the compass no longer clears or cancels anything: using it always opens the screen, where "Search" turns into "Cancel" while a search runs
- New options: `disabledDimensions`, `visitedMessage`, `targetVisitedSound` (server), `hudEnabled` (client)

The changelog of the original mod is kept in [CHANGELOG-explorers-compass.md](CHANGELOG-explorers-compass.md).
