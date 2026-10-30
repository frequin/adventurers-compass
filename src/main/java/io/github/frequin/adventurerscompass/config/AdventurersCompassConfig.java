package io.github.frequin.adventurerscompass.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.util.OverlaySide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class AdventurersCompassConfig {

	private static Path configFilePath;
	private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	
	public static boolean allowTeleport = false;
	public static boolean displayCoordinates = true;
	public static int maxRadius = 1000;
	public static int compassDurability = 0;
	public static int defaultXpLevel = 0;
	public static Map<String, Integer> perStructureXpLevels = new HashMap<String, Integer>();
	public static List<String> hiddenStructures = new ArrayList<String>();
	public static boolean visitedMessage = true;
	public static List<String> disabledDimensions = new ArrayList<String>();
	public static boolean targetVisitedSound = true;
	
	public static boolean hudEnabled = true;
	public static boolean displayWithChatOpen = true;
	public static boolean translateStructureNames = true;
	public static int overlayLineOffset = 1;
	public static OverlaySide overlaySide = OverlaySide.LEFT;
	
	public static void load() {
		Reader reader;
		if(getFilePath().toFile().exists()) {
			try {
				reader = Files.newBufferedReader(getFilePath());
				
				Data data = gson.fromJson(reader, Data.class);
				
				allowTeleport = data.common.allowTeleport;
				displayCoordinates = data.common.displayCoordinates;
				maxRadius = data.common.maxRadius;
				compassDurability = data.common.compassDurability;
				defaultXpLevel = data.common.defaultXpLevel;
				perStructureXpLevels = data.common.perStructureXpLevels;
				hiddenStructures = data.common.hiddenStructures;
				visitedMessage = data.common.visitedMessage;
				disabledDimensions = data.common.disabledDimensions == null ? new ArrayList<String>() : data.common.disabledDimensions;
				targetVisitedSound = data.common.targetVisitedSound;
				
				hudEnabled = data.client.hudEnabled;
				displayWithChatOpen = data.client.displayWithChatOpen;
				translateStructureNames = data.client.translateStructureNames;
				overlayLineOffset = data.client.overlayLineOffset;
				overlaySide = data.client.overlaySide;
				
				reader.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		save();
	}
	
	public static void save() {
		try {
			Writer writer = Files.newBufferedWriter(getFilePath());
			Data data = new Data(new Data.Common(allowTeleport, displayCoordinates, maxRadius, compassDurability, defaultXpLevel, perStructureXpLevels, hiddenStructures, visitedMessage, disabledDimensions, targetVisitedSound), new Data.Client(hudEnabled, displayWithChatOpen, translateStructureNames, overlayLineOffset, overlaySide));
			gson.toJson(data, writer);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Whether the compass refuses to search in this dimension id ({@code disabledDimensions}). */
	public static boolean isDisabledDimension(String dimension) {
		return disabledDimensions.contains(dimension);
	}

	private static Path getFilePath() {
		if(configFilePath == null) {
			configFilePath = FabricLoader.getInstance().getConfigDir().resolve(AdventurersCompass.MODID + ".json");
		}
		return configFilePath;
	}
	
	private static class Data {
		
		private Common common;
		private Client client;
		
		public Data(Common common, Client client) {
			this.common = common;
			this.client = client;
		}
		
		private static class Common {
			private final String allowTeleportComment = "Allows ops (or the single player with cheats on) and creative players to teleport to the Target. Teleporting never crosses dimensions, does not wear the compass and does not mark the Target Visited.";
			private final boolean allowTeleport;

			private final String displayCoordinatesComment = "Shows the Target's X and Z on a second HUD line. The distance is always shown.";
			private final boolean displayCoordinates;
			
			private final String maxRadiusComment = "The maximum horizontal distance, in blocks, from where the search started to a structure the compass can find. Raising it makes searches that find nothing more expensive.";
			private final int maxRadius;
			
			private final String compassDurabilityComment = "The number of successful structure searches before the compass breaks and must be repaired. Set to 0 to disable durability.";
			private final int compassDurability;

			private final String defaultXpLevelComment = "The default number of XP levels consumed when searching for a structure. Individual structures can be configured via xpLevelOverrides. Max of 3 levels.";
			private final int defaultXpLevel;
			
			private final String perStructureXpLevelsComment = "A map of per-structure XP level costs that override defaultXpLevels. Structures not listed here use defaultXpLevels. Max of 3 levels. The wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex: {\"minecraft:buried_treasure\":3, \"minecraft:end*\":2, \"minecraft:*village*\":1}";
			private final Map<String, Integer> perStructureXpLevels;
			
			private final String hiddenStructuresComment = "Structures to Hide: they are not listed in the compass screen and never searched. Each entry is a structure id, where * matches any number of characters and ? one character, or a structure tag starting with #. Structures with the tag #c:hidden_from_locator_selection are always Hidden. A Hidden structure already in a compass Selection stays there as a greyed chip, and a Target already found is kept. Ex (ignore backslashes): [\"minecraft:stronghold\", \"minecraft:*village*\", \"#minecraft:village\"]";
			private final List<String> hiddenStructures;

			private final String visitedMessageComment = "Shows a message above the hotbar the first time a player stands in a piece of a structure Instance, which then becomes Visited and is skipped by searches.";
			private final boolean visitedMessage;

			private final String disabledDimensionsComment = "Dimension ids where the compass does not work: the screen lists nothing and Search and Pass are refused, with no XP taken. A Target found in another dimension is kept. Ex: [\"minecraft:the_end\"]";
			private final List<String> disabledDimensions;

			private final String targetVisitedSoundComment = "Plays a soft sound to the player when the Target of a compass they carry becomes Visited (by walking into it or with \"Mark Visited\"). Other Structures stay silent.";
			private final boolean targetVisitedSound;
			
			private Common() {
				allowTeleport = false;
				displayCoordinates = true;
				maxRadius = 1000;
				compassDurability = 0;
				defaultXpLevel = 0;
				perStructureXpLevels = new HashMap<String, Integer>();
				hiddenStructures = new ArrayList<String>();
				visitedMessage = true;
				disabledDimensions = new ArrayList<String>();
				targetVisitedSound = true;
			}

			private Common(boolean allowTeleport, boolean displayCoordinates, int maxRadius, int compassDurability, int defaultXpLevel, Map<String, Integer> perStructureXpLevels, List<String> hiddenStructures, boolean visitedMessage, List<String> disabledDimensions, boolean targetVisitedSound) {
				this.allowTeleport = allowTeleport;
				this.displayCoordinates = displayCoordinates;
				this.maxRadius = maxRadius;
				this.compassDurability = compassDurability;
				this.defaultXpLevel = defaultXpLevel;
				this.perStructureXpLevels = perStructureXpLevels;
				this.hiddenStructures = hiddenStructures;
				this.visitedMessage = visitedMessage;
				this.disabledDimensions = disabledDimensions;
				this.targetVisitedSound = targetVisitedSound;
			}
		}
		
		private static class Client {
			private final String hudEnabledComment = "Shows the Target (or the search state) on the HUD while the compass is held in either hand.";
			private final boolean hudEnabled;

			private final String displayWithChatOpenComment = "Displays compass information even while chat is open.";
			private final boolean displayWithChatOpen;
			
			private final String translateStructureNamesComment = "Attempts to translate structure names before fixing the unlocalized names. Translations may not be available for all structures.";
			private final boolean translateStructureNames;
			
			private final String overlayLineOffsetComment = "The line offset for information rendered on the HUD.";
			private final int overlayLineOffset;
			
			private final String overlaySideComment = "The side for information rendered on the HUD. Ex: LEFT, RIGHT";
			private final OverlaySide overlaySide;
			
			private Client() {
				hudEnabled = true;
				displayWithChatOpen = true;
				translateStructureNames = true;
				overlayLineOffset = 1;
				overlaySide = OverlaySide.LEFT;
			}
			
			private Client(boolean hudEnabled, boolean displayWithChatOpen, boolean translateStructureNames, int overlayLineOffset, OverlaySide overlaySide) {
				this.hudEnabled = hudEnabled;
				this.displayWithChatOpen = displayWithChatOpen;
				this.translateStructureNames = translateStructureNames;
				this.overlayLineOffset = overlayLineOffset;
				this.overlaySide = overlaySide;
			}
		}
	}

}
