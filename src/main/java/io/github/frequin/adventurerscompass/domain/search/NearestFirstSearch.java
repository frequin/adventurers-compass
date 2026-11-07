package io.github.frequin.adventurerscompass.domain.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

import io.github.frequin.adventurerscompass.domain.Clock;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * Finds the nearest Instance of any searchable Structure, across all their
 * placements, by exact horizontal distance from the origin.
 *
 * <p>Candidates of every placement are merged in one min-heap. The first
 * candidate popped that holds an Instance is the nearest one. Visited and
 * Skipped Instances are set aside by key before any presence test. The search
 * runs in steps under a time budget given by the caller.
 */
public final class NearestFirstSearch<P> {

	private record Source<P>(P placement, List<String> structureIds, CandidateSource candidates) {
	}

	private record Entry<P>(long distanceSq, Candidate candidate, String structureId, P placement) {
	}

	private final WorldPort<P> world;
	private final SearchRequest request;
	private final Clock clock;
	private final long maxRadiusSq;
	private final List<Source<P>> sources = new ArrayList<>();
	private final PriorityQueue<Entry<P>> heap = new PriorityQueue<>(Comparator.comparingLong(Entry::distanceSq));
	private final Set<InstanceKey> seen = new HashSet<>();
	private int visitedIgnored;
	private int skippedIgnored;
	private SearchResult result;

	public NearestFirstSearch(WorldPort<P> world, SearchRequest request, Clock clock) {
		this.world = world;
		this.request = request;
		this.clock = clock;
		this.maxRadiusSq = (long) request.maxRadius() * request.maxRadius();

		Map<P, List<String>> structuresByPlacement = new LinkedHashMap<>();
		for (String structureId : request.structureIds().stream().sorted().toList()) {
			for (P placement : world.placements(request.dimension(), structureId)) {
				structuresByPlacement.computeIfAbsent(placement, p -> new ArrayList<>()).add(structureId);
			}
		}
		structuresByPlacement.forEach((placement, structureIds) -> sources.add(new Source<>(placement, structureIds, world.candidates(request.dimension(), placement, request.origin()))));
	}

	public SearchRequest request() {
		return request;
	}

	/**
	 * Advances the search. Always does at least one unit of work (one candidate),
	 * then stops before the next one once {@code budgetNanos} has elapsed.
	 *
	 * @return the result once the search has ended, empty while it goes on
	 */
	public Optional<SearchResult> step(long budgetNanos) {
		if (result != null) {
			return Optional.of(result);
		}
		long start = clock.nanoTime();
		do {
			result = examineNextCandidate();
		} while (result == null && clock.nanoTime() - start < budgetNanos);
		return Optional.ofNullable(result);
	}

	/** Stops the search; it ends as {@link SearchResult.Cancelled} unless it had already ended. */
	public void cancel() {
		if (result == null) {
			result = new SearchResult.Cancelled();
		}
		heap.clear();
	}

	public boolean isDone() {
		return result != null;
	}

	private SearchResult examineNextCandidate() {
		refill();
		Entry<P> entry = heap.poll();
		if (entry == null) {
			return new SearchResult.NothingFound(request.dimension(), request.maxRadius(), visitedIgnored, skippedIgnored);
		}
		InstanceKey key = new InstanceKey(request.dimension(), entry.structureId(), entry.candidate().chunk());
		if (!seen.add(key)) {
			return null;
		}
		if (request.visited().contains(key)) {
			visitedIgnored++;
			return null;
		}
		if (request.skipped().contains(key)) {
			skippedIgnored++;
			return null;
		}
		if (world.isPresent(request.dimension(), entry.structureId(), entry.placement(), entry.candidate().chunk())) {
			return new SearchResult.Found(key, entry.candidate().locatePos());
		}
		return null;
	}

	/** Pulls batches until no source can still produce a candidate nearer than the heap's top. */
	private void refill() {
		boolean pulled;
		do {
			pulled = false;
			double nearestQueued = heap.isEmpty() ? Double.POSITIVE_INFINITY : heap.peek().distanceSq();
			for (Source<P> source : sources) {
				double bound = source.candidates().nextLowerBound();
				if (bound <= request.maxRadius() && bound * bound <= nearestQueued) {
					for (Candidate candidate : source.candidates().nextBatch()) {
						long distanceSq = candidate.locatePos().distanceSq(request.origin());
						if (distanceSq <= maxRadiusSq) {
							for (String structureId : source.structureIds()) {
								heap.add(new Entry<>(distanceSq, candidate, structureId, source.placement()));
							}
						}
					}
					pulled = true;
				}
			}
		} while (pulled);
	}

}
