package com.rotatingmind.design.ratelimiter.benchmark;

import com.rotatingmind.design.ratelimiter.RateLimiter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Param;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark: Mixed Zipf Distribution Workload
 *
 * Purpose: Simulates realistic traffic patterns where some keys are "hot" (frequently accessed)
 * and many keys are "cold" (rarely accessed). This follows the Zipf/Pareto distribution
 * commonly seen in real-world scenarios (80/20 rule).
 *
 * Real-World Examples:
 * - Social media: Celebrity accounts get way more traffic than regular users
 * - E-commerce: Popular products get most views, long-tail products get few
 * - APIs: Login/home endpoints hit frequently, admin endpoints rarely
 * - Content: Top 10% of articles/videos get 90% of views
 *
 * What this benchmark tests:
 * 1. Cache effectiveness: Does the bounded cache keep hot keys while evicting cold ones?
 * 2. Mixed contention: Hot keys have thread contention, cold keys don't
 * 3. Realistic performance: More accurate than pure-hot or pure-unique workloads
 * 4. Memory stability: Map size should stabilize (not grow unbounded like HighCardinality)
 *
 * Expected Results (with bounded cache):
 * - Map size: Stabilizes at ~50-70% of maxSize (hot keys stay, cold evicted)
 * - Throughput: ~900k-1.1M ops/sec (mixed contention)
 * - Latency: p50 ~400ns (cold keys), p99 ~3-5µs (hot keys with contention)
 * - Evictions: Should see continuous evictions of cold keys
 *
 * Expected Results (without bounded cache - baseline):
 * - Map size: Grows toward uniqueKeys limit (10,000)
 * - Memory: Grows with map size
 * - No evictions
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class RateLimiterMixedZipfBench {

    private RateLimiter rl;
    private Random random;

    /**
     * Total number of unique keys in the key space.
     *
     * Example: uniqueKeys=10000 means we'll generate keys from "user-0" to "user-9999"
     *
     * With Zipf distribution (skew=0.8):
     * - user-0 to user-99 (top 1%) will get ~40-50% of requests [HOT]
     * - user-100 to user-999 (next 9%) will get ~30% of requests [WARM]
     * - user-1000 to user-9999 (bottom 90%) will get ~20-30% of requests [COLD]
     */
    @Param({"10000"})
    private int uniqueKeys;

    /**
     * Skew parameter controls how "skewed" the distribution is.
     *
     * Skew values and their effects:
     * - skew = 0.0: Uniform distribution (all keys equally likely) - no hot keys
     * - skew = 0.5: Mild skew (some popular keys, relatively balanced)
     * - skew = 0.8: Realistic skew (matches typical web traffic patterns) [DEFAULT]
     * - skew = 1.0: Strong skew (classic Zipf - very concentrated on top keys)
     * - skew = 1.5: Extreme skew (almost all traffic to top 1% of keys)
     *
     * Formula explanation:
     * Lower skew (closer to 0) → More uniform, less concentration
     * Higher skew (>1.0) → More concentration on top-ranked keys
     *
     * Real-world analogy:
     * - YouTube views: skew ~1.0-1.2 (few viral videos, long tail of rarely-watched)
     * - API endpoints: skew ~0.7-0.9 (some popular endpoints, many niche ones)
     * - User activity: skew ~0.8-1.0 (power users vs casual users)
     */
    @Param({"0.8"})
    private double skew;

    /**
     * Setup the rate limiter and random number generator.
     *
     * RateLimiter config:
     * - maxRequests=10: Allow 10 requests per window
     * - window=200ms: Short window for fast benchmark iterations
     *
     * Random seed=12345: Fixed seed for reproducibility (same sequence each run)
     */
    @Setup
    public void setup() {
        rl = new RateLimiter(10, Duration.ofMillis(200));
        random = new Random(12345); // Fixed seed for reproducible results
    }

    /**
     * The actual benchmark method - called repeatedly by JMH worker threads.
     *
     * Flow:
     * 1. Generate a skewed index using Zipf distribution (0 to uniqueKeys-1)
     * 2. Create a key string like "user-42"
     * 3. Try to acquire a permit for that key
     * 4. JMH measures how many iterations/second we can do
     *
     * Example execution sequence (with skew=0.8, uniqueKeys=10000):
     *
     * Iteration 1: zipfIndex() → 3    → tryAcquire("user-3")    [likely hot key]
     * Iteration 2: zipfIndex() → 0    → tryAcquire("user-0")    [very hot key]
     * Iteration 3: zipfIndex() → 15   → tryAcquire("user-15")   [hot key]
     * Iteration 4: zipfIndex() → 2    → tryAcquire("user-2")    [very hot key]
     * Iteration 5: zipfIndex() → 127  → tryAcquire("user-127")  [warm key]
     * Iteration 6: zipfIndex() → 0    → tryAcquire("user-0")    [very hot again!]
     * Iteration 7: zipfIndex() → 8945 → tryAcquire("user-8945") [cold/rare key]
     * Iteration 8: zipfIndex() → 5    → tryAcquire("user-5")    [hot key]
     * ... and so on
     *
     * Notice: Lower-numbered keys (0, 2, 3, 5, 15) appear much more frequently
     * than high-numbered keys (8945). This is the Zipf distribution in action!
     */
    @Benchmark
    public void mixedZipf() {
        // Generate skewed index following Zipf-like distribution
        int idx = zipfIndex(uniqueKeys, skew);

        // Create key string (e.g., "user-42")
        String key = "user-" + idx;

        // Try to acquire - return value not used in throughput benchmark
        // (we just measure how many calls/second we can make)
        rl.tryAcquire(key);
    }

    /**
     * Generate a Zipf-distributed index in the range [0, n).
     *
     * Algorithm: Power-law transformation
     * This is a simplified/approximation of Zipf that's fast for benchmarking.
     *
     * Mathematical intuition:
     * - Start with uniform random r in [0, 1)
     * - Apply power transformation: r^(1/s)
     * - Lower 's' (skew) makes it more uniform
     * - Higher 's' concentrates values near 0
     * - Scale to [0, n) range
     *
     * Step-by-step example with n=10000, s=0.8:
     *
     * EXAMPLE 1 (hot key):
     * 1. random.nextDouble() → r = 0.05 (small random number)
     * 2. Math.pow(0.05, 1/0.8) → Math.pow(0.05, 1.25) → 0.0266
     * 3. 0.0266 * 10000 → 266
     * 4. (int) 266 → idx = 266
     * 5. Return "user-266" [relatively hot key]
     *
     * EXAMPLE 2 (very hot key):
     * 1. random.nextDouble() → r = 0.001 (very small)
     * 2. Math.pow(0.001, 1.25) → 0.000178
     * 3. 0.000178 * 10000 → 1.78
     * 4. (int) 1.78 → idx = 1
     * 5. Return "user-1" [VERY hot - top ranked!]
     *
     * EXAMPLE 3 (cold key):
     * 1. random.nextDouble() → r = 0.95 (large random number)
     * 2. Math.pow(0.95, 1.25) → 0.9378
     * 3. 0.9378 * 10000 → 9378
     * 4. (int) 9378 → idx = 9378
     * 5. Return "user-9378" [cold/rare key]
     *
     * EXAMPLE 4 (medium key):
     * 1. random.nextDouble() → r = 0.5 (middle value)
     * 2. Math.pow(0.5, 1.25) → 0.42
     * 3. 0.42 * 10000 → 4200
     * 4. (int) 4200 → idx = 4200
     * 5. Return "user-4200" [medium popularity]
     *
     * Key insight: Small random values (0-0.1) → small indices (0-1000) [HOT]
     *              Large random values (0.9-1.0) → large indices (9000-10000) [COLD]
     *
     * Probability distribution (with skew=0.8, n=10000):
     * - ~40% chance of idx < 1000 (top 10% of keys)
     * - ~30% chance of idx in [1000, 3000]
     * - ~20% chance of idx in [3000, 6000]
     * - ~10% chance of idx > 6000 (bottom keys)
     *
     * @param n Total number of keys (e.g., 10000)
     * @param s Skew factor (e.g., 0.8) - higher = more skewed
     * @return Index in range [0, n-1]
     */
    private int zipfIndex(int n, double s) {
        // Step 1: Get uniform random in [0, 1)
        double r = random.nextDouble();

        // Step 2: Apply power-law transformation
        // This makes small values more likely (concentrates probability at low indices)
        double transformed = Math.pow(r, 1.0 / s);

        // Step 3: Scale to [0, n) range
        int idx = (int) (transformed * n);

        // Step 4: Safety clamp (shouldn't happen but guard against floating-point edge cases)
        if (idx >= n) idx = n - 1;

        return idx;
    }
}

