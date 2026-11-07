package io.github.frequin.adventurerscompass.domain.xp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.github.frequin.adventurerscompass.domain.Glob;

/**
 * The XP scale: what a found Target costs. A Structure's cost is its
 * {@code perStructure} override (the first glob that matches its id) or
 * {@code defaultLevel}; the real cost is that times the Current dimension's
 * multiplier ({@code dimensionMultipliers}, else {@code defaultMultiplier}),
 * rounded up.
 *
 * @param defaultLevel         levels of a Structure without override
 * @param perStructure         glob ({@code *}, {@code ?}) to levels, checked in order; 0 makes it free
 * @param defaultMultiplier    multiplier of a dimension without its own
 * @param dimensionMultipliers dimension id to multiplier
 */
public record XpCosts(int defaultLevel, Map<String, Integer> perStructure, double defaultMultiplier, Map<String, Double> dimensionMultipliers) {

	/** Every Target is free. */
	public static final XpCosts FREE = new XpCosts(0, Map.of(), 1, Map.of());

	public XpCosts {
		// Keep the order of the overrides: the first matching glob wins
		perStructure = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(perStructure));
		dimensionMultipliers = Map.copyOf(dimensionMultipliers);
	}

	/** Levels a Target of {@code structureId} found in {@code dimension} costs. */
	public int cost(String structureId, String dimension) {
		double multiplier = dimensionMultipliers.getOrDefault(dimension, defaultMultiplier);
		// Decimal arithmetic, so that 10 x 0.7 rounds up to 7 and not 8
		return Math.max(0, BigDecimal.valueOf(structureLevel(structureId)).multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.CEILING).intValueExact());
	}

	/** The levels needed to launch a search of {@code searchable} in {@code dimension}. */
	public XpThreshold threshold(Set<String> searchable, String dimension) {
		return XpThreshold.of(searchable.stream().map(id -> cost(id, dimension)).toList());
	}

	private int structureLevel(String structureId) {
		for (Map.Entry<String, Integer> override : perStructure.entrySet()) {
			if (Glob.of(override.getKey()).matches(structureId)) {
				return override.getValue();
			}
		}
		return defaultLevel;
	}

}
