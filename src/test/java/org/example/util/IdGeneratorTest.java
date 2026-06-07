package org.example.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest {

    @Test
    void shouldStartFromOne() {
        assertEquals(1L, new IdGenerator().next());
    }

    @Test
    void shouldReturnMonotonicallyIncreasingIds() {
        IdGenerator gen = new IdGenerator();
        assertEquals(1L, gen.next());
        assertEquals(2L, gen.next());
        assertEquals(3L, gen.next());
    }

    @Test
    void shouldNeverReturnZero() {
        IdGenerator gen = new IdGenerator();
        for (int i = 0; i < 1_000; i++) {
            assertTrue(gen.next() > 0, "ID must always be positive");
        }
    }

    @Test
    void shouldSupportIndependentGenerators() {
        IdGenerator orderIdGen = new IdGenerator();
        IdGenerator tradeIdGen = new IdGenerator();

        assertEquals(1L, orderIdGen.next());
        assertEquals(1L, tradeIdGen.next());
        assertEquals(2L, orderIdGen.next());
        assertEquals(2L, tradeIdGen.next());
    }
}