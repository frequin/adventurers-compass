package io.github.frequin.adventurerscompass.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ReadableName;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.compass.Status;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.domain.visited.VisitedSet;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.util.LevelUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** The words the HUD and the screen use for the compass state. */
@Environment(EnvType.CLIENT)
public final class CompassTexts {

	private CompassTexts() {
	}

	/**
	 * The HUD: line 1 "Structure · distance" or the state in its place, line 2
	 * "X …, Z …" when {@code displayCoordinates}. Empty when there is nothing to say.
	 */
	public static List<Component> hudLines(CompassState state, Player player, boolean displayCoordinates) {
		List<Component> lines = new ArrayList<>();
		Status status = state.status(currentDimension(player), visited(player).asSet());
		switch (status) {
			case SEARCHING -> lines.add(Component.translatable("string.adventurerscompass.hud.searching"));
			case TARGET -> lines.add(Component.translatable("string.adventurerscompass.hud.target", structureName(state.target()), distance(state.target(), player)));
			case TARGET_VISITED -> lines.add(Component.translatable("string.adventurerscompass.hud.targetVisited", structureName(state.target())));
			case OTHER_DIMENSION -> lines.add(Component.translatable("string.adventurerscompass.hud.otherDimension", structureName(state.target()), dimensionName(state.target().dimension())));
			case NOTHING_FOUND -> lines.add(nothingFound(state.nothingFound().result()));
			case IDLE -> {
			}
		}
		// Line 2 belongs to the Target named on line 1, not to a "nothing found" shown over it
		if (displayCoordinates && state.target() != null && status != Status.SEARCHING && status != Status.NOTHING_FOUND) {
			lines.add(Component.translatable("string.adventurerscompass.hud.coordinates", state.target().pos().x(), state.target().pos().z()));
		}
		return lines;
	}

	public static Component nothingFound(SearchResult.NothingFound nothing) {
		return Component.translatable("string.adventurerscompass.hud.nothingFound", dimensionName(nothing.dimension()), nothing.radius(), nothing.visitedIgnored(), nothing.skippedIgnored());
	}

	public static Component structureName(Target target) {
		return Component.literal(StructureUtils.getStructureName(Identifier.parse(target.structureId())));
	}

	/** {@code dimension.<namespace>.<path>}, else the path in title case ("the_nether" gives "The Nether"). */
	public static Component dimensionName(String dimension) {
		Identifier id = Identifier.parse(dimension);
		return Component.translatableWithFallback("dimension." + id.getNamespace() + "." + id.getPath(), ReadableName.of(dimension));
	}

	/** Horizontal distance in whole blocks. */
	public static int distance(Target target, Player player) {
		BlockXZ here = new BlockXZ(player.blockPosition().getX(), player.blockPosition().getZ());
		return (int) Math.round(Math.sqrt(here.distanceSq(target.pos())));
	}

	/** The player's Visited Instances, as synced by the server to this player only. */
	public static VisitedSet visited(Player player) {
		return player.getAttachedOrElse(PlayerVisited.VISITED, VisitedSet.empty());
	}

	public static String currentDimension(Player player) {
		return LevelUtils.dimensionId(player.level());
	}

}
