package org.example.domain.model;

import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TradeTest {

    private IdGenerator orderIdGen;
    private IdGenerator tradeIdGen;

    @BeforeEach
    void setUp() {
        orderIdGen = new IdGenerator();
        tradeIdGen = new IdGenerator();
    }

    @Test
    void shouldReturnTradeWithId() {
        Trade trade = new Trade(tradeIdGen.next(), 10000L, 2,
                orderIdGen.next(), orderIdGen.next());
        assertEquals(1L, trade.getTradeId());
    }

    @Test
    void shouldReturnTradeAttributes() {
        long buyId  = orderIdGen.next();
        long sellId = orderIdGen.next();
        Trade trade = new Trade(tradeIdGen.next(), 10000L, 2.0, buyId, sellId);

        assertEquals(10000L, trade.getPrice());
        assertEquals(2.0,    trade.getQuantity(), 1e-12);
        assertEquals(buyId,  trade.getBuyOrderId());
        assertEquals(sellId, trade.getSellOrderId());
    }

    @Test
    void shouldCreateValidTimestamps() {
        Instant before = Instant.now();

        Trade trade1 = new Trade(tradeIdGen.next(), 10000L, 2,
                orderIdGen.next(), orderIdGen.next());
        Trade trade2 = new Trade(tradeIdGen.next(), 10000L, 2,
                orderIdGen.next(), orderIdGen.next());

        assertFalse(trade1.getExecutedAt().isBefore(before),
                "trade1 executedAt must not be before test start");
        assertFalse(trade2.getExecutedAt().isBefore(trade1.getExecutedAt()),
                "trade2 must not be executed before trade1");
    }
}