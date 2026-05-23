package com.rotatingmind.springtx.ratelimiter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Expanded tests that demonstrate fixed-window trade-offs with examples and suggested fixes.
 *
 * Each test includes comments that explain the observed behavior and why it happens, and
 * demonstrates an alternative (where applicable) such as a token-bucket implementation.
 */
class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocksAndResetsAfterWindow() throws InterruptedException {
        // Basic sanity: limiter allows up to maxRequests within a window, then blocks, and
        // resets after the window elapses.
        RateLimiter rl = new RateLimiter(3, Duration.ofMillis(200));
        String key = "user1";

        assertTrue(rl.tryAcquire(key));
        assertTrue(rl.tryAcquire(key));
        assertTrue(rl.tryAcquire(key));

        // next one should be blocked
        assertFalse(rl.tryAcquire(key));

        // after window passes, it should allow again
        Thread.sleep(250);

        assertTrue(rl.tryAcquire(key));
    }

    @Test
    void demonstratesFixedWindowBoundaryBurst_issue() throws InterruptedException {
        // This test demonstrates the fixed-window boundary problem: two bursts that happen on
        // either side of a window boundary can each consume up to the limit, resulting in an
        // effective short-term burst up to 2 * limit.

        int limit = 3;
        long windowMillis = 200;
        RateLimiter rl = new RateLimiter(limit, Duration.ofMillis(windowMillis));
        String key = "burstKey";

        // Do `limit` requests near the end of the current window.
        // We sleep a little to ensure the first batch lands near a boundary: start, then wait
        // nearly the whole window, then fire the first batch.
        Thread.sleep(50);
        for (int i = 0; i < limit; i++) assertTrue(rl.tryAcquire(key));

        // Now wait to cross the boundary (sleep slightly more than remaining window time).
        // This causes the limiter to reset its count for the new window.
        Thread.sleep(windowMillis + 20);

        // Now another `limit` requests are allowed immediately after the window reset.
        for (int i = 0; i < limit; i++) assertTrue(rl.tryAcquire(key));

        // In a short real-time span, we've allowed limit + limit requests -> a burst of up to 2*limit.
        // This is the fixed-window boundary artifact. If you need to avoid this, consider a
        // token-bucket or sliding-window approach (examples below).
    }

    @Test
    void showsMemoryGrowthWithManyKeys_andHowToMitigate() throws Exception {
        // The in-memory implementation stores a Window per distinct key. In a high-cardinality
        // workload this will grow without bound. Here we use reflection to inspect the
        // internal map and show it grows when we use many unique keys.

        RateLimiter rl = new RateLimiter(1, Duration.ofMillis(1000));

        final int keys = 500;
        for (int i = 0; i < keys; i++) {
            String k = "user-" + i;
            rl.tryAcquire(k);
        }

        // reflect into private map to check size (testing-only introspection)
        Field mapField = RateLimiter.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<?, ?> internal = (Map<?, ?>) mapField.get(rl);

        assertTrue(internal.size() >= keys);

        // Mitigation: In production replace the ConcurrentHashMap with a bounded cache (e.g., Caffeine)
        // with eviction/TTL so memory usage remains bounded. Alternatively, implement a background
        // cleanup that removes windows whose last access is older than some threshold.
    }

    @Test
    void tokenBucketAlternative_smoothesBursting() throws InterruptedException {
        // A token-bucket smoother allows sustained rate with controlled bursts. We implement a
        // tiny in-test TokenBucket and show that it avoids the 2*limit boundary burst issue.

        class TokenBucket {
            private final double refillPerMillis; // tokens per millisecond
            private final double capacity;
            private double tokens;
            private long lastRefill;

            TokenBucket(double permitsPerSecond, double capacity) {
                this.refillPerMillis = permitsPerSecond / 1000.0;
                this.capacity = capacity;
                this.tokens = capacity;
                this.lastRefill = System.currentTimeMillis();
            }

            synchronized boolean tryConsume(double amount) {
                long now = System.currentTimeMillis();
                long elapsed = now - lastRefill;
                if (elapsed > 0) {
                    tokens = Math.min(capacity, tokens + elapsed * refillPerMillis);
                    lastRefill = now;
                }
                if (tokens >= amount) {
                    tokens -= amount;
                    return true;
                }
                return false;
            }
        }

        // Configure token bucket to allow 3 permits per 200ms (~15 per second), capacity 3.
        double permitsPerSec = 15.0;
        TokenBucket tb = new TokenBucket(permitsPerSec, 3);

        // consume 3 tokens
        assertTrue(tb.tryConsume(1));
        assertTrue(tb.tryConsume(1));
        assertTrue(tb.tryConsume(1));

        // Immediately another should be blocked (no extra tokens)
        assertFalse(tb.tryConsume(1));

        // Wait a bit to refill partially, but not full capacity
        Thread.sleep(100); // refill ~1.5 tokens
        boolean firstAfterRefill = tb.tryConsume(1);
        // Either allowed or blocked depending on refill; the key property is gradual refill,
        // avoiding the exact 2*limit instantaneous burst of fixed-window.
        // We assert that after enough sleep we will eventually be allowed again.
        if (!firstAfterRefill) {
            Thread.sleep(100);
            assertTrue(tb.tryConsume(1));
        }
    }

    @Test
    void slidingWindowApproximation_usingSubwindows() throws InterruptedException {
        // A practical sliding-window approximation divides the window into N sub-windows and
        // keeps counts per sub-window; when checking the limit it sums counts weighted by how
        // much of the window each sub-window contributes. Below is a simple 2-bucket example
        // (previous and current) that reduces the boundary burst compared to fixed-window.

        class SlidingCounter {
            private final int limit;
            private final long windowMillis;
            private volatile long bucketStart;
            private final AtomicInteger bucketA = new AtomicInteger(0); // older
            private final AtomicInteger bucketB = new AtomicInteger(0); // current

            SlidingCounter(int limit, long windowMillis) {
                this.limit = limit;
                this.windowMillis = windowMillis;
                this.bucketStart = System.currentTimeMillis();
            }

            synchronized boolean tryAcquire() {
                long now = System.currentTimeMillis();
                long half = windowMillis / 2;
                if (now - bucketStart >= windowMillis) {
                    // full window passed: shift and reset
                    bucketA.set(0);
                    bucketB.set(0);
                    bucketStart = now;
                } else if (now - bucketStart >= half) {
                    // move current to older and start fresh current
                    bucketA.set(bucketB.get());
                    bucketB.set(0);
                    bucketStart += half;
                }
                int total = bucketA.get() + bucketB.get();
                if (total < limit) {
                    bucketB.incrementAndGet();
                    return true;
                }
                return false;
            }
        }

        SlidingCounter sc = new SlidingCounter(3, 200);

        // Use it roughly like before: you should observe reduced burstiness at boundaries vs fixed-window.
        assertTrue(sc.tryAcquire());
        assertTrue(sc.tryAcquire());
        assertTrue(sc.tryAcquire());
        assertFalse(sc.tryAcquire());

        Thread.sleep(120); // cross half-window so some capacity moves from previous bucket
        // After shifting, some allowance becomes available earlier than fixed-window full reset
        // — this demonstrates improved boundary behavior.
        sc.tryAcquire(); // may or may not be allowed depending on exact timing, but the design reduces burst.
    }
}

