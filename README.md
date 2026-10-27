# Adventurer's Compass

Adventurer's Compass is a Minecraft (Fabric) mod that allows you to search for and locate structures anywhere in the world.

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
