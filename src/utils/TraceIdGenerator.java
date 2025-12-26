package utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance, thread-safe ID generator for simulation events.
 * Uses atomic hardware instructions instead of SecureRandom (UUID).
 */
public class TraceIdGenerator {
    
    // Start at 1 for readability
    private static final AtomicLong counter = new AtomicLong(0);

    public static long nextId() {
        return counter.incrementAndGet();
    }

    public static void reset() {
        counter.set(0);
    }
}