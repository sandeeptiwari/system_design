# rate_limiter

This is a standalone Java module providing a simple in-memory fixed-window rate limiter.

To build and run tests from the project root:

```bash
./gradlew :rate_limiter:test --no-daemon
```

# Moderate contention (8 threads)
./gradlew :rate_limiter:jmhRun \
-PjmhArgs="-i 5 -wi 3 -f 1 -t 8 .*HotKeyBench.*" \
-PjmhJvmArgs="-Xmx256m"

# High contention (32 threads) - really stress test it
./gradlew :rate_limiter:jmhRun \
-PjmhArgs="-i 5 -wi 3 -f 1 -t 32 .*HotKeyBench.*" \
-PjmhJvmArgs="-Xmx256m"