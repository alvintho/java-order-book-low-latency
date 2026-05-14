package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTypeTest {

    @Test
    void shouldHaveLimitType() {
        assertEquals("LIMIT", OrderType.LIMIT.name());
    }

    @Test
    void shouldHaveMarketType() {
        assertEquals("MARKET", OrderType.MARKET.name());
    }
}