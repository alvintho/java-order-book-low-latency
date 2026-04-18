package org.example;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeTest {
    @Test
    void shouldReturnTradeWithId() {
        Trade trade = new Trade(100.0, 2);

        assertNotNull(trade.getTradeId());
        assertDoesNotThrow(() -> UUID.fromString(trade.getTradeId().toString()));
    }

    @Test
    void shouldReturnTradeAttributes() {
        Trade trade = new Trade(100.0, 2);

        assertEquals(100.0, trade.getPrice());
        assertEquals(2, trade.getQuantity());
    }

    @Test
    void shouldCreateValidTimestamps() {
        long beforeTradeCreationTime = System.nanoTime();

        Trade trade1 = new Trade(100.0, 2);
        Trade trade2 = new Trade(100.0, 2);

        assertTrue(trade1.getTimestamp() > 0);
        assertTrue(trade1.getTimestamp() > beforeTradeCreationTime);
        assertTrue(trade2.getTimestamp() > trade1.getTimestamp());
    }
}
