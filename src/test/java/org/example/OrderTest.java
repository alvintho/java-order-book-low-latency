package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {
    private IdGenerator idGen;

    @BeforeEach
    void setUp() {
        idGen = new IdGenerator();
    }

    @Test
    void shouldReturnOrderWithId() {
        Order order = new Order(idGen.next(), 20.0, 1, Side.BUY, 100);

        assertEquals(1L, order.getOrderId());
    }

    @Test
    void shouldReturnSequentialIds() {
        Order order1 = new Order(idGen.next(), 100.0, 2, Side.BUY, 100);
        Order order2 = new Order(idGen.next(), 100.0, 3, Side.SELL, 100);

        assertEquals(1L, order1.getOrderId());
        assertEquals(2L, order2.getOrderId());
    }

    @Test
    void shouldReturnOrderAttributes() {
        Order order = new Order(idGen.next(), 100.0, 2, Side.BUY, 100);

        assertEquals(10000L, order.getPrice());
        assertEquals(2, order.getQuantity());
        assertEquals(Side.BUY, order.getSide());
        assertTrue(order.isBid());
    }

    @Test
    void shouldCreateValidTimestamps() {
        long beforeOrderCreationTime = System.nanoTime();

        Order order1 = new Order(idGen.next(), 100.0, 2, Side.BUY, 100);
        Order order2 = new Order(idGen.next(), 100.0, 2, Side.SELL, 100);

        assertTrue(order1.getTimestamp() > 0);
        assertTrue(order1.getTimestamp() > beforeOrderCreationTime);
        assertTrue(order2.getTimestamp() > order1.getTimestamp());
    }

    @Test
    void shouldThrowExceptionForNegativeId() {
        assertThrows(IllegalArgumentException.class, () -> new Order(idGen.next() - 100,100.0, 1, Side.BUY, 100));
    }

    @Test
    void shouldThrowExceptionForNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Order(idGen.next(),100.0, -1, Side.BUY, 100));
    }

    @Test
    void shouldThrowExceptionForNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Order(idGen.next(), -100.0, 1, Side.BUY, 100));
    }

    @Test
    void shouldRejectPriceBeyondScale() {
        assertThrows(IllegalArgumentException.class, () -> new Order(idGen.next(), 100.001, 1, Side.BUY, 100));
    }
}