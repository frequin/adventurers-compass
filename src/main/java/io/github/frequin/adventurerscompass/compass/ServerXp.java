package io.github.frequin.adventurerscompass.compass;

import java.util.HashMap;
import java.util.Map;

import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.CatalogueEntry;
import io.github.frequin.adventurerscompass.domain.xp.PlayerXp;
import io.github.frequin.adventurerscompass.domain.xp.XpCosts;

import net.minecraft.server.level.ServerPlayer;

/** Reads the XP scale from the server config and a player's XP for the domain. */
public final class ServerXp {

	private ServerXp() {
	}

	/** The XP scale of the current config. */
	public static XpCosts costs() {
		return new XpCosts(AdventurersCompassConfig.defaultXpLevel, orEmpty(AdventurersCompassConfig.perStructureXpLevels), AdventurersCompassConfig.defaultDimensionXpMultiplier, orEmpty(AdventurersCompassConfig.dimensionXpMultipliers));
	}

	/** The player's levels now; creative ({@code hasInfiniteMaterials}) pays nothing. */
	public static PlayerXp of(ServerPlayer player) {
		return new PlayerXp(player.experienceLevel, player.hasInfiniteMaterials());
	}

	/** Real cost in {@code dimension} of every Structure of the catalogue, for the screen. */
	public static Map<String, Integer> realCosts(Catalogue catalogue, String dimension) {
		XpCosts costs = costs();
		Map<String, Integer> real = new HashMap<String, Integer>();
		for (CatalogueEntry entry : catalogue.entries()) {
			real.put(entry.structureId(), costs.cost(entry.structureId(), dimension));
		}
		return real;
	}

	private static <V> Map<String, V> orEmpty(Map<String, V> map) {
		// A config file may say null for a map
		return map == null ? Map.of() : map;
	}

}
