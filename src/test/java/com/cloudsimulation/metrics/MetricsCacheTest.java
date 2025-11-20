package com.cloudsimulation.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsCache.
 */
public class MetricsCacheTest {
    private MetricsCache cache;
    private MetricsSnapshot sampleSnapshot;

    @BeforeEach
    public void setUp() {
        cache = new MetricsCache();
        sampleSnapshot = new MetricsSnapshot(
            10.0, // timestamp
            0.75, // avgCpuUtilization
            0.60, // avgMemoryUtilization
            5,    // overloadedVmCount
            2,    // underloadedVmCount
            0,    // totalMigrations
            50,   // activeVmCount
            0.0   // powerConsumption
        );
    }

    @Test
    public void testCacheHitWithinWindow() {
        // Put snapshot at time 10.0
        cache.put(10.0, sampleSnapshot);

        // Test cache hit within 5-tick window (10.0 to 14.9)
        MetricsSnapshot retrieved = cache.get(10.0);
        assertNotNull(retrieved, "Cache should return snapshot at same time");
        assertEquals(sampleSnapshot, retrieved);

        retrieved = cache.get(12.0);
        assertNotNull(retrieved, "Cache should return snapshot within 2 ticks");
        assertEquals(sampleSnapshot, retrieved);

        retrieved = cache.get(14.9);
        assertNotNull(retrieved, "Cache should return snapshot at 4.9 ticks");
        assertEquals(sampleSnapshot, retrieved);
    }

    @Test
    public void testCacheMissAfterWindow() {
        // Put snapshot at time 10.0
        cache.put(10.0, sampleSnapshot);

        // Test cache miss after 5-tick window (>= 15.0)
        MetricsSnapshot retrieved = cache.get(15.0);
        assertNull(retrieved, "Cache should return null at exactly 5 ticks");

        retrieved = cache.get(20.0);
        assertNull(retrieved, "Cache should return null after 5 ticks");
    }

    @Test
    public void testCacheInvalidation() {
        // Put snapshot and verify it's cached
        cache.put(10.0, sampleSnapshot);
        assertNotNull(cache.get(10.0), "Snapshot should be cached");

        // Invalidate cache
        cache.invalidate();

        // Verify cache is empty
        assertNull(cache.get(10.0), "Cache should be empty after invalidation");
    }

    @Test
    public void testEmptyCache() {
        // Test that empty cache returns null
        MetricsSnapshot retrieved = cache.get(0.0);
        assertNull(retrieved, "Empty cache should return null");

        retrieved = cache.get(100.0);
        assertNull(retrieved, "Empty cache should return null for any time");
    }

    @Test
    public void testIsValid() {
        // Test isValid on empty cache
        assertFalse(cache.isValid(0.0), "Empty cache should be invalid");

        // Put snapshot at time 10.0
        cache.put(10.0, sampleSnapshot);

        // Test validity within window
        assertTrue(cache.isValid(10.0), "Cache should be valid at same time");
        assertTrue(cache.isValid(12.0), "Cache should be valid within 2 ticks");
        assertTrue(cache.isValid(14.9), "Cache should be valid at 4.9 ticks");

        // Test invalidity outside window
        assertFalse(cache.isValid(15.0), "Cache should be invalid at 5 ticks");
        assertFalse(cache.isValid(20.0), "Cache should be invalid after 5 ticks");
    }

    @Test
    public void testCacheOverwrite() {
        // Put first snapshot
        cache.put(10.0, sampleSnapshot);

        // Create second snapshot
        MetricsSnapshot secondSnapshot = new MetricsSnapshot(
            20.0, 0.80, 0.65, 3, 1, 0, 45, 0.0
        );

        // Put second snapshot (should overwrite)
        cache.put(20.0, secondSnapshot);

        // Verify only second snapshot is cached
        assertNull(cache.get(10.0), "First snapshot should be overwritten");
        assertNotNull(cache.get(20.0), "Second snapshot should be cached");
        assertEquals(secondSnapshot, cache.get(20.0));
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        // Put initial snapshot
        cache.put(10.0, sampleSnapshot);

        // Create threads that read from cache concurrently
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    MetricsSnapshot retrieved = cache.get(12.0);
                    if (retrieved != null && retrieved.equals(sampleSnapshot)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        doneLatch.await();

        // Verify all threads successfully read the cached value
        assertEquals(threadCount, successCount.get(),
            "All threads should successfully read cached value concurrently");
    }

    @Test
    public void testConcurrentInvalidation() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Put initial snapshot
        cache.put(10.0, sampleSnapshot);

        // Create threads that invalidate cache concurrently
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    cache.invalidate();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        doneLatch.await();

        // Verify cache is invalidated (no exceptions thrown)
        assertNull(cache.get(10.0), "Cache should be invalidated after concurrent invalidation");
    }
}
