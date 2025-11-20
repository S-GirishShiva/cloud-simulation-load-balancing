package com.cloudsimulation.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches MetricsSnapshot for a configurable number of simulation ticks to avoid redundant aggregation.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class MetricsCache {
    private static final double DEFAULT_CACHE_VALIDITY_WINDOW = 5.0;

    private final Map<Double, MetricsSnapshot> cache;
    private volatile double lastCacheTime;
    private final double cacheValidityWindow;

    /**
     * Constructs a new MetricsCache with the default 5-tick validity window.
     */
    public MetricsCache() {
        this(DEFAULT_CACHE_VALIDITY_WINDOW);
    }

    /**
     * Constructs a new MetricsCache with a custom validity window.
     *
     * @param cacheValidityWindow Number of simulation ticks to cache metrics
     * @throws IllegalArgumentException if cacheValidityWindow is negative
     */
    public MetricsCache(double cacheValidityWindow) {
        if (cacheValidityWindow < 0) {
            throw new IllegalArgumentException("Cache validity window must be non-negative");
        }
        this.cache = new ConcurrentHashMap<>();
        this.lastCacheTime = -1.0;
        this.cacheValidityWindow = cacheValidityWindow;
    }

    /**
     * Stores a metrics snapshot with the given timestamp.
     *
     * @param timestamp Simulation time when snapshot was taken
     * @param snapshot Metrics snapshot to cache
     */
    public void put(double timestamp, MetricsSnapshot snapshot) {
        cache.clear(); // Only keep most recent snapshot
        cache.put(timestamp, snapshot);
        lastCacheTime = timestamp;
    }

    /**
     * Retrieves cached snapshot if within the 5-tick validity window.
     *
     * @param currentTime Current simulation time
     * @return Cached snapshot if valid, null otherwise
     */
    public MetricsSnapshot get(double currentTime) {
        if (!isValid(currentTime)) {
            return null;
        }
        return cache.get(lastCacheTime);
    }

    /**
     * Checks if the cached snapshot is still valid.
     *
     * @param currentTime Current simulation time
     * @return true if cache is valid (within configured tick window), false otherwise
     */
    public boolean isValid(double currentTime) {
        if (lastCacheTime < 0) {
            return false;
        }
        // Cache is valid only if we're moving forward in time and within configured tick window
        double timeDiff = currentTime - lastCacheTime;
        return timeDiff >= 0 && timeDiff < cacheValidityWindow;
    }

    /**
     * Forces cache refresh by clearing all cached data.
     */
    public void invalidate() {
        cache.clear();
        lastCacheTime = -1.0;
    }

    /**
     * Gets the configured cache validity window.
     *
     * @return The number of simulation ticks for which cache is valid
     */
    public double getCacheValidityWindow() {
        return cacheValidityWindow;
    }
}
