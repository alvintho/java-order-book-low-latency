package org.example.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe monotonically increasing ID generator.
 *
 * IDs start at 1. Zero is reserved as "unset / null ID".
 */
public final class IdGenerator {

    private final AtomicLong counter = new AtomicLong(0L);

    /** Returns the next unique ID (starts at 1). */
    public long next() {
        return counter.incrementAndGet();
    }
}