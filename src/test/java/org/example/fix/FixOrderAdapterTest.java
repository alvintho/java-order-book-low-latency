package org.example.fix;

import org.example.domain.enums.OrderType;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.domain.model.Order;
import org.example.util.Price;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixOrderAdapterTest {

    private static final int SCALE = 100;

    // ── Two-arg constructor (orderId baked in) ────────────────────────────────

    @Test
    void shouldParseLimitBuyFromFixFields() {
        Map<Integer, String> fields = baseFields();
        fields.put(59, "1"); // GTC

        FixOrderAdapter adapter = new FixOrderAdapter(1L, SCALE);
        Order order = adapter.fromNewOrderSingle(fields);

        assertEquals("CLO-001",       order.getClOrdId());
        assertEquals("AAPL",          order.getSymbol());
        assertEquals(Side.BUY,        order.getSide());
        assertEquals(OrderType.LIMIT,  order.getOrderType());
        assertEquals(100.0,           order.getOrderQty(), 1e-9);
        assertEquals(Price.toLong(150.50, SCALE), order.getPrice());
        assertEquals(TimeInForce.GTC,  order.getTimeInForce());
        assertEquals(1L,              order.getOrderId());
    }

    @Test
    void shouldParseMarketSellFromFixFields() {
        Map<Integer, String> fields = new HashMap<>();
        fields.put(11, "CLO-002");
        fields.put(55, "MSFT");
        fields.put(54, "2");   // SELL
        fields.put(40, "1");   // MARKET
        fields.put(38, "50");
        // No price tag required for market orders

        FixOrderAdapter adapter = new FixOrderAdapter(2L, SCALE);
        Order order = adapter.fromNewOrderSingle(fields);

        assertEquals(Side.SELL,        order.getSide());
        assertEquals(OrderType.MARKET, order.getOrderType());
        assertEquals(50.0,             order.getOrderQty(), 1e-9);
        assertEquals(2L,               order.getOrderId());
    }

    @Test
    void shouldParseIocTimeInForce() {
        Map<Integer, String> fields = baseFields();
        fields.put(59, "3"); // IOC

        Order order = new FixOrderAdapter(3L, SCALE).fromNewOrderSingle(fields);
        assertEquals(TimeInForce.IOC, order.getTimeInForce());
    }

    @Test
    void shouldParseFokTimeInForce() {
        Map<Integer, String> fields = baseFields();
        fields.put(59, "4"); // FOK

        Order order = new FixOrderAdapter(4L, SCALE).fromNewOrderSingle(fields);
        assertEquals(TimeInForce.FOK, order.getTimeInForce());
    }

    @Test
    void shouldDefaultToGtcWhenTifTagAbsent() {
        Map<Integer, String> fields = baseFields();
        fields.remove(59);

        Order order = new FixOrderAdapter(5L, SCALE).fromNewOrderSingle(fields);
        assertEquals(TimeInForce.GTC, order.getTimeInForce());
    }

    // ── One-arg constructor (orderId supplied at parse time) ──────────────────

    @Test
    void shouldAcceptOrderIdAtParseTime() {
        FixOrderAdapter adapter = new FixOrderAdapter(SCALE);
        Order order = adapter.fromNewOrderSingle(10L, baseFields());

        assertEquals(10L, order.getOrderId());
    }

    @Test
    void shouldThrowWhenNoFixedIdAndNoIdProvided() {
        FixOrderAdapter adapter = new FixOrderAdapter(SCALE);
        assertThrows(IllegalStateException.class,
                () -> adapter.fromNewOrderSingle(baseFields()));
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void shouldRejectMissingRequiredField() {
        Map<Integer, String> fields = baseFields();
        fields.remove(55); // remove Symbol tag

        assertThrows(IllegalArgumentException.class,
                () -> new FixOrderAdapter(6L, SCALE).fromNewOrderSingle(fields));
    }

    @Test
    void shouldRejectUnknownSideCode() {
        Map<Integer, String> fields = baseFields();
        fields.put(54, "9"); // invalid

        assertThrows(IllegalArgumentException.class,
                () -> new FixOrderAdapter(7L, SCALE).fromNewOrderSingle(fields));
    }

    @Test
    void shouldRejectUnknownOrdTypeCode() {
        Map<Integer, String> fields = baseFields();
        fields.put(40, "9"); // invalid

        assertThrows(IllegalArgumentException.class,
                () -> new FixOrderAdapter(8L, SCALE).fromNewOrderSingle(fields));
    }

    @Test
    void shouldParseSellSideCorrectly() {
        Map<Integer, String> fields = baseFields();
        fields.put(54, "2"); // SELL

        Order order = new FixOrderAdapter(9L, SCALE).fromNewOrderSingle(fields);
        assertEquals(Side.SELL, order.getSide());
        assertFalse(order.isBid());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Base valid NewOrderSingle fields (limit buy, no TIF tag). */
    private static Map<Integer, String> baseFields() {
        Map<Integer, String> f = new HashMap<>();
        f.put(11, "CLO-001");
        f.put(55, "AAPL");
        f.put(54, "1");       // BUY
        f.put(40, "2");       // LIMIT
        f.put(38, "100");
        f.put(44, "150.50");
        return f;
    }
}