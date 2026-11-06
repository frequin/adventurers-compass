package io.github.frequin.adventurerscompass.domain.visited;

import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * One Visited Instance.
 *
 * @param key its key
 * @param day the world day it was marked on
 */
public record VisitedInstance(InstanceKey key, long day) {
}
