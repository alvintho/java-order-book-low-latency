package org.example.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IdGeneratorTest {

    @Test
    void shouldStartFromOne() {
        IdGenerator gen = new IdGenerator();
        assertEquals(1L, gen.next());
    }

    @Test
    void shouldReturnMonotonicallyIncreasingIds() {
        IdGenerator gen = new IdGenerator();
        long first  = gen.next();
        long second = gen.next();
        long third  = gen.next();

        assertEquals(1L, first);
        assertEquals(2L, second);
        assertEquals(3L, third);
    }

    @Test
    void shouldNeverReturnZero() {
        // Zero reserved as "no ID" / null sentinel
        IdGenerator gen = new IdGenerator();
        for (int i = 0; i < 1000; i++) {
            assertTrue(gen.next() > 0);
        }
    }

    @Test
    void shouldSupportIndependentGenerators() {
        // Order IDs and Trade IDs use separate generators
        IdGenerator orderIdGen = new IdGenerator();
        IdGenerator tradeIdGen = new IdGenerator();

        assertEquals(1L, orderIdGen.next());
        assertEquals(1L, tradeIdGen.next());
        assertEquals(2L, orderIdGen.next());
        assertEquals(2L, tradeIdGen.next());
    }
}