package org.example.model;

import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {
    private IdGenerator idGen;
    private static final int SCALE = 100;

    @BeforeEach
    void setUp() {
        idGen = new IdGenerator();
    }

    @Test
    void shouldReturnOrderWithId() {
        Order order = Order.limitBuy(idGen.next(), 20.0, 1, SCALE);

        assertEquals(1L, order.getOrderId());
    }

    @Test
    void shouldReturnSequentialIds() {
        Order order1 = Order.limitBuy(idGen.next(), 100.0, 2, SCALE);
        Order order2 = Order.limitSell(idGen.next(), 100.0, 3, SCALE);

        assertEquals(1L, order1.getOrderId());
        assertEquals(2L, order2.getOrderId());
    }

    @Test
    void shouldReturnLimitOrderAttributes() {
        Order order = Order.limitBuy(idGen.next(), 100.0, 2, SCALE);

        assertEquals(10000L, order.getPrice());
        assertEquals(2, order.getQuantity());
        assertEquals(Side.BUY, order.getSide());
        assertEquals(OrderType.LIMIT, order.getOrderType());
        assertTrue(order.isBid());
        assertFalse(order.isMarket());
    }

    @Test
    void shouldCreateValidTimestamps() {
        long before = System.nanoTime();

        Order order1 = Order.limitBuy(idGen.next(), 100.0, 2, SCALE);
        Order order2 = Order.limitSell(idGen.next(), 100.0, 2, SCALE);

        assertTrue(order1.getTimestamp() > 0);
        assertTrue(order1.getTimestamp() >= before);
        assertTrue(order2.getTimestamp() >= order1.getTimestamp());
    }

    @Test
    void shouldRejectInvalidOrderId() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(0, 100.0, 1, SCALE));
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(-1, 100.0, 1, SCALE));
    }

    @Test
    void shouldThrowExceptionForNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(idGen.next(), 100.0, -1, SCALE));
    }

    @Test
    void shouldThrowExceptionForNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(idGen.next(), -100.0, 1, SCALE));
    }

    @Test
    void shouldRejectPriceBeyondScale() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(idGen.next(), 100.001, 1, SCALE));
    }

    // ── Market order tests ──────────────────────────────────

    @Test
    void shouldCreateMarketBuyOrder() {
        Order order = Order.marketBuy(idGen.next(), 10.0);

        assertEquals(1L, order.getOrderId());
        assertEquals(0L, order.getPrice());
        assertEquals(10.0, order.getQuantity());
        assertEquals(Side.BUY, order.getSide());
        assertEquals(OrderType.MARKET, order.getOrderType());
        assertTrue(order.isMarket());
        assertTrue(order.isBid());
    }

    @Test
    void shouldCreateMarketSellOrder() {
        Order order = Order.marketSell(idGen.next(), 10.0);

        assertEquals(1L, order.getOrderId());
        assertEquals(0L, order.getPrice());
        assertEquals(10.0, order.getQuantity());
        assertEquals(Side.SELL, order.getSide());
        assertEquals(OrderType.MARKET, order.getOrderType());
        assertTrue(order.isMarket());
        assertFalse(order.isBid());
    }

    @Test
    void shouldRejectMarketOrderWithNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.marketBuy(idGen.next(), -1));
        assertThrows(IllegalArgumentException.class,
                () -> Order.marketSell(idGen.next(), 0));
    }

    @Test
    void shouldRejectMarketOrderWithInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.marketBuy(0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> Order.marketSell(-1, 10.0));
    }
}