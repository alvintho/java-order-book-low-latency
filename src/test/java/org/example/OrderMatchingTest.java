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

    @Test
    void shouldPartiallyFillWhenIncomingOrderVolumeIsLarger() {
        Order buyOrder = new Order(100.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 15.0, Side.SELL);

        orderBook.addOrder(buyOrder);
        Trade trade = orderBook.addOrder(sellOrder);

        assertNotNull(trade);
        assertEquals(100.0, trade.getPrice());
        assertEquals(10.0, trade.getQuantity());

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(100.0, orderBook.getBestAsk());

        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(5.0, orderBook.getTotalAskVolume());
        assertEquals(5.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldMatchOldestOrderFirstAtSamePrice() { // price-time priority
        Order firstBuy = new Order(100.0, 5.0, Side.BUY);
        Order secondBuy = new Order(100.0, 5.0, Side.BUY);
        Order sellOrder = new Order(100.0, 5.0, Side.SELL);

        orderBook.addOrder(firstBuy);
        orderBook.addOrder(secondBuy);
        Trade trade = orderBook.addOrder(sellOrder);

        assertNotNull(trade);
        assertEquals(5.0, trade.getQuantity());

        // First buy should be filled and removed
        assertNull(orderBook.getOrder(firstBuy));

        // Second buy should still be resting
        assertNotNull(orderBook.getOrder(secondBuy));
        assertEquals(1, orderBook.getOrderCountAtPrice(100.0, Side.BUY));

        assertEquals(5.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalAskVolume());
        assertEquals(5.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldMatchAcrossMultipleOrdersAtSamePrice() {
        Order firstBuy = new Order(100.0, 3.0, Side.BUY);
        Order secondBuy = new Order(100.0, 4.0, Side.BUY);
        Order thirdBuy = new Order(100.0, 3.0, Side.BUY);

        orderBook.addOrder(firstBuy);
        orderBook.addOrder(secondBuy);
        orderBook.addOrder(thirdBuy);

        Order sellOrder = new Order(100.0, 10.0, Side.SELL);

        Trade trade = orderBook.addOrder(sellOrder);

        assertNotNull(trade);
        assertEquals(100.0, trade.getPrice());
        assertEquals(10.0, trade.getQuantity());

        assertNull(orderBook.getOrder(firstBuy));
        assertNull(orderBook.getOrder(secondBuy));
        assertNull(orderBook.getOrder(thirdBuy));

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());

        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalAskVolume());
        assertEquals(0.0, orderBook.getTotalVolume());
    }
    @Test
    void shouldMatchIncomingAskAcrossMultipleBidLevels() {
        orderBook.addOrder(new Order(100.0, 5.0, Side.BUY));
        orderBook.addOrder(new Order(99.0, 5.0, Side.BUY));
        orderBook.addOrder(new Order(98.0, 5.0, Side.BUY));

        // Sell 12 should consume:
        // - 5 at 100.0 (best bid)
        // - 5 at 99.0
        // - 2 at 98.0 (partial)
        Order sellOrder = new Order(98.0, 12.0, Side.SELL);
        Trade trade = orderBook.addOrder(sellOrder);

        assertNotNull(trade);
        assertEquals(12.0, trade.getQuantity());

        // Only 3 remaining from the 98.0 buy order
        assertEquals(98.0, orderBook.getBestBid());
        assertEquals(1, orderBook.getOrderCountAtPrice(98.0, Side.BUY));
        assertEquals(0, orderBook.getOrderCountAtPrice(99.0, Side.BUY));
        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.BUY));

        assertEquals(Double.NaN, orderBook.getBestAsk());

        assertEquals(3.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalAskVolume());
        assertEquals(3.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldMatchIncomingBuyAcrossMultipleAskLevels() {
        orderBook.addOrder(new Order(100.0, 5.0, Side.SELL));
        orderBook.addOrder(new Order(101.0, 5.0, Side.SELL));
        orderBook.addOrder(new Order(102.0, 5.0, Side.SELL));

        // Buy 12 should consume:
        // - 5 at 100.0 (best ask)
        // - 5 at 101.0
        // - 2 at 102.0 (partial)
        Order buyOrder = new Order(102.0, 12.0, Side.BUY);
        Trade trade = orderBook.addOrder(buyOrder);

        assertNotNull(trade);
        assertEquals(12.0, trade.getQuantity());

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(102.0, orderBook.getBestAsk());

        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.SELL));
        assertEquals(0, orderBook.getOrderCountAtPrice(101.0, Side.SELL));
        assertEquals(1, orderBook.getOrderCountAtPrice(102.0, Side.SELL));

        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(3.0, orderBook.getTotalAskVolume());
        assertEquals(3.0, orderBook.getTotalVolume());
    }

}
