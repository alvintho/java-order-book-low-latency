package org.example.engine;

import org.example.domain.enums.Side;
import org.example.domain.model.Instrument;
import org.example.domain.model.Order;
import org.example.domain.model.Trade;
import org.example.domain.port.BaseOrderBook;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Matching semantics: a trade occurs when bid price >= ask price.
 */
class OrderMatchingTest {

    private int       scale;
    private BaseOrderBook book;
    private IdGenerator idGen;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        scale = instrument.getScale();
        book  = new PriceTimePriorityOrderBook(instrument, new IdGenerator(), r -> {});
        idGen = new IdGenerator();
    }

    // ── Basic matching ────────────────────────────────────────────────────────

    @Test
    void shouldMatchWhenBidAndAskAtSamePrice() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(buy);
        List<Trade> trades = book.addOrder(sell);

        assertEquals(1,      trades.size());
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(10.0,   trades.getFirst().getQuantity(), 1e-12);
        assertEquals(0, book.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(0, book.getOrderCountAtPrice(100.0, Side.SELL));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void shouldMatchWhenBidPriceHigherThanAskPrice() {
        Order sell = Order.limitSell(idGen.next(), 100.0, 10.0, scale);
        Order buy  = Order.limitBuy (idGen.next(), 101.0, 10.0, scale);
        book.addOrder(sell);
        List<Trade> trades = book.addOrder(buy);

        assertEquals(1,      trades.size());
        // Trade executes at the resting ask price (100.0)
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(10.0,   trades.getFirst().getQuantity(), 1e-12);
        assertTrue(Double.isNaN(book.getBestBid()));
        assertTrue(Double.isNaN(book.getBestAsk()));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void shouldNotMatchWhenBidPriceLowerThanAskPrice() {
        Order buy  = Order.limitBuy (idGen.next(),  99.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(sell);
        List<Trade> trades = book.addOrder(buy);

        assertTrue(trades.isEmpty());
        assertEquals(99.0,  book.getBestBid(), 1e-9);
        assertEquals(100.0, book.getBestAsk(), 1e-9);
        assertEquals(20.0,  book.getTotalVolume(), 1e-12);
    }

    // ── Partial fills ─────────────────────────────────────────────────────────

    @Test
    void shouldPartiallyFillWhenIncomingOrderIsSmaller() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 100.0,  5.0, scale);
        book.addOrder(buy);
        List<Trade> trades = book.addOrder(sell);

        assertEquals(1,      trades.size());
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(5.0,    trades.getFirst().getQuantity(), 1e-12);
        assertEquals(100.0,  book.getBestBid(), 1e-9);
        assertTrue(Double.isNaN(book.getBestAsk()));
        assertEquals(5.0, book.getTotalBidVolume(), 1e-12);
        assertEquals(0.0, book.getTotalAskVolume(), 1e-12);
        assertEquals(5.0, book.getTotalVolume(),    1e-12);
    }

    @Test
    void shouldPartiallyFillWhenIncomingOrderIsLarger() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 100.0, 15.0, scale);
        book.addOrder(buy);
        List<Trade> trades = book.addOrder(sell);

        assertEquals(1,      trades.size());
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(10.0,   trades.getFirst().getQuantity(), 1e-12);
        assertTrue(Double.isNaN(book.getBestBid()));
        assertEquals(100.0,  book.getBestAsk(), 1e-9);
        assertEquals(0.0, book.getTotalBidVolume(), 1e-12);
        assertEquals(5.0, book.getTotalAskVolume(), 1e-12);
        assertEquals(5.0, book.getTotalVolume(),    1e-12);
    }

    // ── Price-time priority ───────────────────────────────────────────────────

    @Test
    void shouldMatchOldestOrderFirstAtSamePrice() {
        Order first  = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        Order second = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        Order sell   = Order.limitSell(idGen.next(), 100.0, 5.0, scale);

        book.addOrder(first);
        book.addOrder(second);
        List<Trade> trades = book.addOrder(sell);

        assertEquals(1, trades.size());
        assertEquals(first.getOrderId(), trades.getFirst().getBuyOrderId(),
                "Oldest buy should fill first");
        assertNull(book.getOrder(first.getOrderId()),  "first should be filled and removed");
        assertNotNull(book.getOrder(second.getOrderId()), "second should still rest");
        assertEquals(1, book.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(5.0, book.getTotalBidVolume(), 1e-12);
    }

    @Test
    void shouldMatchAcrossMultipleOrdersAtSamePrice() {
        Order b1 = Order.limitBuy(idGen.next(), 100.0, 3.0, scale);
        Order b2 = Order.limitBuy(idGen.next(), 100.0, 4.0, scale);
        Order b3 = Order.limitBuy(idGen.next(), 100.0, 3.0, scale);
        book.addOrder(b1);
        book.addOrder(b2);
        book.addOrder(b3);

        List<Trade> trades = book.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        assertEquals(3, trades.size());
        assertEquals(3.0, trades.get(0).getQuantity(), 1e-12);
        assertEquals(4.0, trades.get(1).getQuantity(), 1e-12);
        assertEquals(3.0, trades.get(2).getQuantity(), 1e-12);
        assertNull(book.getOrder(b1.getOrderId()));
        assertNull(book.getOrder(b2.getOrderId()));
        assertNull(book.getOrder(b3.getOrderId()));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    // ── Multi-level matching ──────────────────────────────────────────────────

    @Test
    void shouldMatchIncomingAskAcrossMultipleBidLevels() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 5.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(),  99.0, 5.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(),  98.0, 5.0, scale));

        // Sell 12: consumes 5@100, 5@99, 2@98
        List<Trade> trades = book.addOrder(Order.limitSell(idGen.next(), 98.0, 12.0, scale));

        assertEquals(3, trades.size());
        assertEquals(10000L, trades.get(0).getPrice()); assertEquals(5.0, trades.get(0).getQuantity(), 1e-12);
        assertEquals(9900L,  trades.get(1).getPrice()); assertEquals(5.0, trades.get(1).getQuantity(), 1e-12);
        assertEquals(9800L,  trades.get(2).getPrice()); assertEquals(2.0, trades.get(2).getQuantity(), 1e-12);

        assertEquals(98.0, book.getBestBid(), 1e-9);
        assertEquals(3.0,  book.getTotalBidVolume(), 1e-12);
        assertEquals(0.0,  book.getTotalAskVolume(), 1e-12);
    }

    @Test
    void shouldMatchIncomingBuyAcrossMultipleAskLevels() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 5.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 102.0, 5.0, scale));

        // Buy 12: consumes 5@100, 5@101, 2@102
        List<Trade> trades = book.addOrder(Order.limitBuy(idGen.next(), 102.0, 12.0, scale));

        assertEquals(3, trades.size());
        assertEquals(10000L, trades.get(0).getPrice()); assertEquals(5.0, trades.get(0).getQuantity(), 1e-12);
        assertEquals(10100L, trades.get(1).getPrice()); assertEquals(5.0, trades.get(1).getQuantity(), 1e-12);
        assertEquals(10200L, trades.get(2).getPrice()); assertEquals(2.0, trades.get(2).getQuantity(), 1e-12);

        assertEquals(102.0, book.getBestAsk(), 1e-9);
        assertEquals(3.0,   book.getTotalAskVolume(), 1e-12);
        assertEquals(0.0,   book.getTotalBidVolume(), 1e-12);
    }

    // ── Trade linkage ─────────────────────────────────────────────────────────

    @Test
    void shouldLinkTradeToParticipatingOrders() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(buy);
        List<Trade> trades = book.addOrder(sell);

        assertEquals(1, trades.size());
        assertEquals(buy.getOrderId(),  trades.getFirst().getBuyOrderId());
        assertEquals(sell.getOrderId(), trades.getFirst().getSellOrderId());
    }

    // ── Market orders ─────────────────────────────────────────────────────────

    @Test
    void shouldMatchMarketBuyAgainstBestAsk() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));
        List<Trade> trades = book.addOrder(Order.marketBuy(idGen.next(), 10.0));

        assertEquals(1,      trades.size());
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(10.0,   trades.getFirst().getQuantity(), 1e-12);
        assertTrue(Double.isNaN(book.getBestAsk()));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void shouldMatchMarketSellAgainstBestBid() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 10.0, scale));
        List<Trade> trades = book.addOrder(Order.marketSell(idGen.next(), 10.0));

        assertEquals(1,      trades.size());
        assertEquals(10000L, trades.getFirst().getPrice());
        assertEquals(10.0,   trades.getFirst().getQuantity(), 1e-12);
        assertTrue(Double.isNaN(book.getBestBid()));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void shouldCancelMarketOrderWhenBookEmpty() {
        Order mkt = Order.marketBuy(idGen.next(), 10.0);
        List<Trade> trades = book.addOrder(mkt);

        assertTrue(trades.isEmpty());
        assertNull(book.getOrder(mkt.getOrderId()));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void shouldPartialFillMarketOrderAndCancelRemainder() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));
        Order mkt = Order.marketBuy(idGen.next(), 10.0);
        List<Trade> trades = book.addOrder(mkt);

        assertEquals(1,   trades.size());
        assertEquals(5.0, trades.getFirst().getQuantity(), 1e-12);
        assertTrue(Double.isNaN(book.getBestAsk()));
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
        assertNull(book.getOrder(mkt.getOrderId()));
    }

    @Test
    void shouldMatchMarketOrderAcrossMultiplePriceLevels() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 5.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 102.0, 5.0, scale));

        Order mkt = Order.marketBuy(idGen.next(), 12.0);
        List<Trade> trades = book.addOrder(mkt);

        assertEquals(3, trades.size());
        assertEquals(10000L, trades.get(0).getPrice()); assertEquals(5.0, trades.get(0).getQuantity(), 1e-12);
        assertEquals(10100L, trades.get(1).getPrice()); assertEquals(5.0, trades.get(1).getQuantity(), 1e-12);
        assertEquals(10200L, trades.get(2).getPrice()); assertEquals(2.0, trades.get(2).getQuantity(), 1e-12);
        assertEquals(102.0,  book.getBestAsk(), 1e-9);
        assertEquals(3.0,    book.getTotalAskVolume(), 1e-12);
        assertNull(book.getOrder(mkt.getOrderId()));
    }

    @Test
    void shouldNotAddMarketOrderToOrderMap() {
        Order mkt = Order.marketBuy(idGen.next(), 10.0);
        book.addOrder(mkt);
        assertNull(book.getOrder(mkt.getOrderId()));
    }

    @Test
    void shouldRejectCancelOfMarketOrder() {
        // Market orders never enter the orderMap, so cancel throws
        Order mkt = Order.marketBuy(idGen.next(), 10.0);
        book.addOrder(mkt);
        assertThrows(NoSuchElementException.class,
                () -> book.cancelOrder(mkt.getOrderId()));
    }
}