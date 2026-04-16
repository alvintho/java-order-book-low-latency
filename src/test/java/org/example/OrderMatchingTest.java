package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderMatchingTest {
    private OrderBook orderBook;

    /*
    * A match occurs when:
    * BUY Price >= SELL price
    *
    * Bid: Buy 101.0, Qty 10
    * Ask: Sell 100.0, Qty 10
    *     --> MATCH!
    * */

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    void shouldMatchBidAndAskAtSamePrice() {
        Order buyOrder = new Order(100.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 10.0, Side.SELL);

        orderBook.addOrder(buyOrder);
        Trade trade = orderBook.addOrder(sellOrder);

        assertNotNull(trade);
        assertEquals(100.0, trade.getPrice());
        assertEquals(10.0, trade.getQuantity());
        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.SELL));

        assertEquals(0, orderBook.getTotalBidVolume());
        assertEquals(0, orderBook.getTotalAskVolume());
        assertEquals(0, orderBook.getTotalVolume());
    }

}
