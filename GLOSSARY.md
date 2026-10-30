# Adventurer's Compass

A compass that points a nomadic player at the next structure worth reaching, without ever relying on a home base.

## Language

**Structure**:
A kind of generated feature that can appear in a world (a village, a stronghold, a modded tower), identified by its registry id.
_Avoid_: Structure type

**Group**:
A named set of Structures that share a placement in world generation, as listed in the compass screen.
_Avoid_: Category, structure set

**Hidden**:
The state of a Structure the compass never lists or searches for, set by whoever installs or runs the game rather than by the player.
_Avoid_: Blacklisted, disabled, excluded

**Instance**:
One concrete occurrence of a Structure in a world, identified by its position.
_Avoid_: Location, structure (when one occurrence is meant)

**Selection**:
The set of Structures and Groups ticked on a compass; a search looks for the nearest Instance of any of them.
_Avoid_: Filter, profile, target list

**Target**:
The Instance the compass currently points at.
_Avoid_: Destination, result

**Visited**:
The state of an Instance a player has stood inside, or has marked by hand as their Target, which searches skip; held per player and per world, shared by all their compasses, and reversible.
_Avoid_: Explored, discovered, done

**Skipped**:
The state of an Instance a player chose to pass over without visiting it; held by one compass and cleared when a fresh search starts or the Selection changes.
_Avoid_: Ignored, excluded, Visited (when the player has not been there)

**Current dimension**:
The dimension the player stands in; the only one whose Structures are listed and searched.
