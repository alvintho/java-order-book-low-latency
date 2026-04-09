package org.example;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class OrderBookTest {

    @Test
    public void testNewOrderBookShouldBeEmpty() {
        OrderBook orderBook = new OrderBook();
        assertEquals(0, orderBook.getTotalVolume());
    }
}