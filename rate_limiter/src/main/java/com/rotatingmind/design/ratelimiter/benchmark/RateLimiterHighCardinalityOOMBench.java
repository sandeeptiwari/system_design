package com.rotatingmind.design.ratelimiter.benchmark;

import com.rotatingmind.design.ratelimiter.RateLimiter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class RateLimiterHighCardinalityOOMBench {

    private RateLimiter rl;
    private AtomicLong counter;

    @Setup(Level.Trial)
    public void setup() {
        // keep window large enough so most keys won't be reused in the window
        rl = new RateLimiter(1, Duration.ofSeconds(60));
        counter = new AtomicLong(0);
    }

    @Benchmark
    public void createUniqueKeys() {
        long id = counter.getAndIncrement();
        String key = "user-" + id + "-bench";
        rl.tryAcquire(key);

        // occasional lightweight logging to observe growth when running interactively
        if ((id & 0x3FFF) == 0) { // every 16384 ops
            int size = getMapSize(rl);
            long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.out.println("[HighCardinality] mapSize=" + size + " usedBytes=" + used);
        }
    }

    @SuppressWarnings("unchecked")
    private int getMapSize(RateLimiter rl) {
        try {
            Field f = RateLimiter.class.getDeclaredField("map");
            f.setAccessible(true);
            Map<String, ?> m = (Map<String, ?>) f.get(rl);
            return m.size();
        } catch (Exception e) {
            return -1;
        }
    }
}

