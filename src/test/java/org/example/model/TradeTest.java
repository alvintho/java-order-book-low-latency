package org.example.model;

import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeTest {
    private IdGenerator orderIdGen;
    private IdGenerator tradeIdGen;

    @BeforeEach
    void setUp() {
        orderIdGen = new IdGenerator();
        tradeIdGen = new IdGenerator();
    }

    @Test
    void shouldReturnTradeWithId() {
        long buyOrderId = orderIdGen.next();
        long  sellOrderId = orderIdGen.next();

        Trade trade = new Trade(tradeIdGen.next(),10000L, 2, buyOrderId, sellOrderId);

        assertEquals(1, trade.getTradeId());
    }

    @Test
    void shouldReturnTradeAttributes() {
        long buyOrderId = orderIdGen.next();
        long sellOrderId = orderIdGen.next();
        Trade trade = new Trade(tradeIdGen.next(),10000L, 2, buyOrderId, sellOrderId);

        assertEquals(10000L, trade.getPrice());
        assertEquals(2, trade.getQuantity());
    }

    @Test
    void shouldCreateValidTimestamps() {
        long beforeTradeCreationTime = System.nanoTime();

        Trade trade1 = new Trade(tradeIdGen.next(),10000L, 2, orderIdGen.next(), orderIdGen.next());
        Trade trade2 = new Trade(tradeIdGen.next(),10000L, 2, orderIdGen.next(), orderIdGen.next());

        assertTrue(trade1.getTimestamp() > 0);
        assertTrue(trade1.getTimestamp() > beforeTradeCreationTime);
        assertTrue(trade2.getTimestamp() > trade1.getTimestamp());
    }
}
