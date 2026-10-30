package io.github.frequin.adventurerscompass.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.CatalogueEntry;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class SelectionCodecsTest {

	private static final Selection SELECTION = new Selection(Set.of("minecraft:pillager_outpost", "othermod:tower"), Set.of("minecraft:villages"));

	@Test
	void aSelectionSurvivesBeingSavedAndLoaded() {
		var saved = SelectionCodecs.SELECTION.encodeStart(JsonOps.INSTANCE, SELECTION).getOrThrow();
		var loaded = SelectionCodecs.SELECTION.parse(JsonOps.INSTANCE, saved).getOrThrow();

		assertEquals(SELECTION, loaded);
	}

	@Test
	void aSavedSelectionWithMissingFieldsLoadsAsEmpty() {
		var loaded = SelectionCodecs.SELECTION.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();

		assertEquals(Selection.EMPTY, loaded);
	}

	@Test
	void aSelectionCrossesTheNetwork() {
		ByteBuf buf = Unpooled.buffer();
		SelectionCodecs.SELECTION_STREAM.encode(buf, SELECTION);

		assertEquals(SELECTION, SelectionCodecs.SELECTION_STREAM.decode(buf));
	}

	@Test
	void aCatalogueCrossesTheNetwork() {
		Catalogue catalogue = new Catalogue(List.of(
				new CatalogueEntry("minecraft:village_plains", "minecraft:villages", Set.of("minecraft:overworld"), "Minecraft"),
				new CatalogueEntry("minecraft:ruined_portal", "minecraft:ruined_portals", Set.of("minecraft:overworld", "minecraft:the_nether"), "Minecraft")));
		ByteBuf buf = Unpooled.buffer();
		SelectionCodecs.CATALOGUE_STREAM.encode(buf, catalogue);

		assertEquals(catalogue.entries(), SelectionCodecs.CATALOGUE_STREAM.decode(buf).entries());
	}

}
