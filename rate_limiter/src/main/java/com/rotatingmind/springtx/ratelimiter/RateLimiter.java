package com.rotatingmind.springtx.ratelimiter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple in-memory fixed-window rate limiter keyed by an arbitrary string.
 *
 * <p>Behavior summary:
 * - Maintains a per-key fixed window (start timestamp + count).
 * - The first request for a key creates the window starting at the current time.
 * - Within the window period (windowMillis), up to {@code maxRequests} are permitted.
 * - When the current time moves past the window, the counter is reset and a new window starts.
 *
 * <p>Trade-offs and limitations (why this is simple but not always suitable):
 *
 * 1) Accuracy / boundary artifacts (fixed-window problem):
 *    - Fixed-window stores counts per aligned window interval. Two bursts that occur near the
 *      boundary of adjacent windows can each be allowed up to the limit, producing an effective
 *      burst of up to 2 * limit in a short time (one right before the boundary and one right after).
 *    - This is expected for fixed-window algorithms. Alternatives (sliding-window log,
 *      sliding-window counter with sub-windows, token bucket, or leaky bucket) reduce this
 *      burstiness at the cost of higher memory, CPU, or implementation complexity.
 *
 * 2) Memory growth and cleanup:
 *    - This implementation keeps an entry in a ConcurrentHashMap for every key ever used. In a
 *      high-cardinality workload (lots of unique keys), memory will grow indefinitely.
 *    - Production systems should add eviction (e.g., TTL-based removal, periodic cleanup of
 *      stale entries, or use a bounded cache like Caffeine/Guava) to prevent OOM.
 *
 * 3) Concurrency and contention:
 *    - Per-key synchronization is used (synchronized on the Window instance) so only threads
 *      contending for the same key block each other. This reduces global contention but can still
 *      cause contention for very-hot keys.
 *    - Alternatives include using atomic window counters with CAS or long accumulation buckets
 *      to reduce blocking, at the cost of more complex code.
 *
 * 4) Time source and clock skew:
 *    - Uses System.currentTimeMillis(). On systems with non-monotonic clocks or when running
 *      distributed across machines, clock skew can change behavior. Using System.nanoTime() for
 *      relative elapsed time or synchronizing clocks reduces this risk for local JVMs.
 *
 * 5) Single-process only:
 *    - This is an in-memory limiter and does not coordinate across processes or nodes. For
 *      distributed rate-limiting, use a shared store (Redis, Memcached) or a distributed token
 *      bucket implementation. Note that moving to Redis introduces network latency and the need
 *      for atomic operations (Lua scripts or INCR with expirations) to ensure correctness.
 *
 * 6) Precision vs performance:
 *    - More precise algorithms (sliding-window log with per-request timestamps) give exact
 *      limiting but require storing per-request timestamps and more RAM/CPU. Token bucket gives
 *      smooth rate-limiting and allows controlled bursting, often being a good compromise.
 *
 * 7) Fairness and ordering:
 *    - This limiter is best-effort and does not implement queuing or fairness across callers; it
 *      only responds allow/deny. Callers should decide how to retry or back off.
 *
 * Implementation notes:
 * - The class is intentionally small for demonstration and unit testing. It uses a
 *   ConcurrentHashMap keyed by a String, each value is a Window object that holds the start
 *   timestamp and an AtomicInteger count. We synchronize on the Window instance to update the
 *   window and counter atomically.
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    // Map storing per-key windows. Consider replacing with a bounded cache with TTL in prod.
    private final ConcurrentHashMap<String, Window> map = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, Duration window) {
        if (maxRequests <= 0) throw new IllegalArgumentException("maxRequests must be > 0");
        Objects.requireNonNull(window, "window");
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
    }

    /**
     * Try to acquire a permit for the given key. Returns true if allowed, false otherwise.
     *
     * Important implementation details and trade-offs:
     * - The method obtains the current time once at the start. This time is used for window
     *   expiry checks and counts. Using a single timestamp per call avoids repeated clock calls
     *   and keeps the window semantics consistent inside the method.
     * - We use computeIfAbsent to avoid races when creating the Window object. After getting the
     *   Window reference we synchronize on it to mutate its fields. This keeps synchronization
     *   local to the key while ensuring correct resets when the window expires.
     * - Increment is done via AtomicInteger.incrementAndGet() inside the synchronized block. The
     *   AtomicInteger is used because it provides clear semantics and can be used outside of
     *   synchronization if refactoring later. The synchronized block ensures the window reset and
     *   count increment happen atomically relative to other threads for the same key.
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Window w = map.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.windowStart >= windowMillis) {
                w.windowStart = now;
                w.count.set(0);
            }
            if (w.count.incrementAndGet() <= maxRequests) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static class Window {
        volatile long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.windowStart = start;
        }
    }
}

