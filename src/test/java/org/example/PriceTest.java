package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PriceTest {

    @Test
    void shouldConvertDoubleToLongWithScale100() {
        assertEquals(10000L, Price.toLong(100.00, 100));
        assertEquals(10001L, Price.toLong(100.01, 100));
        assertEquals(9999L, Price.toLong(99.99, 100));
    }

    @Test
    void shouldConvertDoubleToLongWithScale100000() {
        assertEquals(10000050L, Price.toLong(100.00050, 100_000));
        assertEquals(12345600L, Price.toLong(123.45600, 100_000));
    }

    @Test
    void shouldConvertLongToDouble() {
        assertEquals(100.00, Price.toDouble(10000L, 100));
        assertEquals(100.01, Price.toDouble(10001L, 100));
        assertEquals(100.00050, Price.toDouble(10000050L, 100_000));
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> Price.toLong(-1.0, 100));
    }

    @Test
    void shouldRejectPriceBeyondScale() {
        assertThrows(IllegalArgumentException.class, () -> Price.toLong(100.001, 100));
    }

    @Test
    void shouldAcceptPriceAtExactScale() {
        assertDoesNotThrow(() -> Price.toLong(100.00, 100));
        assertDoesNotThrow(() -> Price.toLong(100.01, 100));
        assertDoesNotThrow(() -> Price.toLong(100.00001, 100_000));
    }
}