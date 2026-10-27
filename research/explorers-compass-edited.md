# Que reprendre d'Explorer's Compass Edited ?

Ticket : [#2](https://github.com/frequin/adventurers-compass/issues/2). Recherche faite le 2026-10-27.
Vocabulaire : voir `GLOSSARY.md` (Structure, Group, Instance, Selection, Target, Visited, Current dimension).

## Résumé

- **Explorer's Compass Edited** (ECE) est un fork Forge/NeoForge d'Explorer's Compass par **MCMostWolf**. Il ajoute quatre choses : sauter les Instances déjà trouvées, un bouton pour vider cet historique, un HUD qui montre les 2 dernières recherches, et la création automatique d'un waypoint Xaero's Minimap. Il ajoute aussi une option CustomModelData par Structure pour les packs de ressources.
- **Licence : CC BY-NC-SA 4.0**, la même que l'original et que ce fork. Le code peut légalement être repris dans Adventurer's Compass avec attribution (MCMostWolf + lien + mention des changements), en restant non commercial et sous CC BY-NC-SA 4.0.
- **En pratique : on reprend les idées, pas le code.** Le code est en Forge 1.20.1 (ForgeConfigSpec, `NetworkEvent`, NBT `getTag()`), le source public s'arrête à la 1.20.1 alors que le jar le plus récent est en NeoForge 1.21.1, le master publié ne compile pas tel quel, et la mécanique « déjà trouvé » a des défauts (mémoire seulement, pas de dimension, globale au serveur).
- Pour un joueur nomade, l'idée qui compte est **« ne pas me renvoyer vers une Instance déjà faite »**. Elle correspond à notre **Visited**, mais ECE l'implémente comme « déjà pointé », pas comme « déjà visité ». Le HUD d'historique est aussi une bonne idée. La base actuelle (Explorer's Compass upstream) couvre déjà en partie « Instance suivante » (`SearchForNextPacket`, `PREV_POS`).

## Sources primaires

| Source | URL |
| --- | --- |
| Page CurseForge (via l'API cfwidget, la page directe étant protégée par Cloudflare) | https://www.curseforge.com/minecraft/mc-mods/explorers-compass-edited, https://api.cfwidget.com/minecraft/mc-mods/explorers-compass-edited |
| Dépôt source (fork GitHub de `MattCzyr/ExplorersCompass`) | https://github.com/MCMostWolf/ExplorersCompassEdited (branche `master`, commit `4565be6`, 2025-11-01) |
| Fichier de licence du dépôt | https://github.com/MCMostWolf/ExplorersCompassEdited/blob/master/LICENSE.md |
| README du dépôt | https://github.com/MCMostWolf/ExplorersCompassEdited/blob/master/README.md |
| Issues du dépôt | https://github.com/MCMostWolf/ExplorersCompassEdited/issues |
| Texte de la licence | https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode |

Aucune page Modrinth trouvée (la recherche dans l'API Modrinth renvoie 0 résultat).

### Faits sur le projet

- Propriétaire CurseForge : `MCMostWolf`, projet 1203888, créé le 2025-02-20, environ 11,2 M de téléchargements. Le champ licence de CurseForge est **vide** ; la licence vient donc du dépôt (cfwidget).
- Fichiers publiés : Forge 1.16.5, 1.18.2, 1.19.2, 1.20.1 (dernier `1.4.0`, 2025-11-01) et NeoForge 1.21.1 (dernier `3.0.4`, 2026-06-12) (cfwidget).
- Branches du dépôt : `master` (Forge 1.20.1, `gradle.properties` : `minecraft_version=1.20.1`, `forge_version=47.0.1`), `1.16.5-forge`, `1.18.2-forge`. **Il n'y a pas de source publiée pour NeoForge 1.21.1.**
- Le master publié ne compile pas tel quel : `ExplorersCompassScreen` appelle `new CleanFoundPacket(player.getUUID())` alors que `CleanFoundPacket` n'a que les constructeurs `()` et `(FriendlyByteBuf)`. Les clés de langue `string.explorerscompass.cleanMap` / `have_searched` manquent dans tous les fichiers `lang/`. Le source ne correspond donc pas exactement aux jars distribués.

## Fonctionnalités, intérêt pour le nomade, statut de réutilisation

« Statut » : **Déjà là** = déjà dans notre base Explorer's Compass upstream ; **Idée** = à reprendre comme idée et à réécrire pour Fabric 26.3 ; **Non** = à ne pas reprendre.

| # | Fonctionnalité ECE (preuve) | Intérêt pour un joueur nomade | Statut |
| --- | --- | --- | --- |
| 1 | **Sauter les Instances déjà trouvées.** `SearchWorkerManager.foundChunks` (une `List<Pair<UUID, ChunkPos>>` statique) est rempli à chaque succès dans `StructureSearchWorker.succeed`. `getStructureGeneratingAt` ignore les chunks déjà dans la liste pour ce joueur. Relancer une recherche pointe donc vers l'Instance suivante. | **Fort.** C'est le cœur de la boucle nomade : ne jamais être renvoyé vers un village déjà pillé. Mais ECE marque une Instance comme « vue » dès qu'elle est *trouvée*, pas quand le joueur y *est allé*, ce qui ne correspond pas à notre **Visited**. Défauts relevés dans le code : la liste vit en mémoire seulement (perdue au redémarrage du serveur, et réinitialisée par le constructeur de `SearchWorkerManager`) ; la clé ne contient pas la dimension (`ChunkPos` seul) ; la liste n'est pas filtrée par Structure. | **Idée.** Remplacer par un état **Visited** persistant, par joueur, par monde et par dimension, et réversible (cf. glossaire). Notre base a déjà « Instance suivante » via `PREV_POS` / `SearchForNextPacket` / `maxNextSearches`, mais limité à la Target courante et stocké dans l'item. |
| 2 | **Bouton « vider le cache »** (`cleanMapButton` dans `ExplorersCompassScreen` envoie `CleanFoundPacket`, qui retire les entrées du joueur) + option `cleanCache` (par défaut `false`) qui vide l'historique quand une recherche échoue (`StructureSearchWorker.fail`). | **Moyen.** Permet de revenir en arrière. Mais c'est du tout ou rien : on ne peut pas retirer une seule Instance. Le README annonce un retour automatique à l'Instance la plus proche en cas d'échec, alors que l'option correspondante est désactivée par défaut. | **Idée.** Préférer « marquer / démarquer Visited » Instance par Instance, plus un « tout réinitialiser » secondaire. |
| 3 | **HUD d'historique** : `ClientEventHandler.renderSearchHistory` affiche, dans une colonne à côté du statut, les 2 recherches précédentes (Structure + coordonnées, ou « Not found »). Les données sont stockées dans l'item (`HaveSearched` : `StructureKey1/2`, `FoundX1/2`, …). Option client `showHaveFound` (par défaut `true`). | **Moyen.** Aide à garder en tête les Instances repérées mais pas encore atteintes. Limites : 2 entrées en dur, stockage dans le NBT de l'item (un autre compas a un autre historique), et au moins une faute de frappe dans l'initialisation (`FoundZ2` écrit deux fois, `FoundZ1` jamais initialisé). | **Idée.** Si on la reprend, la dériver de l'état Visited / des Targets récentes, et non d'un historique figé à 2 entrées dans l'item. |
| 4 | **Waypoint Xaero's Minimap automatique** à chaque succès (`NewWayPointPacket` → `NewWayPoint.addPoint`, qui utilise les classes internes `xaero.common.*`). Option client `newWayPoint` (par défaut `true`). | **Moyen à fort** pour qui utilise Xaero : la Target reste sur la carte une fois le compas rangé. Mais c'est **fragile** : les issues #4, #5 et #7 signalent crashs et régressions à chaque mise à jour de Xaero (#7 : crash puis monde qui ne se charge plus tant que l'option reste active). | **Idée, plus tard.** Si on le fait : intégration optionnelle, désactivée en cas d'erreur, et de préférence via une API publique (ou une intégration JourneyMap/Xaero séparée). Ne pas reprendre l'appel aux classes internes. |
| 5 | **CustomModelData par Structure** : `config/explorerscompass/config.json` associe une Structure à une valeur, appliquée à l'item en cas de succès (`BetterUI`, `ExplorersCompassItem.succeed`). Option `customResource` (par défaut `false`). | **Faible** pour le gameplay (utile aux packs de ressources). Mécanique obsolète depuis les composants d'item (1.20.5+) ; en 26.3 ce serait une propriété de modèle d'item. | **Non** (hors sujet nomade). |
| 6 | **Interface « plus visible »** (`TransparentButton`, `TransparentTextField`, mise en page de `StructureSearchList` modifiées). | **Faible.** Choix esthétique, sans effet sur la Selection. | **Non.** |
| 7 | **Recherche par Group** (`searchForGroup`, bouton « Search for Group »), tri (nom / source / Group / dimension), filtre texte avec préfixe `@source`, `structureBlacklist` avec jokers, choix du côté du HUD (`overlaySide`), coordonnées / distance sur le HUD (`displayCoordinates`). | **Fort** (Selection, filtres, HUD). Mais ce n'est pas un apport d'ECE : tout cela vient d'Explorer's Compass upstream. | **Déjà là** (vérifié dans notre base : `sorting/*`, `GroupSorting`, `OverlaySide`). |
| 8 | **Sélection multi-structures libre** (cocher plusieurs Structures de Groups différents). | **Fort.** Absent d'ECE : on ne peut chercher qu'une Structure ou un Group à la fois (`CompassSearchPacket(key, List.of(key))` ou la liste du Group). | **Rien à reprendre.** À concevoir nous-mêmes (cf. **Selection**). |
| 9 | **Option `couldFoundAlexCaves`** (par défaut `true`). | Nul. L'option est déclarée mais jamais lue dans le source publié (contournement de crash, issue #2 d'ECE). | **Non.** |

### Défauts d'ECE qui n'apportent rien au nomade

- Aucune Selection multi-structures, aucun filtre « Current dimension » au-delà de ce que fait l'upstream, aucun état persistant.
- La recherche ne démarre pas « loin du joueur » : le README dit « Keep the player's surroundings from being searched », mais aucun code du master ne fait cela. Le seul effet réel est le saut des chunks déjà trouvés (ligne 1).
- Issue #6 d'ECE (Instances à l'ouest manquées) : l'upstream a corrigé un bug semblable en 1.21.11 (« instances west of the search starting position would be missed », `CHANGELOG-explorers-compass.md`).

## Licence : que peut-on faire ?

- `LICENSE.md` du dépôt = texte intégral **CC BY-NC-SA 4.0** ; le README dit « This mod is available under the Creative Commons Attribution-NonCommercial ShareAlike 4.0 International License » ; `gradle.properties` : `mod_license=CC BY-NC-SA 4.0`. Le champ licence de CurseForge est vide, donc la source qui fait foi est le dépôt.
- Adventurer's Compass est lui-même sous CC BY-NC-SA 4.0 (`LICENSE.md`, `README.md`). La clause ShareAlike (section 3(b)) impose que l'Adapter's License soit la même licence (ou une licence compatible BY-NC-SA) : **c'est le cas, la réutilisation du code est donc autorisée** aux conditions suivantes :
  - **BY** : créditer MCMostWolf, mettre un lien vers la licence et le dépôt, et indiquer les modifications (section 3(a)) ;
  - **NC** : pas d'usage commercial ;
  - **SA** : rester sous CC BY-NC-SA 4.0.
- Réserves pratiques :
  - Le seul code disponible est Forge 1.20.1. Le NeoForge 1.21.1 est publié en jar sans source, et décompiler le jar pour réutiliser le code est à éviter.
  - Le code est très lié à Forge et aux NBT d'item pré-1.20.5. Un portage vers Fabric 26.3 serait de toute façon une réécriture.
  - L'intégration Xaero dépend des classes internes d'un mod tiers dont le source n'est pas public (issue #7).
- **Recommandation : s'inspirer des idées (lignes 1 à 4 du tableau), réécrire, et ajouter une ligne de crédit « idées inspirées de Explorer's Compass Edited par MCMostWolf » dans le README.** Les idées en elles-mêmes ne sont pas protégées par le droit d'auteur ; le crédit est une courtoisie, et il devient obligatoire si du code est repris.

## Questions ouvertes

- Le jar NeoForge 1.21.1 `3.0.4` contient peut-être des comportements absents du source (bouton de nettoyage traduit, etc.) ; non vérifié, puisque le jar n'a pas été téléchargé.
- Faut-il un waypoint vers une carte tierce dès la v1 ? Cela dépend des mods de carte visés sur Fabric 26.3 (Xaero, JourneyMap) et de leurs API publiques.
