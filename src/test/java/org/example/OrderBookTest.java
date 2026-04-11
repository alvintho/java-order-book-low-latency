package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class OrderBookTest {
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    void shouldAddBuyOrderToBids() {
        Order order = new Order(UUID.randomUUID(), 100.0, 10.0, Side.BUY);

        orderBook.addOrder(order);

        assertEquals(100.0, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());
    }

    @Test
    void shouldAddSellOrderToAsks() {
        Order order = new Order(UUID.randomUUID(), 100.0, 10.0, Side.SELL);

        orderBook.addOrder(order);

        assertEquals(100.0, orderBook.getBestAsk());
        assertEquals(Double.NaN, orderBook.getBestBid());
    }

    @Test
    void shouldTrackBestBidsAsHighestPrice() {
        orderBook.addOrder(new Order(UUID.randomUUID(), 100.14, 10.0, Side.BUY));
        orderBook.addOrder(new Order(UUID.randomUUID(), 90.0, 10.0, Side.BUY));
        orderBook.addOrder(new Order(UUID.randomUUID(), 80.0, 10.0, Side.BUY));

        assertEquals(100.14, orderBook.getBestBid());
    }

    @Test
    void shouldTrackBestAsksAsLowestPrice() {
        orderBook.addOrder(new Order(UUID.randomUUID(), 100.14, 10.0, Side.SELL));
        orderBook.addOrder(new Order(UUID.randomUUID(), 90.0, 10.0, Side.SELL));
        orderBook.addOrder(new Order(UUID.randomUUID(), 80.23, 10.0, Side.SELL));

        assertEquals(80.23, orderBook.getBestAsk());
    }

    @Test
    void shouldGroupOrderAtSamePriceLevel() {
        orderBook.addOrder(new Order(UUID.randomUUID(), 100.0, 10.0, Side.BUY));
        orderBook.addOrder(new Order(UUID.randomUUID(), 100.0, 20.0, Side.BUY));

        assertEquals(2, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
    }
}