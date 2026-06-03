package com.rotatingmind.springtx.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A bounded, TTL-aware cache for storing WindowEntry objects keyed by strings.
 *
 * <p>Features:
 * - TTL-based eviction: entries not accessed for {@code ttlMillis} are removed.
 * - Size-bounded: if the cache exceeds {@code maxSize}, least-recently-used entries are evicted.
 * - Background cleaner: a scheduled thread periodically scans and removes stale entries.
 * - Metrics: exposes currentSize(), evictionCount(), and lastCleanupDurationMs().
 *
 * <p>Implementation:
 * - Uses ConcurrentHashMap for fast key lookups.
 * - Uses ConcurrentLinkedDeque to track approximate access order (for LRU trimming).
 * - On each access, updates the entry's lastAccessMillis and pushes the key to the front of the deque.
 * - The background cleaner removes entries older than TTL and trims the tail if size > maxSize.
 *
 * <p>Trade-offs:
 * - Approximate LRU: under high concurrency, the deque may not be perfectly ordered, but this is
 *   acceptable for eviction heuristics.
 * - Background cleanup introduces small delays; tune {@code cleanupIntervalMillis} for your workload.
 * - Per-access deque update has low overhead (lock-free operations).
 */
public class WindowCache {

    private final ConcurrentHashMap<String, WindowEntry> map;
    private final ConcurrentLinkedDeque<String> accessOrder;
    private final AtomicInteger size;
    private final long ttlMillis;
    private final int maxSize;
    private final long cleanupIntervalMillis;

    private final AtomicLong evictionCount = new AtomicLong(0);
    private volatile long lastCleanupDurationMs = 0;

    private final ScheduledExecutorService cleanupExecutor;
    private volatile boolean shutdown = false;

    /**
     * Create a WindowCache with specified TTL and size bounds.
     *
     * @param ttlMillis time-to-live in milliseconds (expireAfterAccess)
     * @param maxSize maximum number of entries to retain
     * @param cleanupIntervalMillis interval between background cleanup runs
     */
    public WindowCache(long ttlMillis, int maxSize, long cleanupIntervalMillis) {
        if (ttlMillis <= 0) throw new IllegalArgumentException("ttlMillis must be > 0");
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        if (cleanupIntervalMillis <= 0) throw new IllegalArgumentException("cleanupIntervalMillis must be > 0");

        this.map = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentLinkedDeque<>();
        this.size = new AtomicInteger(0);
        this.ttlMillis = ttlMillis;
        this.maxSize = maxSize;
        this.cleanupIntervalMillis = cleanupIntervalMillis;

        // Start background cleanup thread
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WindowCache-Cleaner");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                cleanupIntervalMillis,
                cleanupIntervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Get or create a WindowEntry for the given key.
     * Updates lastAccessMillis and adds key to access-order deque.
     *
     * @param key the key
     * @param now current timestamp in milliseconds
     * @return the WindowEntry for this key
     */
    public WindowEntry getOrCreate(String key, long now) {
        WindowEntry entry = map.computeIfAbsent(key, k -> {
            size.incrementAndGet();
            return new WindowEntry(now, now);
        });
        entry.updateLastAccess(now);
        // Push to front of deque to mark as recently accessed (approximate LRU)
        accessOrder.offerFirst(key);
        return entry;
    }

    /**
     * Background cleanup: remove stale entries (TTL) and trim to maxSize if needed.
     */
    private void cleanup() {
        if (shutdown) return;

        long startTime = System.currentTimeMillis();
        long now = startTime;
        int removed = 0;

        // Step 1: Remove entries that exceed TTL
        for (Map.Entry<String, WindowEntry> e : map.entrySet()) {
            if (shutdown) break;
            String key = e.getKey();
            WindowEntry entry = e.getValue();
            if (now - entry.getLastAccessMillis() > ttlMillis) {
                if (map.remove(key, entry)) {
                    size.decrementAndGet();
                    removed++;
                }
            }
        }

        // Step 2: If size still exceeds maxSize, trim from tail (least recently accessed)
        while (size.get() > maxSize && !shutdown) {
            String key = accessOrder.pollLast();
            if (key == null) break;
            WindowEntry entry = map.remove(key);
            if (entry != null) {
                size.decrementAndGet();
                removed++;
            }
        }

        if (removed > 0) {
            evictionCount.addAndGet(removed);
        }

        lastCleanupDurationMs = System.currentTimeMillis() - startTime;
    }

    /**
     * Shutdown the background cleanup thread. Should be called when the cache is no longer needed.
     */
    public void shutdown() {
        shutdown = true;
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the current number of entries in the cache.
     */
    public int currentSize() {
        return size.get();
    }

    /**
     * Get the total number of evictions since cache creation.
     */
    public long evictionCount() {
        return evictionCount.get();
    }

    /**
     * Get the duration of the last cleanup run in milliseconds.
     */
    public long lastCleanupDurationMs() {
        return lastCleanupDurationMs;
    }

    /**
     * Get the configured TTL in milliseconds.
     */
    public long getTtlMillis() {
        return ttlMillis;
    }

    /**
     * Get the configured max size.
     */
    public int getMaxSize() {
        return maxSize;
    }
}

