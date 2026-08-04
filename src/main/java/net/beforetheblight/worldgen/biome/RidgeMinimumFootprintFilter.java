package net.beforetheblight.worldgen.biome;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Removes horizontally connected Ridge biome components that are too small.
 * Coordinates are biome quart coordinates and connectivity never changes Y.
 */
public final class RidgeMinimumFootprintFilter {
	public static final int MIN_COMPONENT_QUARTS = 256;
	public static final int CACHE_CAPACITY = 32_768;

	private static final int[][] CARDINAL_NEIGHBORS = {
		{1, 0},
		{-1, 0},
		{0, 1},
		{0, -1}
	};

	private RidgeMinimumFootprintFilter() {
	}

	@FunctionalInterface
	public interface RawRidgeSampler {
		boolean isRawRidge(int quartX, int quartY, int quartZ);
	}

	public enum Decision {
		KEEP,
		REMOVE
	}

	public record QuartCoordinate(int x, int y, int z) {
	}

	/**
	 * Stateful bounded classifier. Instances must be confined to one thread and
	 * one biome-source/sampler scope.
	 */
	public static final class Classifier {
		private final int minimumComponentQuarts;
		private final Map<QuartCoordinate, Boolean> rawCache;
		private final Map<QuartCoordinate, Decision> statusCache;

		public Classifier() {
			this(MIN_COMPONENT_QUARTS, CACHE_CAPACITY);
		}

		public Classifier(int minimumComponentQuarts, int cacheCapacity) {
			if (minimumComponentQuarts < 1) {
				throw new IllegalArgumentException("minimumComponentQuarts must be positive");
			}
			if (cacheCapacity < 1) {
				throw new IllegalArgumentException("cacheCapacity must be positive");
			}
			this.minimumComponentQuarts = minimumComponentQuarts;
			this.rawCache = new BoundedLruMap<>(cacheCapacity);
			this.statusCache = new BoundedLruMap<>(cacheCapacity);
		}

		public void clear() {
			rawCache.clear();
			statusCache.clear();
		}

		public void rememberRawRidge(int quartX, int quartY, int quartZ) {
			rawCache.put(new QuartCoordinate(quartX, quartY, quartZ), true);
		}

		public boolean shouldKeep(
			int quartX,
			int quartY,
			int quartZ,
			RawRidgeSampler rawSampler
		) {
			QuartCoordinate origin = new QuartCoordinate(quartX, quartY, quartZ);
			Decision cachedOrigin = statusCache.get(origin);
			if (cachedOrigin != null) {
				return cachedOrigin == Decision.KEEP;
			}
			if (!isRawRidge(origin, rawSampler)) {
				return false;
			}

			ArrayDeque<QuartCoordinate> pending = new ArrayDeque<>();
			Set<QuartCoordinate> enqueued = new HashSet<>();
			Set<QuartCoordinate> ridgeCells = new HashSet<>();
			pending.add(origin);
			enqueued.add(origin);

			while (!pending.isEmpty()) {
				QuartCoordinate position = pending.removeFirst();
				Decision cached = statusCache.get(position);
				if (cached != null) {
					cacheDecision(ridgeCells, cached);
					return cached == Decision.KEEP;
				}
				if (!isRawRidge(position, rawSampler)) {
					continue;
				}

				ridgeCells.add(position);
				if (ridgeCells.size() >= minimumComponentQuarts) {
					cacheDecision(ridgeCells, Decision.KEEP);
					return true;
				}

				for (int[] neighbor : CARDINAL_NEIGHBORS) {
					QuartCoordinate next = new QuartCoordinate(
						position.x() + neighbor[0],
						position.y(),
						position.z() + neighbor[1]
					);
					if (enqueued.add(next)) {
						pending.addLast(next);
					}
				}
			}

			cacheDecision(ridgeCells, Decision.REMOVE);
			return false;
		}

		public int rawCacheSize() {
			return rawCache.size();
		}

		public int statusCacheSize() {
			return statusCache.size();
		}

		private boolean isRawRidge(
			QuartCoordinate position,
			RawRidgeSampler rawSampler
		) {
			Boolean cached = rawCache.get(position);
			if (cached != null) {
				return cached;
			}
			boolean rawRidge = rawSampler.isRawRidge(position.x(), position.y(), position.z());
			rawCache.put(position, rawRidge);
			return rawRidge;
		}

		private void cacheDecision(Set<QuartCoordinate> positions, Decision decision) {
			for (QuartCoordinate position : positions) {
				statusCache.put(position, decision);
			}
		}
	}

	private static final class BoundedLruMap<K, V> extends LinkedHashMap<K, V> {
		private final int capacity;

		private BoundedLruMap(int capacity) {
			super(Math.min(capacity, 1_024), 0.75F, true);
			this.capacity = capacity;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > capacity;
		}
	}
}
