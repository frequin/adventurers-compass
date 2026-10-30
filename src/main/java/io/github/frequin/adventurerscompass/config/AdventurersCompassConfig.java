package io.github.frequin.adventurerscompass.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	
	public static boolean allowTeleport = true;
	public static boolean displayCoordinates = true;
	public static int maxRadius = 1000;
	public static int compassDurability = 0;
	public static int defaultXpLevel = 5;
	public static Map<String, Integer> perStructureXpLevels = new HashMap<String, Integer>();
	public static double defaultDimensionXpMultiplier = 2;
	public static Map<String, Double> dimensionXpMultipliers = defaultDimensionXpMultipliers();
	public static List<String> structureBlacklist = new ArrayList<String>();
	public static boolean visitedMessage = true;
	
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
				defaultDimensionXpMultiplier = data.common.defaultDimensionXpMultiplier;
				dimensionXpMultipliers = data.common.dimensionXpMultipliers;
				structureBlacklist = data.common.structureBlacklist;
				visitedMessage = data.common.visitedMessage;
				
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
			Data data = new Data(new Data.Common(allowTeleport, displayCoordinates, maxRadius, compassDurability, defaultXpLevel, perStructureXpLevels, defaultDimensionXpMultiplier, dimensionXpMultipliers, structureBlacklist, visitedMessage), new Data.Client(hudEnabled, displayWithChatOpen, translateStructureNames, overlayLineOffset, overlaySide));
			gson.toJson(data, writer);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** The default multipliers: the Overworld costs the base price, every other dimension uses defaultDimensionXpMultiplier. */
	private static Map<String, Double> defaultDimensionXpMultipliers() {
		Map<String, Double> multipliers = new LinkedHashMap<String, Double>();
		multipliers.put("minecraft:overworld", 1.0);
		return multipliers;
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
			private final String allowTeleportComment = "Allows a player to teleport to a located structure when in creative mode, opped, or in cheat mode.";
			private final boolean allowTeleport;

			private final String displayCoordinatesComment = "Shows the Target's X and Z on a second HUD line. The distance is always shown.";
			private final boolean displayCoordinates;
			
			private final String maxRadiusComment = "The maximum horizontal distance, in blocks, from where the search started to a structure the compass can find. Raising it makes searches that find nothing more expensive.";
			private final int maxRadius;
			
			private final String compassDurabilityComment = "The number of successful structure searches before the compass breaks and must be repaired. Set to 0 to disable durability.";
			private final int compassDurability;

			private final String xpCostComment = "XP cost of a search. Levels are taken only when a Target is found: a cancelled search or one that finds nothing is free. Cost paid = ceil(Structure cost x multiplier of the Current dimension), where the Structure cost is its perStructureXpLevels entry, else defaultXpLevel, and the multiplier is the dimensionXpMultipliers entry of the dimension the player searches in, else defaultDimensionXpMultiplier. Ex with the defaults: a village costs 5 levels in the Overworld (5 x 1) and a fortress 10 in the Nether (5 x 2); with a multiplier of 1.5, 5 levels become ceil(7.5) = 8. Search and Pass need as many levels as the most expensive Structure the Selection can find here; if the player spends levels during the search, only the levels left are taken. Creative players pay nothing.";

			private final String defaultXpLevelComment = "The XP levels a Structure costs when perStructureXpLevels has no entry for it, before the dimension multiplier. No upper bound; 0 makes searches free.";
			private final int defaultXpLevel;
			
			private final String perStructureXpLevelsComment = "Per-Structure XP levels that replace defaultXpLevel, before the dimension multiplier. No upper bound; 0 makes the Structure free. The first matching entry wins. The wildcard character * matches any number of characters and ? matches one character. Ex: {\"minecraft:stronghold\":15, \"minecraft:*village*\":2, \"minecraft:buried_treasure\":0}";
			private final Map<String, Integer> perStructureXpLevels;

			private final String defaultDimensionXpMultiplierComment = "Multiplies the cost of a search in a dimension that dimensionXpMultipliers does not list. Decimals are allowed; the result is rounded up.";
			private final double defaultDimensionXpMultiplier;

			private final String dimensionXpMultipliersComment = "Per-dimension multipliers of the cost, by dimension id, applied to the dimension the player searches in. Ex: {\"minecraft:overworld\":1, \"minecraft:the_nether\":2, \"minecraft:the_end\":3}";
			private final Map<String, Double> dimensionXpMultipliers;
			
			private final String structureBlacklistComment = "A list of structures that the compass will not display in the GUI and will not be able to search for. The wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex (ignore backslashes): [\"minecraft:stronghold\", \"minecraft:endcity\", \"minecraft:*village*\"]";
			private final List<String> structureBlacklist;

			private final String visitedMessageComment = "Shows a message above the hotbar the first time a player stands in a piece of a structure Instance, which then becomes Visited and is skipped by searches.";
			private final boolean visitedMessage;
			
			private Common() {
				allowTeleport = true;
				displayCoordinates = true;
				maxRadius = 1000;
				compassDurability = 0;
				defaultXpLevel = 5;
				perStructureXpLevels = new HashMap<String, Integer>();
				defaultDimensionXpMultiplier = 2;
				dimensionXpMultipliers = defaultDimensionXpMultipliers();
				structureBlacklist = new ArrayList<String>();
				visitedMessage = true;
			}

			private Common(boolean allowTeleport, boolean displayCoordinates, int maxRadius, int compassDurability, int defaultXpLevel, Map<String, Integer> perStructureXpLevels, double defaultDimensionXpMultiplier, Map<String, Double> dimensionXpMultipliers, List<String> structureBlacklist, boolean visitedMessage) {
				this.allowTeleport = allowTeleport;
				this.displayCoordinates = displayCoordinates;
				this.maxRadius = maxRadius;
				this.compassDurability = compassDurability;
				this.defaultXpLevel = defaultXpLevel;
				this.perStructureXpLevels = perStructureXpLevels;
				this.defaultDimensionXpMultiplier = defaultDimensionXpMultiplier;
				this.dimensionXpMultipliers = dimensionXpMultipliers;
				this.structureBlacklist = structureBlacklist;
				this.visitedMessage = visitedMessage;
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
