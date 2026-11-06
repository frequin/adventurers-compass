package io.github.frequin.adventurerscompass.domain.rows;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * One row of the Visited or Skipped tab.
 *
 * @param instance the Instance, which the row's ✕ acts on
 * @param name     the Structure's name
 * @param day      the world day it was marked, empty for a Skipped Instance
 * @param center   the centre of the start chunk, the only position the Instance key holds
 * @param distance horizontal distance from the player in blocks, empty outside the Current dimension
 */
public record InstanceRow(InstanceKey instance, String name, OptionalLong day, BlockXZ center, OptionalInt distance) {

	/** What follows the name: {@code Day N · X, Z · distance}, the dimension replacing the distance when elsewhere. */
	public String details(RowLabels labels) {
		List<String> parts = new ArrayList<>();
		day.ifPresent(value -> parts.add(labels.day(value)));
		parts.add(center.x() + ", " + center.z());
		parts.add(distance.isPresent() ? labels.distance(distance.getAsInt()) : labels.dimension(instance.dimension()));
		return String.join(" · ", parts);
	}

}
