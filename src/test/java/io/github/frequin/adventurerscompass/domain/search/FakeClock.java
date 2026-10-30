package io.github.frequin.adventurerscompass.domain.search;

import io.github.frequin.adventurerscompass.domain.Clock;

/** A clock that only moves when told to. */
final class FakeClock implements Clock {

	private long now;

	@Override
	public long nanoTime() {
		return now;
	}

	void advance(long nanos) {
		now += nanos;
	}

}
