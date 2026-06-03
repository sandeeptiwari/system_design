package com.rotatingmind.design.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A cache entry holding rate-limiting window state for a single key.
 *
 * <p>Fields:
 * - windowStart: the starting timestamp (in milliseconds) of the current fixed window.
 * - count: atomic counter for the number of requests in the current window.
 * - lastAccessMillis: the last time this entry was accessed, used for TTL-based eviction.
 *
 * <p>This class is designed to be used with WindowCache and is internally synchronized
 * for window resets while using atomic operations for counter increments.
 */
public class WindowEntry {
    volatile long windowStart;
    final AtomicInteger count = new AtomicInteger(0);
    volatile long lastAccessMillis;

    public WindowEntry(long windowStart, long lastAccessMillis) {
        this.windowStart = windowStart;
        this.lastAccessMillis = lastAccessMillis;
    }

    /**
     * Update the last access timestamp. Called on every access to support TTL eviction.
     */
    public void updateLastAccess(long now) {
        this.lastAccessMillis = now;
    }

    /**
     * Get the current window start time.
     */
    public long getWindowStart() {
        return windowStart;
    }

    /**
     * Get the current count.
     */
    public int getCount() {
        return count.get();
    }

    /**
     * Get the last access timestamp.
     */
    public long getLastAccessMillis() {
        return lastAccessMillis;
    }
}

