package io.github.frequin.adventurerscompass.domain.search;

import io.github.frequin.adventurerscompass.domain.Clock;

/** A clock that only moves when told to. */
public final class FakeClock implements Clock {

	private long now;

	@Override
	public long nanoTime() {
		return now;
	}

	public void advance(long nanos) {
		now += nanos;
	}

}
