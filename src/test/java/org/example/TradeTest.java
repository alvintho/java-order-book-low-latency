package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeTest {
    @Test
    void shouldReturnTradeWithId() {
        UUID buyOrderId = UUID.randomUUID();
        UUID sellOrderId = UUID.randomUUID();

        Trade trade = new Trade(100.0, 2, buyOrderId, sellOrderId);

        assertNotNull(trade.getTradeId());
        assertDoesNotThrow(() -> UUID.fromString(trade.getTradeId().toString()));
    }

    @Test
    void shouldReturnTradeAttributes() {
        UUID buyOrderId = UUID.randomUUID();
        UUID sellOrderId = UUID.randomUUID();
        Trade trade = new Trade(100.0, 2, buyOrderId, sellOrderId);

        assertEquals(100.0, trade.getPrice());
        assertEquals(2, trade.getQuantity());
    }

    @Test
    void shouldCreateValidTimestamps() {
        long beforeTradeCreationTime = System.nanoTime();

        Trade trade1 = new Trade(100.0, 2, UUID.randomUUID(), UUID.randomUUID());
        Trade trade2 = new Trade(100.0, 2, UUID.randomUUID(), UUID.randomUUID());

        assertTrue(trade1.getTimestamp() > 0);
        assertTrue(trade1.getTimestamp() > beforeTradeCreationTime);
        assertTrue(trade2.getTimestamp() > trade1.getTimestamp());
    }
}
