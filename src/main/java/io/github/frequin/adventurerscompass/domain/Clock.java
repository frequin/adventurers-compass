package io.github.frequin.adventurerscompass.domain;

/** Monotonic time source, injectable so tests can drive time. */
@FunctionalInterface
public interface Clock {

	Clock SYSTEM = System::nanoTime;

	long nanoTime();

}
