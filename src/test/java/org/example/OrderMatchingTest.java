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
    void shouldMatchWhenBidAndAskAtSamePrice() {
        /*
        * Bid Price = Ask Price
        * Should match
        * */
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

    @Test
    void shouldMatchWhenBidPriceHigherThanSellPrice() {
        /*
         * Bid Price >= Ask Price
         * Should match
         * */
        Order buyOrder = new Order(101.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 10.0, Side.SELL);

        orderBook.addOrder(sellOrder);
        Trade trade = orderBook.addOrder(buyOrder);

        assertNotNull(trade);
        assertEquals(100.0, trade.getPrice());
        assertEquals(10.0, trade.getQuantity());

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());
        assertEquals(0, orderBook.getTotalVolume());
    }

    @Test
    void shouldNotMatchWhenBidPriceLowerThanSellPrice() {
        /*
         * Bid Price < Ask Price
         * Should NOT match
         * */
        Order buyOrder = new Order(99.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 10.0, Side.SELL);

        orderBook.addOrder(sellOrder);
        Trade trade = orderBook.addOrder(buyOrder);

        assertNull(trade);
        assertEquals(99.0, orderBook.getBestBid());
        assertEquals(100.0, orderBook.getBestAsk());
        assertEquals(20.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldPartiallyFillWhenIncomingOrderVolumeIsSmaller() {
        Order buyOrder = new Order(100.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 5.0, Side.SELL);

        orderBook.addOrder(buyOrder);
        Trade trade = orderBook.addOrder(sellOrder);

        assertNotNull(trade);
        assertEquals(100.0, trade.getPrice());
        assertEquals(5.0, trade.getQuantity());

        assertEquals(100.0, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());

        assertEquals(5.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalAskVolume());
        assertEquals(5.0, orderBook.getTotalVolume());
    }

}
