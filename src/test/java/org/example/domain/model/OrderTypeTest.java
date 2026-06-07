package org.example.domain.model;

import org.example.domain.enums.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTypeTest {

    @Test
    void shouldHaveLimitType() {
        assertEquals("LIMIT", OrderType.LIMIT.name());
    }

    @Test
    void shouldHaveMarketType() {
        assertEquals("MARKET", OrderType.MARKET.name());
    }

    @Test
    void shouldMapFixCodes() {
        assertEquals('2', OrderType.LIMIT.getFixCode());
        assertEquals('1', OrderType.MARKET.getFixCode());
    }

    @Test
    void shouldParseFromFixCode() {
        assertEquals(OrderType.LIMIT,  OrderType.fromFixCode('2'));
        assertEquals(OrderType.MARKET, OrderType.fromFixCode('1'));
    }

    @Test
    void shouldRejectUnknownFixCode() {
        assertThrows(IllegalArgumentException.class, () -> OrderType.fromFixCode('9'));
    }
}