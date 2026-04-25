package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class OrderBookTest {
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    void shouldAddBuyOrderToBids() {
        Order order = new Order(100.0, 10.0, Side.BUY);

        orderBook.addOrder(order);

        assertEquals(100.0, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());
    }

    @Test
    void shouldAddSellOrderToAsks() {
        Order order = new Order(100.0, 10.0, Side.SELL);

        orderBook.addOrder(order);

        assertEquals(100.0, orderBook.getBestAsk());
        assertEquals(Double.NaN, orderBook.getBestBid());
    }

    @Test
    void shouldNotAddInvalidOrder() {
        assertThrows(IllegalArgumentException.class, () -> orderBook.addOrder(null));
    }

    @Test
    void shouldGetOrderFromOrderMap() {
        Order order = new Order(100.0, 10.0, Side.BUY);

        orderBook.addOrder(order);
        Order retrievedOrder = orderBook.getOrder(order.getOrderId());
        assertEquals(order.getOrderId(), retrievedOrder.getOrderId());
    }

    @Test
    void shouldNotGetNonExistingOrderFromOrderMap() {
        Order order = new Order(100.0, 10.0, Side.BUY);

        assertNull(orderBook.getOrder(order.getOrderId()));
    }

    @Test
    void shouldSuccessfullyRemoveExistingUnmatchedOrder() {
        Order order = new Order(100.0, 10.0, Side.BUY);
        Order order2 = new Order(101.0, 10.0, Side.SELL);

        orderBook.addOrder(order);
        orderBook.addOrder(order2);

        orderBook.removeOrder(order.getOrderId());
        orderBook.removeOrder(order2.getOrderId());

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());

        assertEquals(0.0, orderBook.getTotalVolume());
        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalAskVolume());

        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.SELL));
    }

    @Test
    void shouldNotRemoveNonExistingOrder() {
        Order order = new Order(100.0, 10.0, Side.BUY);

        assertThrows(IllegalStateException.class, () -> orderBook.removeOrder(order.getOrderId()));
        assertThrows(IllegalArgumentException.class, () -> orderBook.removeOrder(null));
    }


    @Test
    void shouldTrackBestBidsAsHighestPrice() {
        orderBook.addOrder(new Order(100.14, 10.0, Side.BUY));
        orderBook.addOrder(new Order(90.0, 10.0, Side.BUY));
        orderBook.addOrder(new Order(80.0, 10.0, Side.BUY));

        assertEquals(100.14, orderBook.getBestBid());
    }

    @Test
    void shouldTrackBestAsksAsLowestPrice() {
        orderBook.addOrder(new Order(100.14, 10.0, Side.SELL));
        orderBook.addOrder(new Order(90.0, 10.0, Side.SELL));
        orderBook.addOrder(new Order(80.23, 10.0, Side.SELL));

        assertEquals(80.23, orderBook.getBestAsk());
    }

    @Test
    void shouldTrackOrderCountAtPrice() {
        orderBook.addOrder(new Order(100.0, 10.0, Side.BUY));
        orderBook.addOrder(new Order(100.0, 20.0, Side.BUY));

        orderBook.addOrder(new Order(101.0, 30.0, Side.SELL));
        orderBook.addOrder(new Order(101.0, 40.0, Side.SELL));
        orderBook.addOrder(new Order(102.0, 50.0, Side.SELL));


        assertEquals(2, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(2, orderBook.getOrderCountAtPrice(101.0, Side.SELL));
        assertEquals(1, orderBook.getOrderCountAtPrice(102.0, Side.SELL));
    }

    @Test
    void shouldGetTotalVolume() {
        orderBook.addOrder(new Order(100.0, 10.0, Side.BUY));
        orderBook.addOrder(new Order(100.0, 20.0, Side.BUY));

        orderBook.addOrder(new Order(101.0, 30.0, Side.SELL));
        orderBook.addOrder(new Order(101.0, 40.0, Side.SELL));
        orderBook.addOrder(new Order(102.0, 50.0, Side.SELL));

        assertEquals(150.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldNotAddDuplicateOrder() {
        Order order = new Order(100.0, 10.0, Side.BUY);

        orderBook.addOrder(order);

        assertThrows(IllegalStateException.class, () -> orderBook.addOrder(order));

        // Verify state unchanged
        assertEquals(1, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(10.0, orderBook.getTotalBidVolume());
        assertEquals(10.0, orderBook.getTotalVolume());
    }
}