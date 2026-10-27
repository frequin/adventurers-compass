# Adventurer's Compass

A compass that points a nomadic player at the next structure worth reaching, without ever relying on a home base.

## Language

**Structure**:
A kind of generated feature that can appear in a world (a village, a stronghold, a modded tower), identified by its registry id.
_Avoid_: Structure type

**Group**:
A named set of Structures that share a placement in world generation, as listed in the compass screen.
_Avoid_: Category, structure set

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
The state of an Instance a player has been to, which searches skip; held per player and per world, and reversible.
_Avoid_: Explored, discovered, done

**Current dimension**:
The dimension the player stands in; by default the only one searched and listed.
