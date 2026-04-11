package org.example;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    @Test
    void shouldReturnOrderWithId() {
        Order order = new Order(20, 1, Side.BUY);

        assertNotNull(order.getOrderId());
        assertDoesNotThrow(() -> UUID.fromString(order.getOrderId().toString()));
    }

    @Test
    void shouldReturnOrderAttributes() {
        Order order = new Order(100.0, 2, Side.BUY);

        assertEquals(100.0, order.getPrice());
        assertEquals(2, order.getQuantity());
        assertEquals(Side.BUY, order.getSide());
        assertTrue(order.isBid());
    }

    @Test
    void shouldCreateValidTimestamps() {
        long beforeOrderCreationTime = System.nanoTime();

        Order order1 = new Order(100.0, 2, Side.BUY);
        Order order2 = new Order(100.0, 2, Side.SELL);

        assertTrue(order1.getTimestamp() > 0);
        assertTrue(order1.getTimestamp() > beforeOrderCreationTime);
        assertTrue(order2.getTimestamp() > order1.getTimestamp());
    }

    @Test
    void shouldThrowExceptionForNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Order(100.0, -1, Side.BUY));
    }

    @Test
    void shouldThrowExceptionForNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Order(-100.0, 1, Side.BUY));
    }
}
