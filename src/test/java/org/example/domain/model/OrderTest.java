package org.example.domain.model;

import org.example.domain.enums.OrdStatus;
import org.example.domain.enums.OrderType;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private IdGenerator idGen;
    private static final int SCALE = 100;

    @BeforeEach
    void setUp() {
        idGen = new IdGenerator();
    }

    // ── ID assignment ─────────────────────────────────────────────────────────

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

    // ── Limit order attributes ────────────────────────────────────────────────

    @Test
    void shouldReturnLimitOrderAttributes() {
        Order order = Order.limitBuy(idGen.next(), 100.0, 2, SCALE);

        assertEquals(10000L,        order.getPrice());
        assertEquals(2.0,           order.getLeavesQty(), 1e-12);
        assertEquals(Side.BUY,      order.getSide());
        assertEquals(OrderType.LIMIT, order.getOrderType());
        assertTrue(order.isBid());
        assertFalse(order.isMarket());
    }

    // ── Timestamps ───────────────────────────────────────────────────────────

    @Test
    void shouldCreateValidTimestamps() {
        long beforeMs = System.currentTimeMillis();

        Order order1 = Order.limitBuy(idGen.next(),  100.0, 2, SCALE);
        Order order2 = Order.limitSell(idGen.next(), 100.0, 2, SCALE);

        // createdAt is Instant — compare via toEpochMilli
        assertTrue(order1.getCreatedAt().toEpochMilli() >= beforeMs);
        assertFalse(order1.getCreatedAt().isAfter(order2.getCreatedAt()),
                "order1 must not be created after order2");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void shouldRejectInvalidOrderId() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(0,  100.0, 1, SCALE));
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(-1, 100.0, 1, SCALE));
    }

    @Test
    void shouldThrowExceptionForNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(idGen.next(), 100.0, -1, SCALE));
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(idGen.next(), 100.0,  0, SCALE));
    }

    @Test
    void shouldThrowExceptionForNegativePrice() {
        // Negative price rejected by Order factory before reaching Price.toLong
        assertThrows(IllegalArgumentException.class,
                () -> Order.limitBuy(idGen.next(), -100.0, 1, SCALE));
    }

    // ── Market orders ─────────────────────────────────────────────────────────

    @Test
    void shouldCreateMarketBuyOrder() {
        Order order = Order.marketBuy(idGen.next(), 10.0);

        assertEquals(1L,              order.getOrderId());
        assertEquals(0L,              order.getPrice());
        assertEquals(10.0,            order.getLeavesQty(), 1e-12);
        assertEquals(Side.BUY,        order.getSide());
        assertEquals(OrderType.MARKET, order.getOrderType());
        assertEquals(TimeInForce.IOC,  order.getTimeInForce());
        assertTrue(order.isMarket());
        assertTrue(order.isBid());
    }

    @Test
    void shouldCreateMarketSellOrder() {
        Order order = Order.marketSell(idGen.next(), 10.0);

        assertEquals(1L,              order.getOrderId());
        assertEquals(0L,              order.getPrice());
        assertEquals(10.0,            order.getLeavesQty(), 1e-12);
        assertEquals(Side.SELL,        order.getSide());
        assertEquals(OrderType.MARKET, order.getOrderType());
        assertEquals(TimeInForce.IOC,  order.getTimeInForce());
        assertTrue(order.isMarket());
        assertFalse(order.isBid());
    }

    @Test
    void shouldRejectMarketOrderWithNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.marketBuy(idGen.next(), -1));
        assertThrows(IllegalArgumentException.class,
                () -> Order.marketSell(idGen.next(), 0));
    }

    @Test
    void shouldRejectMarketOrderWithInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> Order.marketBuy( 0,  10.0));
        assertThrows(IllegalArgumentException.class, () -> Order.marketSell(-1, 10.0));
    }

    // ── Fill state machine ────────────────────────────────────────────────────

    @Test
    void freshOrderShouldBeNew() {
        Order o = Order.limitBuy(idGen.next(), 100.0, 10.0, SCALE);
        assertEquals(OrdStatus.NEW, o.getOrdStatus());
        assertEquals(10.0, o.getLeavesQty(), 1e-12);
        assertEquals(0.0,  o.getCumQty(),    1e-12);
        assertFalse(o.isFilled());
    }

    @Test
    void partialFillShouldTransitionToPartiallyFilled() {
        Order o = Order.limitBuy(idGen.next(), 100.0, 10.0, SCALE);
        o.applyFill(4.0, 10000L);

        assertEquals(OrdStatus.PARTIALLY_FILLED, o.getOrdStatus());
        assertEquals(6.0,    o.getLeavesQty(), 1e-12);
        assertEquals(4.0,    o.getCumQty(),    1e-12);
        assertFalse(o.isFilled());
    }

    @Test
    void fullFillShouldTransitionToFilled() {
        Order o = Order.limitSell(idGen.next(), 100.0, 5.0, SCALE);
        o.applyFill(5.0, 10000L);

        assertEquals(OrdStatus.FILLED, o.getOrdStatus());
        assertTrue(o.isFilled());
        assertEquals(0.0, o.getLeavesQty(), 1e-12);
    }

    @Test
    void avgPxShouldBeVolumeWeighted() {
        Order o = Order.limitBuy(idGen.next(), 102.0, 10.0, SCALE);
        o.applyFill(5.0, 10000L); // 5 @ 100.00 scaled
        o.applyFill(5.0, 10200L); // 5 @ 102.00 scaled

        double expected = (5.0 * 10000 + 5.0 * 10200) / 10.0;
        assertEquals(expected, o.getAvgPx(), 1e-6);
    }

    @Test
    void cancelShouldSetCanceledStatus() {
        Order o = Order.limitBuy(idGen.next(), 100.0, 5.0, SCALE);
        o.cancel();
        assertEquals(OrdStatus.CANCELED, o.getOrdStatus());
    }

    @Test
    void cancelOnFilledOrderShouldBeNoOp() {
        Order o = Order.limitBuy(idGen.next(), 100.0, 5.0, SCALE);
        o.applyFill(5.0, 10000L);
        o.cancel();
        assertEquals(OrdStatus.FILLED, o.getOrdStatus(),
                "cancel() on a filled order must be a no-op");
    }

    @Test
    void applyFillWithZeroOrNegativeQtyShouldThrow() {
        Order o = Order.limitBuy(idGen.next(), 100.0, 10.0, SCALE);
        assertThrows(IllegalArgumentException.class, () -> o.applyFill(0.0,  10000L));
        assertThrows(IllegalArgumentException.class, () -> o.applyFill(-1.0, 10000L));
    }
}