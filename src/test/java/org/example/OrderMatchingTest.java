package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertNotNull(trades);
        assertEquals(1, trades.size());
        assertEquals(100.0, trades.getFirst().getPrice());
        assertEquals(10.0, trades.getFirst().getQuantity());
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
        List<Trade> trades = orderBook.addOrder(buyOrder);

        assertNotNull(trades);
        assertEquals(1, trades.size());
        assertEquals(100.0, trades.getFirst().getPrice());
        assertEquals(10.0, trades.getFirst().getQuantity());

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
        List<Trade> trades = orderBook.addOrder(buyOrder);

        assertEquals(0, trades.size());
        assertEquals(99.0, orderBook.getBestBid());
        assertEquals(100.0, orderBook.getBestAsk());
        assertEquals(20.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldPartiallyFillWhenIncomingOrderVolumeIsSmaller() {
        Order buyOrder = new Order(100.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 5.0, Side.SELL);

        orderBook.addOrder(buyOrder);
        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertNotNull(trades);
        assertEquals(1, trades.size());
        assertEquals(100.0, trades.getFirst().getPrice());
        assertEquals(5.0, trades.getFirst().getQuantity());

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
        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertNotNull(trades);
        assertEquals(1, trades.size());
        assertEquals(100.0, trades.getFirst().getPrice());
        assertEquals(10.0, trades.getFirst().getQuantity());

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
        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertNotNull(trades);
        assertEquals(1, trades.size());
        assertEquals(100.0, trades.getFirst().getPrice());
        assertEquals(5.0, trades.getFirst().getQuantity());

        // First buy should be filled and removed
        assertNull(orderBook.getOrder(firstBuy.getOrderId()));

        // Second buy should still be resting
        assertNotNull(orderBook.getOrder(secondBuy.getOrderId()));
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

        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertNotNull(trades);
        assertEquals(3, trades.size());

        // Verify each trade
        Trade trade1 = trades.getFirst();
        assertEquals(100.0, trade1.getPrice());
        assertEquals(3.0, trade1.getQuantity());

        Trade trade2 = trades.get(1);
        assertEquals(100.0, trade2.getPrice());
        assertEquals(4.0, trade2.getQuantity());

        Trade trade3 = trades.get(2);
        assertEquals(100.0, trade3.getPrice());
        assertEquals(3.0, trade3.getQuantity());

        assertNull(orderBook.getOrder(firstBuy.getOrderId()));
        assertNull(orderBook.getOrder(secondBuy.getOrderId()));
        assertNull(orderBook.getOrder(thirdBuy.getOrderId()));

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
        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertNotNull(trades);
        assertEquals(3, trades.size());

        // Verify each trade
        Trade trade1 = trades.getFirst();
        assertEquals(100.0, trade1.getPrice());
        assertEquals(5.0, trade1.getQuantity());

        Trade trade2 = trades.get(1);
        assertEquals(99.0, trade2.getPrice());
        assertEquals(5.0, trade2.getQuantity());

        Trade trade3 = trades.get(2);
        assertEquals(98.0, trade3.getPrice());
        assertEquals(2.0, trade3.getQuantity());

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
        List<Trade> trades = orderBook.addOrder(buyOrder);

        assertNotNull(trades);
        assertEquals(3, trades.size());

        // Verify each trade
        Trade trade1 = trades.getFirst();
        assertEquals(100.0, trade1.getPrice());
        assertEquals(5.0, trade1.getQuantity());

        Trade trade2 = trades.get(1);
        assertEquals(101.0, trade2.getPrice());
        assertEquals(5.0, trade2.getQuantity());

        Trade trade3 = trades.get(2);
        assertEquals(102.0, trade3.getPrice());
        assertEquals(2.0, trade3.getQuantity());


        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(102.0, orderBook.getBestAsk());

        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.SELL));
        assertEquals(0, orderBook.getOrderCountAtPrice(101.0, Side.SELL));
        assertEquals(1, orderBook.getOrderCountAtPrice(102.0, Side.SELL));

        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(3.0, orderBook.getTotalAskVolume());
        assertEquals(3.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldLinkTradeToParticipatingOrders() {
        Order buyOrder = new Order(100.0, 10.0, Side.BUY);
        Order sellOrder = new Order(100.0, 10.0, Side.SELL);

        orderBook.addOrder(buyOrder);
        List<Trade> trades = orderBook.addOrder(sellOrder);

        assertEquals(1, trades.size());
        Trade trade = trades.getFirst();

        assertEquals(buyOrder.getOrderId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getOrderId(), trade.getSellOrderId());
    }

}
