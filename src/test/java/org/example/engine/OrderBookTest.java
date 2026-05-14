package org.example.engine;

import org.example.model.Instrument;
import org.example.model.Order;
import org.example.model.Side;
import org.example.model.Trade;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class OrderBookTest {
    private int instrumentScale;
    private OrderBook orderBook;
    private IdGenerator orderIdGen;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        instrumentScale = instrument.getScale();
        orderBook = new OrderBook(instrument);
        orderIdGen = new IdGenerator();
    }

    @Test
    void shouldAddBuyOrderToBids() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        orderBook.addOrder(order);

        assertEquals(100.0, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());
    }

    @Test
    void shouldAddSellOrderToAsks() {
        Order order = Order.limitSell(orderIdGen.next(), 100.0, 10.0, instrumentScale);

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
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        orderBook.addOrder(order);
        Order retrievedOrder = orderBook.getOrder(order.getOrderId());
        assertEquals(order.getOrderId(), retrievedOrder.getOrderId());
    }

    @Test
    void shouldNotGetNonExistingOrderFromOrderMap() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        assertNull(orderBook.getOrder(order.getOrderId()));
    }

    @Test
    void shouldSuccessfullyCancelExistingUnmatchedOrder() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        Order order2 = Order.limitSell(orderIdGen.next(),101.0, 10.0, instrumentScale);

        orderBook.addOrder(order);
        orderBook.addOrder(order2);

        orderBook.cancelOrder(order.getOrderId());
        orderBook.cancelOrder(order2.getOrderId());

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());

        assertEquals(0.0, orderBook.getTotalVolume());
        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalAskVolume());

        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.SELL));
    }

    @Test
    void shouldNotCancelNonExistingOrder() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        assertThrows(IllegalStateException.class, () -> orderBook.cancelOrder(order.getOrderId()));
        assertThrows(IllegalArgumentException.class, () -> orderBook.cancelOrder(0));
    }


    @Test
    void shouldTrackBestBidsAsHighestPrice() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.14, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 90.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 80.0, 10.0, instrumentScale));

        assertEquals(100.14, orderBook.getBestBid());
    }

    @Test
    void shouldTrackBestAsksAsLowestPrice() {
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 100.14, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 90.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 80.23, 10.0, instrumentScale));

        assertEquals(80.23, orderBook.getBestAsk());
    }

    @Test
    void shouldTrackOrderCountAtPrice() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(),100.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 20.0, instrumentScale));

        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 30.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 40.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 102.0, 50.0, instrumentScale));


        assertEquals(2, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(2, orderBook.getOrderCountAtPrice(101.0, Side.SELL));
        assertEquals(1, orderBook.getOrderCountAtPrice(102.0, Side.SELL));
    }

    @Test
    void shouldGetTotalVolume() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 20.0, instrumentScale));

        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 30.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 40.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 102.0, 50.0, instrumentScale));

        assertEquals(150.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldNotAddDuplicateOrder() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        orderBook.addOrder(order);

        assertThrows(IllegalStateException.class, () -> orderBook.addOrder(order));

        // Verify state unchanged
        assertEquals(1, orderBook.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(10.0, orderBook.getTotalBidVolume());
        assertEquals(10.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldModifyOrderPrice() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        orderBook.addOrder(order);

        Order modifiedOrder = Order.limitBuy(orderIdGen.next(), 105.0, 10.0, instrumentScale);

        orderBook.modifyOrder(order.getOrderId(), modifiedOrder);

        // Old order gone
        assertNull(orderBook.getOrder(order.getOrderId()));
        assertEquals(0, orderBook.getOrderCountAtPrice(100.0, Side.BUY));

        // New order present
        assertNotNull(orderBook.getOrder(modifiedOrder.getOrderId()));
        assertEquals(105.0, orderBook.getBestBid());
        assertEquals(1, orderBook.getOrderCountAtPrice(105.0, Side.BUY));

        assertEquals(10.0, orderBook.getTotalBidVolume());
        assertEquals(10.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldModifyOrderQuantity() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        orderBook.addOrder(order);

        Order modifiedOrder = Order.limitBuy(orderIdGen.next(), 100.0, 20.0, instrumentScale);
        orderBook.modifyOrder(order.getOrderId(), modifiedOrder);

        assertNull(orderBook.getOrder(order.getOrderId()));
        assertNotNull(orderBook.getOrder(modifiedOrder.getOrderId()));

        assertEquals(100.0, orderBook.getBestBid());
        assertEquals(20.0, orderBook.getTotalBidVolume());
        assertEquals(20.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldNotModifyNonExistingOrder() {
        long orderId = orderIdGen.next();
        Order newOrder = Order.limitBuy(orderId, 100.0, 10.0, instrumentScale);

        assertThrows(IllegalStateException.class, () -> orderBook.modifyOrder(orderId, newOrder));
    }

    @Test
    void shouldNotModifyOrderWithDifferentSide() {
        Order order = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        orderBook.addOrder(order);

        Order modifiedOrder = Order.limitSell(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        assertThrows(IllegalArgumentException.class,
                () -> orderBook.modifyOrder(order.getOrderId(), modifiedOrder));
    }

    @Test
    void shouldMatchWhenModifiedOrderCrossesSpread() {
        Order buyOrder = Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale);
        Order sellOrder = Order.limitSell(orderIdGen.next(), 100.0, 10.0, instrumentScale);

        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);

        Order modifiedBuyOrder = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        List<Trade> trades = orderBook.modifyOrder(buyOrder.getOrderId(), modifiedBuyOrder);

        assertNotNull(trades);
        assertEquals(1, trades.size());
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(10.0, trades.getFirst().getQuantity());

        assertEquals(Double.NaN, orderBook.getBestBid());
        assertEquals(Double.NaN, orderBook.getBestAsk());
        assertEquals(0, orderBook.getTotalVolume());
    }

    @Test
    void shouldGetVolumeAtPriceLevel() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 20.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 15.0, instrumentScale));

        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 5.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 8.0, instrumentScale));

        assertEquals(45.0, orderBook.getVolumeAtPrice(100.0, Side.BUY));
        assertEquals(13.0, orderBook.getVolumeAtPrice(101.0, Side.SELL));
        assertEquals(0.0, orderBook.getVolumeAtPrice(99.0, Side.BUY));
    }

    @Test
    void shouldCalculateSpread() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 10.0, instrumentScale));

        assertEquals(2.0, orderBook.getSpread());
    }

    @Test
    void shouldReturnNaNSpreadWhenOneSideEmpty() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale));

        assertEquals(Double.NaN, orderBook.getSpread());
    }

    @Test
    void shouldReturnNaNSpreadWhenBookEmpty() {
        assertEquals(Double.NaN, orderBook.getSpread());
    }

    @Test
    void shouldUpdateSpreadAfterCancel() {
        Order bid1 = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        Order bid2 = Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale);
        Order ask = Order.limitSell(orderIdGen.next(), 101.0, 10.0, instrumentScale);

        orderBook.addOrder(bid1);
        orderBook.addOrder(bid2);
        orderBook.addOrder(ask);

        assertEquals(1.0, orderBook.getSpread());

        orderBook.cancelOrder(bid1.getOrderId());

        assertEquals(2.0, orderBook.getSpread());
    }

    @Test
    void shouldGetBookDepth() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 100.0, 5.0, instrumentScale));  // same level
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 98.0, 10.0, instrumentScale));

        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 102.0, 10.0, instrumentScale));

        assertEquals(3, orderBook.getDepth(Side.BUY));
        assertEquals(2, orderBook.getDepth(Side.SELL));
    }

    @Test
    void shouldReturnZeroDepthWhenSideEmpty() {
        assertEquals(0, orderBook.getDepth(Side.BUY));
        assertEquals(0, orderBook.getDepth(Side.SELL));
    }

    @Test
    void shouldUpdateDepthAfterCancel() {
        Order order1 = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        Order order2 = Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale);

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);

        assertEquals(2, orderBook.getDepth(Side.BUY));

        orderBook.cancelOrder(order1.getOrderId());

        assertEquals(1, orderBook.getDepth(Side.BUY));
    }

    @Test
    void shouldTrackTotalTradeCount() {
        Order order1 = Order.limitBuy(orderIdGen.next(), 100.0, 10.0, instrumentScale);
        Order order2 = Order.limitSell(orderIdGen.next(), 100.0, 5.0, instrumentScale);
        Order order3 = Order.limitSell(orderIdGen.next(), 100.0, 5.0, instrumentScale);
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);

        assertEquals(2, orderBook.getTradeCounts());
    }

    @Test
    void shouldTrackZeroTradesWhenNoMatches() {
        orderBook.addOrder(Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale));
        orderBook.addOrder(Order.limitSell(orderIdGen.next(), 101.0, 10.0, instrumentScale));

        assertEquals(0, orderBook.getTradeCounts());
    }

    @Test
    void shouldAssociateWithInstrument() {
        assertEquals("AAPL", orderBook.getInstrument().getSymbol());
        assertEquals(100, orderBook.getInstrument().getScale());
        assertEquals(1, orderBook.getInstrument().getLotSize());
    }

    @Test
    void shouldRejectNullInstrument() {
        assertThrows(IllegalArgumentException.class, () -> new OrderBook(null));
    }

    @Test
    void shouldReturnEmptyTradesWhenNoMatch() {
        Order buyOrder = Order.limitBuy(orderIdGen.next(), 99.0, 10.0, instrumentScale);
        Order sellOrder = Order.limitSell(orderIdGen.next(), 101.0, 10.0, instrumentScale);

        List<Trade> trades1 = orderBook.addOrder(buyOrder);
        List<Trade> trades2 = orderBook.addOrder(sellOrder);

        assertTrue(trades1.isEmpty());
        assertTrue(trades2.isEmpty());
    }

}