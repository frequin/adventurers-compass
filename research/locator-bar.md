# Barre de localisation vanilla : y afficher une Target (Fabric 26.3)

Ticket : [#13](https://github.com/frequin/adventurers-compass/issues/13).

Question : sur Minecraft 26.3 / Fabric, peut-on afficher dans la barre de localisation (Locator Bar) un point arbitraire, la Target d'une boussole, pour un seul joueur ? Par quelle API, avec quelles limites ?

## Réponse courte

Oui, côté serveur, sans rien de spécial côté client. Le serveur peut envoyer à un seul joueur un paquet vanilla `ClientboundTrackedWaypointPacket` qui décrit une position fixe (`TrackedWaypoint.setPosition(uuid, icon, blockPos)`). Le client vanilla l'affiche comme n'importe quel autre point. Rien dans le paquet ne le relie à une entité ou à une dimension : c'est au mod de le retirer au changement de dimension et de respecter la gamerule `locator_bar`, que le gestionnaire vanilla ne fait respecter que pour ses propres connexions. Fabric API (0.160.6+26.3) ne fournit aucune API pour les waypoints. Tout passe par les classes vanilla.

## Sources

Source primaire unique : le jar Minecraft 26.3 du cache Loom du projet, avec les noms officiels Mojang (26.x n'est plus obfusqué), décompilé avec Vineflower 1.12.0 (présent dans le cache Gradle) :

- `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar`
- Fabric API : les 46 jars de modules `net.fabricmc.fabric-api` du cache Gradle, dont `fabric-api-0.160.6+26.3.jar` et `fabric-rendering-v1-27.0.14`. Aucune classe dont le nom contient « waypoint » ou « locatorbar ».

Les références ci-dessous sont des noms de classes et de méthodes de ce jar.

## Architecture vanilla

| Rôle | Classe | Côté |
|---|---|---|
| Source d'un point (entité ou autre) | `net.minecraft.world.waypoints.WaypointTransmitter` (étend `Waypoint`) | commun |
| Lien source → un joueur | `WaypointTransmitter.Connection` (`connect`, `disconnect`, `update`, `isBroken`) | serveur |
| Gestionnaire par dimension | `net.minecraft.server.waypoints.ServerWaypointManager`, une instance par `ServerLevel` (`ServerLevel.getWaypointManager()`) | serveur |
| Paquet | `net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket(Operation, TrackedWaypoint)`, opérations `TRACK`, `UNTRACK`, `UPDATE` | réseau |
| Point reçu | `net.minecraft.world.waypoints.TrackedWaypoint`, variantes `VEC3I`, `CHUNK`, `AZIMUTH`, `EMPTY` | commun |
| Stockage client | `net.minecraft.client.waypoints.ClientWaypointManager`, champ de `ClientPacketListener` (`getWaypointManager()`) | client |
| Rendu | `net.minecraft.client.gui.contextualbar.LocatorBar` | client |
| Styles d'icône | `net.minecraft.client.resources.WaypointStyleManager` / `WaypointStyle`, ressources `assets/<ns>/waypoint_style/*.json` | client |

Seul `LivingEntity` implémente `WaypointTransmitter` dans le vanilla : il est transmetteur si l'attribut `waypoint_transmit_range` est supérieur à 0. Un `ServerPlayer` reçoit si `waypoint_receive_range` est supérieur à 0. Les deux valent `6.0E7` par défaut pour les joueurs (`Player.createAttributes`).

## Afficher un point fixe pour un seul joueur

### Option A, paquet direct (la plus simple)

`ClientboundTrackedWaypointPacket` expose des fabriques publiques :

- `addWaypointPosition(UUID, Waypoint.Icon, Vec3i)` : opération `TRACK`, type `VEC3I`
- `updateWaypointPosition(UUID, Waypoint.Icon, Vec3i)` : opération `UPDATE`
- `removeWaypoint(UUID)` : opération `UNTRACK`
- et les variantes `...Chunk(ChunkPos)` et `...Azimuth(float)`

On l'envoie avec `serverPlayer.connection.send(packet)` : seul ce joueur le reçoit. Côté client, `ClientWaypointManager` range les points par identifiant (`Either<UUID, String>`). `TRACK` ajoute ou remplace, `UPDATE` modifie un point existant (seulement si le type est le même, sinon un avertissement « Unsupported Waypoint update operation » est journalisé), `UNTRACK` retire.

Points à surveiller :

- **Identifiant** : utiliser un UUID propre au mod, par exemple `UUID.nameUUIDFromBytes("adventurerscompass:target:" + …)`, qui ne corresponde à aucune entité. `TrackedWaypoint.Vec3iWaypoint.position` cherche l'entité qui porte cet UUID dans le niveau client : si elle existe et se trouve à moins de 3 blocs (distance de Manhattan) du vecteur, elle utilise la position des yeux de l'entité. `LocatorBar` masque aussi le point dont l'UUID est celui de l'entité caméra.
- **Hors de `ServerWaypointManager`** : ce point ne lui est pas connu. `breakAllConnections()` (gamerule désactivée) et `removePlayer()` (changement de dimension, déconnexion) ne le retirent donc pas. Le mod doit envoyer `removeWaypoint` lui-même (voir plus bas).

### Option B, implémenter `WaypointTransmitter`

On peut écrire un transmetteur non-entité et l'enregistrer avec `serverLevel.getWaypointManager().trackWaypoint(t)`. `ServerWaypointManager.createConnection` appelle `t.makeWaypointConnectionWith(player)` pour chaque joueur de la dimension. Pour un affichage réservé à un joueur, il suffit de renvoyer `Optional.empty()` pour tous les autres. `updatePlayer` est rappelé à chaque mouvement du joueur (`Entity.move`, `ServerChunkCache.move`), ce qui appelle `isBroken()` et `update()` sur la connexion. Avantage : la gamerule (`isLocatorBarEnabledFor`) et `breakAllConnections()` sont respectées automatiquement. Inconvénients : il faut gérer à la main les ajouts et retraits par `ServerLevel` (un gestionnaire par dimension), et l'objet est stocké dans un `HashSet`, donc `equals` et `hashCode` doivent rester stables. Pour un point fixe, cette option apporte peu par rapport à l'option A plus une vérification de gamerule.

### Option C, client seul

Un mod client peut injecter un point localement, sans serveur : `Minecraft.getInstance().getConnection().getWaypointManager().trackWaypoint(TrackedWaypoint.setPosition(uuid, icon, pos))` (méthodes publiques). Mais le client ne connaît pas la gamerule `locator_bar` (elle n'est pas synchronisée, voir plus bas), et la Target est calculée côté serveur dans ce mod. Ça ne marcherait donc que si le client connaît déjà la position (par exemple via le `SyncPacket` existant), et cela demande le mod sur le client.

## Icône et couleur

`Waypoint.Icon` a deux champs publics et modifiables : `style` (`ResourceKey<WaypointStyleAsset>`, par défaut `minecraft:default`) et `color` (`Optional<Integer>`, RGB). Ils sont sérialisés dans le paquet (`Icon.STREAM_CODEC`).

- **Couleur** : n'importe quelle valeur RGB. Sans couleur, `LocatorBar` dérive une couleur du `hashCode()` de l'UUID (luminosité 0.9). Choisir l'UUID ne donne donc pas une couleur contrôlée : mieux vaut la fixer explicitement.
- **Style** : les clés sont résolues **côté client** par `WaypointStyleManager.get`, qui renvoie `MISSING` (texture manquante) si le style est inconnu. Le vanilla en fournit deux : `minecraft:default` (sprites `default_0` à `default_3`) et `minecraft:bowtie` (`near_distance` 64, sprite `bowtie` de près puis les sprites par défaut). Un style propre au mod (`assets/adventurerscompass/waypoint_style/target.json` et sprites sous `textures/gui/sprites/hud/locator_bar_dot/`) ne s'affiche que sur un client qui a ces ressources (mod installé ou pack de ressources). Pour un client vanilla, il faut s'en tenir à `default` ou `bowtie`.
- **Sprite selon la distance** : `WaypointStyle.sprite(distance)` prend le premier sprite sous `near_distance` (128 par défaut) et le dernier au-delà de `far_distance` (332 par défaut), avec une interpolation entre les deux. Le point rétrécit donc avec la distance, et la distance est calculée en 3D depuis la caméra.
- `/waypoint modify <entité> color|style` ne vise que des entités (`EntityArgument`). Ce n'est pas une piste pour une position fixe.

## Portée et comportement à distance

- **Rendu client** : pas de distance maximale pour un point `VEC3I`. Les coordonnées passent en VarInt. Le point n'est dessiné que si son angle de lacet par rapport à la caméra est dans ±60° (`LocatorBar.VISIBLE_DEGREE_RANGE`). Il n'y a pas d'indicateur « derrière vous ». Une flèche haut ou bas (`PitchDirection`) apparaît quand le point sort verticalement de l'écran, calculée à partir de la projection réelle du point.
- **Dégradation vanilla pour les entités** : ce choix appartient au serveur, pas au client. `LivingEntity.makeWaypointConnectionWith` crée une `EntityBlockConnection` (position exacte) si le chunk de la source est dans la distance de vue du receveur, une `EntityChunkConnection` (centre du chunk) au-delà, puis une `EntityAzimuthConnection` (simple angle) au-delà de `REALLY_FAR_DISTANCE = 332` blocs. Rien n'oblige un mod à reproduire ce comportement : on peut envoyer la position exacte de la Target à n'importe quelle distance.
- **Attributs de portée** : `doesSourceIgnoreReceiver` compare `min(transmit_range de la source, receive_range du receveur)` à la distance. Cela ne concerne que les transmetteurs `LivingEntity` et les connexions gérées par `ServerWaypointManager`. Un paquet direct n'y est pas soumis. Pour bien faire, le mod peut tout de même respecter `waypoint_receive_range` du joueur (`ServerPlayer.isReceivingWaypoints()`).
- **Concurrence avec d'autres barres** : `Hud.nextContextualInfoState` n'affiche la barre que si `ClientWaypointManager.hasWaypoints()`. La barre d'XP la remplace pendant 100 ticks après un changement d'XP (`willPrioritizeExperienceInfo`), et la barre de saut de monture a la priorité quand elle est active. Ajouter la Target fait donc apparaître la barre de localisation à la place de la barre d'XP, même en solo ou en l'absence d'autres joueurs.

## Dimension

- `ServerWaypointManager` existe par `ServerLevel`. Quand un joueur quitte un niveau, `ServerLevel.EntityCallbacks.onTrackingEnd` appelle `removePlayer`, qui envoie `UNTRACK` pour chaque connexion gérée.
- `TrackedWaypoint` ne contient **aucune dimension**. Le client dessine le point dans le niveau où il se trouve. `ClientWaypointManager` est un champ final de `ClientPacketListener`, et rien ne le vide au respawn ni au changement de dimension : la seule référence dans `ClientPacketListener` est `handleWaypoint`.
- Conséquence pour l'option A : le mod doit envoyer `removeWaypoint` quand le joueur change de dimension (par exemple sur `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` de Fabric API, module `fabric-entity-events-v1` 6.0.4), puis le renvoyer seulement si la Target est dans la nouvelle dimension. Il faut aussi le renvoyer après une reconnexion : un nouveau `ClientPacketListener` démarre vide.

## Gamerule `locator_bar` désactivée

- Déclaration : `GameRules.LOCATOR_BAR = registerBoolean("locator_bar", GameRuleCategory.PLAYER, true)`. Lue par niveau : `ServerWaypointManager.isLocatorBarEnabledFor(player)` lit `player.level().getGameRules()`.
- Au changement (`MinecraftServer.onGameRuleChanged`) : si la gamerule passe à `false`, `breakAllConnections()` envoie `UNTRACK` pour toutes les connexions gérées de chaque niveau. Si elle passe à `true`, `updatePlayer` est appelé pour chaque joueur afin de recréer les connexions. Tant qu'elle vaut `false`, `createConnection` et `updateConnection` ne font rien.
- La gamerule n'est pas envoyée au client : aucune classe client ne la lit (seuls `MinecraftServer`, `ServerWaypointManager` et `GameRules` y font référence ; la chaîne trouvée dans `LocatorBar` correspond aux constantes de sprites `LOCATOR_BAR_*`). Avec la gamerule désactivée, le client garde simplement un gestionnaire vide, et la barre disparaît puisque `hasWaypoints()` vaut `false`.
- Un paquet envoyé directement **contourne** la gamerule : le client l'afficherait. Le mod doit lire `GameRules.LOCATOR_BAR` lui-même avant d'envoyer, et réagir au changement (par exemple avec un mixin sur `MinecraftServer.onGameRuleChanged`, ou en vérifiant à chaque tick ou mise à jour de la Target). Avec l'option B, c'est automatique.

## Serveur dédié et client vanilla sans le mod

- Les classes utilisées par les options A et B (`ClientboundTrackedWaypointPacket`, `TrackedWaypoint`, `Waypoint.Icon`, `WaypointTransmitter`, `ServerWaypointManager`) n'ont pas d'annotation `@Environment(CLIENT)`. Elles existent donc sur serveur dédié. Seuls `ClientWaypointManager`, `LocatorBar`, `WaypointStyle` et `WaypointStyleManager` sont réservés au client.
- Le paquet est un paquet vanilla du protocole de jeu (`GamePacketTypes.CLIENTBOUND_WAYPOINT`). Un client vanilla 26.3 sans le mod le décode et l'affiche, avec deux limites : seuls les styles `minecraft:default` et `minecraft:bowtie` existent chez lui, et il ne verra la Target que si le serveur la lui envoie.
- Le point s'affiche comme un point de joueur ou de mob, sans nom ni info-bulle : la barre vanilla ne montre ni texte ni distance chiffrée.

## Recommandation pour le mod

1. Côté serveur, pour chaque joueur qui a une Target : envoyer `addWaypointPosition(targetUuid, icon, targetPos)`, avec un UUID déterministe par joueur, une couleur fixe et le style `minecraft:default`, ou un style propre au mod si le client l'a.
2. Envoyer `removeWaypoint(targetUuid)` à la perte de la Target, quand la boussole n'est plus tenue (selon la règle choisie), au changement de dimension, et quand `locator_bar` passe à `false`. Renvoyer le point au retour en jeu et quand la gamerule repasse à `true`.
3. Ne rien envoyer si `GameRules.LOCATOR_BAR` vaut `false` dans le niveau du joueur, ou si `!player.isReceivingWaypoints()`.

## Questions ouvertes

- Rien n'a été testé en jeu : tout ce qui précède vient de la lecture du code décompilé.
- Il n'y a pas de documentation Fabric ni de javadoc Mojang publiée pour ces classes. Ce sont des internes de Minecraft, susceptibles de changer d'une version à l'autre.
