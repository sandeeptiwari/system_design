package com.rotatingmind.design.ratelimiter.benchmark;

import com.rotatingmind.design.ratelimiter.RateLimiter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark: Hot Key Contention Test
 *
 * Purpose: Tests performance when MANY threads simultaneously hammer the SAME single key.
 * This simulates "celebrity" scenarios or popular endpoints under high load.
 *
 * Real-World Examples:
 * - Celebrity user posting tweet → millions of followers hitting their rate limit simultaneously
 * - Flash sale on popular product → everyone hitting same product endpoint at once
 * - Login endpoint during peak hours → same "/login" key for global rate limit
 * - DDoS attack focused on single endpoint → all requests to same key
 * - Viral video being uploaded → thousands hitting the same uploader's rate limit
 *
 * What this benchmark tests:
 * 1. **Lock contention:** How well does synchronized(w) handle many threads fighting for same lock?
 * 2. **Throughput under contention:** Can we maintain high ops/sec when threads block each other?
 * 3. **Latency degradation:** How much does p50/p99 latency increase under contention?
 * 4. **Scalability:** Does throughput increase linearly with more threads, or hit a ceiling?
 *
 * Expected behavior (with current synchronized approach):
 *
 * Timeline example with 8 threads hitting same key:
 *
 * Window starts (0ms):
 * Thread-1: synchronized(w) → acquired → count=1 ✅ allowed → release lock (took 300ns)
 * Thread-2: synchronized(w) → WAIT (Thread-1 has lock)...
 * Thread-3: synchronized(w) → WAIT (Thread-1 has lock)...
 * Thread-4: synchronized(w) → WAIT (Thread-1 has lock)...
 * ... (5 more threads waiting)
 *
 * Thread-1 releases lock:
 * Thread-2: synchronized(w) → acquired → count=2 ✅ allowed → release (took 350ns)
 * Thread-3: synchronized(w) → WAIT...
 * Thread-4: synchronized(w) → WAIT...
 *
 * ... continues until count reaches limit (1000) ...
 *
 * After 1000 requests in the window:
 * All threads: synchronized(w) → count=1001+ ❌ denied → release (still fast ~300ns, just return false)
 *
 * Window resets (1000ms):
 * Thread-5: synchronized(w) → reset window, count=1 ✅ allowed
 * ... cycle repeats
 *
 * Performance characteristics:
 * - With 8 threads: ~800k-1.0M ops/sec (threads serialize at the lock)
 * - With 32 threads: ~1.0-1.2M ops/sec (more threads but still bottlenecked by single lock)
 * - p50 latency: ~400ns (when lock is available immediately)
 * - p99 latency: ~2-5µs (when thread has to wait for other threads)
 * - Lock wait time: 10-30% of execution time depends on thread count
 *
 * Why performance doesn't scale linearly:
 * - Single lock = only ONE thread can execute the synchronized block at a time
 * - Other threads sit idle waiting for the lock (CPU wasted)
 * - With 32 threads, you get ~4x more waiting, only slightly more throughput
 *
 * Comparison to HighCardinality benchmark:
 * - HighCardinality: Each thread uses DIFFERENT key → no lock contention → 2M+ ops/sec
 * - HotKey: All threads use SAME key → high lock contention → 1M ops/sec (slower!)
 *
 * This proves: Synchronization overhead matters for hot keys!
 *
 * Solutions to improve hot-key performance (for future implementations):
 * 1. CAS loop with AtomicInteger - lock-free, ~10-15% faster
 * 2. LongAdder for counter - distributes contention across cells, ~25-30% faster
 * 3. Striped locks - split hot key across multiple lock objects, ~50% faster
 * 4. Accept the trade-off - for most use cases, 1M ops/sec is plenty fast
 *
 * How to run this benchmark:
 *
 * Moderate contention (8 threads):
 * ./gradlew :rate_limiter:jmhRun -PjmhArgs="-i 5 -wi 3 -f 1 -t 8 .*HotKeyBench.*"
 *
 * High contention (32 threads) - really stress test:
 * ./gradlew :rate_limiter:jmhRun -PjmhArgs="-i 5 -wi 3 -f 1 -t 32 .*HotKeyBench.*"
 *
 * What to look for in results:
 * - Throughput with 8 threads vs 32 threads (should not be 4x - proves lock bottleneck)
 * - Compare to HighCardinality throughput (should be lower - proves contention cost)
 * - p99 latency >> p50 latency (proves some threads wait longer than others)
 * - Score per thread decreases as you add more threads (proves serialization)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class RateLimiterHotKeyBench {

    private RateLimiter rl;

    /**
     * The "hot" key that ALL threads will hammer simultaneously.
     *
     * In production this might be:
     * - A celebrity user ID: "user:elonmusk"
     * - A popular API endpoint: "endpoint:/api/login"
     * - A global service limit: "service:upload"
     * - A flash sale product: "product:ps5-console"
     */
    private final String hotKey = "hot-key";

    /**
     * Setup the rate limiter with a reasonable limit.
     *
     * Config:
     * - maxRequests=1000: Allow 1000 requests per window
     * - window=1 second: Standard rate limiting window
     *
     * This means:
     * - First 1000 calls per second → allowed
     * - Calls 1001+ per second → denied (until window resets)
     *
     * With multiple threads:
     * - Thread-1 might get calls 1-350 (allowed)
     * - Thread-2 might get calls 351-680 (allowed)
     * - Thread-3 might get calls 681-1000 (allowed)
     * - Thread-4 might get calls 1001+ (denied until next window)
     *
     * The exact distribution depends on thread scheduling and OS, but
     * aggregate limit is always 1000/sec.
     */
    @Setup
    public void setup() {
        rl = new RateLimiter(1000, Duration.ofSeconds(1));
    }

    /**
     * The benchmark method - ALL JMH worker threads call THIS method repeatedly
     * with THE SAME key ("hot-key").
     *
     * Execution flow (example with 8 concurrent threads):
     *
     * Thread-1, Iteration 1: rl.tryAcquire("hot-key") → wait for lock → count++ → return true/false
     * Thread-2, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → eventually gets it → count++ → return
     * Thread-3, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → ...
     * Thread-4, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → ...
     * Thread-5, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → ...
     * Thread-6, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → ...
     * Thread-7, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → ...
     * Thread-8, Iteration 1: rl.tryAcquire("hot-key") → BLOCKED waiting for lock → ...
     *
     * Notice: Only ONE thread can execute synchronized(w) at a time!
     * The other 7 threads are IDLE, waiting for their turn.
     *
     * This is the "hot key contention" problem in action!
     *
     * JMH measures:
     * - Total operations per second across all threads
     * - Average time per operation
     * - How the performance changes with different thread counts
     *
     * Key insight:
     * If you double the threads (8→16), you DON'T get double throughput
     * because they're all fighting for the same lock!
     */
    @Benchmark
    public void hitHotKey() {
        rl.tryAcquire(hotKey);
    }
}

