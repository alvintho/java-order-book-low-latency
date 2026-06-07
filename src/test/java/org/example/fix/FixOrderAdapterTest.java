package org.example.fix;

import org.example.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FixOrderAdapterTest {

    @Test
    void shouldParseLimitBuyFromFixFields() {
        Map<Integer, String> fields = new HashMap<>();
        fields.put(11, "CLO-001");
        fields.put(55, "AAPL");
        fields.put(54, "1");  // BUY
        fields.put(40, "2");  // LIMIT
        fields.put(38, "100");
        fields.put(44, "150.50");
        fields.put(59, "1");  // GTC

        FixOrderAdapter adapter = new FixOrderAdapter(1L, 100);
        Order order = adapter.fromNewOrderSingle(fields);

        assertEquals("CLO-001", order.getClOrdId());
        assertEquals("AAPL", order.getSymbol());
        assertEquals(Side.BUY, order.getSide());
        assertEquals(OrderType.LIMIT, order.getOrderType());
        assertEquals(100.0, order.getQuantity());
        assertEquals(15050L, order.getPrice());
        assertEquals(TimeInForce.GTC, order.getTimeInForce());
    }

    @Test
    void shouldParseMarketSellFromFixFields() {
        Map<Integer, String> fields = new HashMap<>();
        fields.put(11, "CLO-002");
        fields.put(55, "MSFT");
        fields.put(54, "2");  // SELL
        fields.put(40, "1");  // MARKET
        fields.put(38, "50");

        FixOrderAdapter adapter = new FixOrderAdapter(2L, 100);
        Order order = adapter.fromNewOrderSingle(fields);

        assertEquals(Side.SELL, order.getSide());
        assertEquals(OrderType.MARKET, order.getOrderType());
        assertEquals(50.0, order.getQuantity());
    }

    @Test
    void shouldParseIocTimeInForce() {
        Map<Integer, String> fields = new HashMap<>();
        fields.put(11, "CLO-003");
        fields.put(55, "AAPL");
        fields.put(54, "1");
        fields.put(40, "2");
        fields.put(38, "10");
        fields.put(44, "100.00");
        fields.put(59, "3");  // IOC

        FixOrderAdapter adapter = new FixOrderAdapter(3L, 100);
        Order order = adapter.fromNewOrderSingle(fields);

        assertEquals(TimeInForce.IOC, order.getTimeInForce());
    }

    @Test
    void shouldRejectMissingRequiredField() {
        Map<Integer, String> fields = new HashMap<>();
        fields.put(11, "CLO-004");
        // Missing symbol (55)
        fields.put(54, "1");
        fields.put(40, "2");
        fields.put(38, "10");
        fields.put(44, "100.00");

        FixOrderAdapter adapter = new FixOrderAdapter(4L, 100);
        assertThrows(IllegalArgumentException.class,
                () -> adapter.fromNewOrderSingle(fields));
    }
}
