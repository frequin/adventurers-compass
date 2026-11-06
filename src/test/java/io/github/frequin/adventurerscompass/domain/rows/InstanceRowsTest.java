package io.github.frequin.adventurerscompass.domain.rows;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.CatalogueEntry;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.domain.visited.VisitedInstance;
import io.github.frequin.adventurerscompass.domain.visited.VisitedSet;

/** The rows of the Visited and Skipped tabs: what each one says. */
class InstanceRowsTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String PLAINS = "minecraft:village_plains";
	private static final String FORTRESS = "minecraft:fortress";
	private static final String TOWER = "towers:mage_tower";

	private static final Catalogue CATALOGUE = new Catalogue(List.of(
			new CatalogueEntry(PLAINS, "minecraft:villages", Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(FORTRESS, "minecraft:nether_complexes", Set.of(NETHER), "Minecraft"),
			new CatalogueEntry(TOWER, "towers:towers", Set.of(OVERWORLD), "Towers Galore")));
	private static final Map<String, String> NAMES = Map.of(PLAINS, "Plains Village", FORTRESS, "Fortress", TOWER, "Mage Tower");

	private static final RowLabels LABELS = new RowLabels() {

		@Override
		public String day(long day) {
			return "Day " + day;
		}

		@Override
		public String distance(int blocks) {
			return blocks + " m";
		}

		@Override
		public String dimension(String dimension) {
			return dimension.equals(NETHER) ? "Nether" : dimension;
		}

	};

	private final InstanceRows rows = new InstanceRows(OVERWORLD, new BlockXZ(0, 0), CATALOGUE, NAMES::get);

	private static InstanceKey key(String dimension, String structure, int chunkX, int chunkZ) {
		return new InstanceKey(dimension, structure, new ChunkCoord(chunkX, chunkZ));
	}

	private static VisitedSet visited(VisitedInstance... instances) {
		return VisitedSet.of(List.of(instances));
	}

	private InstanceRow visitedRow(InstanceKey key, long day) {
		return rows.visited(visited(new VisitedInstance(key, day)), TreeFilter.NONE, InstanceSort.RECENT).getFirst();
	}

	private InstanceRow skippedRow(InstanceKey key) {
		return rows.skipped(Set.of(key), TreeFilter.NONE, InstanceSort.NEAREST).getFirst();
	}

	@Test
	void aVisitedRowShowsItsNameThenDayCoordinatesAndDistance() {
		var row = visitedRow(key(OVERWORLD, PLAINS, 1, 0), 431);

		assertEquals("Plains Village", row.name());
		assertEquals("Day 431 · 24, 8 · 25 m", row.details(LABELS));
	}

	@Test
	void coordinatesAreTheCentreOfTheStartChunkEvenBelowZero() {
		assertEquals("Day 3 · -24, -40 · 47 m", visitedRow(key(OVERWORLD, PLAINS, -2, -3), 3).details(LABELS));
	}

	@Test
	void outsideTheCurrentDimensionTheDimensionReplacesTheDistance() {
		assertEquals("Day 9 · 24, 8 · Nether", visitedRow(key(NETHER, FORTRESS, 1, 0), 9).details(LABELS));
	}

	@Test
	void aSkippedRowHasNoDay() {
		assertEquals("24, 8 · 25 m", skippedRow(key(OVERWORLD, PLAINS, 1, 0)).details(LABELS));
		assertEquals("24, 8 · Nether", skippedRow(key(NETHER, FORTRESS, 1, 0)).details(LABELS));
	}

	@Test
	void twoInstancesOfTheSameStructureReadDifferently() {
		var near = visitedRow(key(OVERWORLD, PLAINS, 1, 0), 12);
		var far = visitedRow(key(OVERWORLD, PLAINS, 40, 3), 12);

		assertEquals(near.name(), far.name());
		assertEquals("Day 12 · 24, 8 · 25 m", near.details(LABELS));
		assertEquals("Day 12 · 648, 56 · 650 m", far.details(LABELS));
	}

	@Test
	void aRowKeepsItsInstanceForTheCross() {
		var instance = key(OVERWORLD, PLAINS, 1, 0);

		assertEquals(instance, visitedRow(instance, 1).instance());
		assertEquals(instance, skippedRow(instance).instance());
	}

	private static final InstanceKey NEAR_VILLAGE = key(OVERWORLD, PLAINS, 1, 0);
	private static final InstanceKey FAR_VILLAGE = key(OVERWORLD, PLAINS, 40, 3);
	private static final InstanceKey MID_TOWER = key(OVERWORLD, TOWER, 10, 0);
	private static final InstanceKey NETHER_FORTRESS = key(NETHER, FORTRESS, 0, 0);

	private static final VisitedSet JOURNEY = visited(
			new VisitedInstance(FAR_VILLAGE, 10),
			new VisitedInstance(NETHER_FORTRESS, 20),
			new VisitedInstance(NEAR_VILLAGE, 20),
			new VisitedInstance(MID_TOWER, 30));
	private static final Set<InstanceKey> PASSED = Set.of(FAR_VILLAGE, NETHER_FORTRESS, NEAR_VILLAGE, MID_TOWER);

	private static List<InstanceKey> keys(List<InstanceRow> rows) {
		return rows.stream().map(InstanceRow::instance).toList();
	}

	@Test
	void recentPutsTheLastMarkedFirstEvenOnTheSameDay() {
		assertEquals(List.of(MID_TOWER, NEAR_VILLAGE, NETHER_FORTRESS, FAR_VILLAGE), keys(rows.visited(JOURNEY, TreeFilter.NONE, InstanceSort.RECENT)));
	}

	@Test
	void nearestPutsTheCurrentDimensionFirstByDistance() {
		var expected = List.of(NEAR_VILLAGE, MID_TOWER, FAR_VILLAGE, NETHER_FORTRESS);

		assertEquals(expected, keys(rows.visited(JOURNEY, TreeFilter.NONE, InstanceSort.NEAREST)));
		assertEquals(expected, keys(rows.skipped(PASSED, TreeFilter.NONE, InstanceSort.NEAREST)));
	}

	@Test
	void nameSortsAlphabeticallyThenByNearest() {
		var expected = List.of(NETHER_FORTRESS, MID_TOWER, NEAR_VILLAGE, FAR_VILLAGE);

		assertEquals(expected, keys(rows.visited(JOURNEY, TreeFilter.NONE, InstanceSort.NAME)));
		assertEquals(expected, keys(rows.skipped(PASSED, TreeFilter.NONE, InstanceSort.NAME)));
	}

	@Test
	void nameSortsAccentedNamesWithTheirLetter() {
		var names = Map.of(PLAINS, "Village", TOWER, "Épave", FORTRESS, "Forteresse");
		var french = new InstanceRows(OVERWORLD, new BlockXZ(0, 0), CATALOGUE, names::get);

		assertEquals(List.of(MID_TOWER, NETHER_FORTRESS, NEAR_VILLAGE), keys(french.skipped(Set.of(NEAR_VILLAGE, MID_TOWER, NETHER_FORTRESS), TreeFilter.NONE, InstanceSort.NAME)));
	}

	@Test
	void textKeepsTheRowsWhoseNameOrIdMatches() {
		assertEquals(List.of(NEAR_VILLAGE, FAR_VILLAGE), keys(rows.visited(JOURNEY, TreeFilter.of("plains"), InstanceSort.NEAREST)));
		assertEquals(List.of(MID_TOWER), keys(rows.skipped(PASSED, TreeFilter.of("mage_tower"), InstanceSort.NEAREST)));
	}

	@Test
	void atModKeepsTheRowsOfThatSource() {
		assertEquals(List.of(MID_TOWER), keys(rows.visited(JOURNEY, TreeFilter.of("@galore"), InstanceSort.RECENT)));
		assertEquals(List.of(MID_TOWER), keys(rows.skipped(PASSED, TreeFilter.of("@towers"), InstanceSort.NEAREST)));
	}

	@Test
	void aRowOfAStructureGoneFromTheCatalogueIsStillFoundByNamespace() {
		var gone = new InstanceRows(OVERWORLD, new BlockXZ(0, 0), Catalogue.EMPTY, NAMES::get);

		assertEquals(List.of(MID_TOWER), keys(gone.visited(JOURNEY, TreeFilter.of("@towers"), InstanceSort.RECENT)));
	}

	@Test
	void theSortButtonCyclesThroughTheOrdersOfEachTab() {
		assertEquals(List.of(InstanceSort.RECENT, InstanceSort.NEAREST, InstanceSort.NAME), InstanceSort.VISITED_ORDERS);
		assertEquals(List.of(InstanceSort.NEAREST, InstanceSort.NAME), InstanceSort.SKIPPED_ORDERS);

		assertEquals(InstanceSort.NEAREST, InstanceSort.RECENT.next(InstanceSort.VISITED_ORDERS));
		assertEquals(InstanceSort.RECENT, InstanceSort.NAME.next(InstanceSort.VISITED_ORDERS));
		assertEquals(InstanceSort.NEAREST, InstanceSort.NAME.next(InstanceSort.SKIPPED_ORDERS));
	}

}
