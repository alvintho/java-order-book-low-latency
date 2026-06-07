package org.example.engine;

import org.example.domain.enums.Side;
import org.example.domain.model.Instrument;
import org.example.domain.model.Order;
import org.example.domain.model.Trade;
import org.example.domain.port.*;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private int        scale;
    private BaseOrderBook book;
    private IdGenerator idGen;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        scale  = instrument.getScale();
        book   = new PriceTimePriorityOrderBook(instrument, new IdGenerator(), r -> {});
        idGen  = new IdGenerator();
    }

    // ── Adding orders ─────────────────────────────────────────────────────────

    @Test
    void shouldAddBuyOrderToBids() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 10.0, scale));

        assertEquals(100.0,     book.getBestBid());
        assertTrue(Double.isNaN(book.getBestAsk()));
    }

    @Test
    void shouldAddSellOrderToAsks() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        assertEquals(100.0,     book.getBestAsk());
        assertTrue(Double.isNaN(book.getBestBid()));
    }

    @Test
    void shouldNotAddNullOrder() {
        assertThrows(NullPointerException.class, () -> book.addOrder(null));
    }

    // ── Order lookup ──────────────────────────────────────────────────────────

    @Test
    void shouldRetrieveRestingOrderById() {
        Order order = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(order);

        Order found = book.getOrder(order.getOrderId());
        assertNotNull(found);
        assertEquals(order.getOrderId(), found.getOrderId());
    }

    @Test
    void shouldReturnNullForUnknownOrderId() {
        assertNull(book.getOrder(9999L));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    void shouldCancelBothSidesAndZeroVolume() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 101.0, 10.0, scale);
        book.addOrder(buy);
        book.addOrder(sell);

        book.cancelOrder(buy.getOrderId());
        book.cancelOrder(sell.getOrderId());

        assertTrue(Double.isNaN(book.getBestBid()));
        assertTrue(Double.isNaN(book.getBestAsk()));
        assertEquals(0.0, book.getTotalVolume(),    1e-12);
        assertEquals(0.0, book.getTotalBidVolume(), 1e-12);
        assertEquals(0.0, book.getTotalAskVolume(), 1e-12);
        assertEquals(0, book.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(0, book.getOrderCountAtPrice(101.0, Side.SELL));
    }

    @Test
    void shouldThrowWhenCancellingUnknownOrder() {
        assertThrows(NoSuchElementException.class, () -> book.cancelOrder(999L));
    }

    @Test
    void shouldThrowWhenCancellingZeroId() {
        assertThrows(IllegalArgumentException.class, () -> book.cancelOrder(0L));
    }

    // ── Best bid / ask ────────────────────────────────────────────────────────

    @Test
    void shouldTrackBestBidAsHighestPrice() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.14, 10.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(),  90.0,  10.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(),  80.0,  10.0, scale));

        assertEquals(100.14, book.getBestBid(), 1e-9);
    }

    @Test
    void shouldTrackBestAskAsLowestPrice() {
        book.addOrder(Order.limitSell(idGen.next(), 100.14, 10.0, scale));
        book.addOrder(Order.limitSell(idGen.next(),  90.0,  10.0, scale));
        book.addOrder(Order.limitSell(idGen.next(),  80.23, 10.0, scale));

        assertEquals(80.23, book.getBestAsk(), 1e-9);
    }

    // ── Order count / volume at price ─────────────────────────────────────────

    @Test
    void shouldTrackOrderCountAtPrice() {
        book.addOrder(Order.limitBuy (idGen.next(), 100.0, 10.0, scale));
        book.addOrder(Order.limitBuy (idGen.next(), 100.0, 20.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 30.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 40.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 102.0, 50.0, scale));

        assertEquals(2, book.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(2, book.getOrderCountAtPrice(101.0, Side.SELL));
        assertEquals(1, book.getOrderCountAtPrice(102.0, Side.SELL));
    }

    @Test
    void shouldGetVolumeAtPriceLevel() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 10.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 20.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 15.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 5.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 8.0, scale));

        assertEquals(45.0, book.getVolumeAtPrice(100.0, Side.BUY),  1e-12);
        assertEquals(13.0, book.getVolumeAtPrice(101.0, Side.SELL), 1e-12);
        assertEquals(0.0,  book.getVolumeAtPrice(99.0,  Side.BUY),  1e-12);
    }

    // ── Total volume ──────────────────────────────────────────────────────────

    @Test
    void shouldGetTotalVolume() {
        book.addOrder(Order.limitBuy (idGen.next(), 100.0, 10.0, scale));
        book.addOrder(Order.limitBuy (idGen.next(), 100.0, 20.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 30.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 40.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 102.0, 50.0, scale));

        assertEquals(150.0, book.getTotalVolume(), 1e-12);
    }

    // ── Duplicate detection ───────────────────────────────────────────────────

    @Test
    void shouldNotAddDuplicateOrder() {
        Order order = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(order);

        assertThrows(IllegalStateException.class, () -> book.addOrder(order));

        assertEquals(1,    book.getOrderCountAtPrice(100.0, Side.BUY));
        assertEquals(10.0, book.getTotalBidVolume(), 1e-12);
        assertEquals(10.0, book.getTotalVolume(),    1e-12);
    }

    // ── Spread ────────────────────────────────────────────────────────────────

    @Test
    void shouldCalculateSpread() {
        book.addOrder(Order.limitBuy (idGen.next(),  99.0, 10.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 10.0, scale));

        assertEquals(2.0, book.getSpread(), 1e-9);
    }

    @Test
    void shouldReturnNaNSpreadWhenOneSideEmpty() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 10.0, scale));
        assertTrue(Double.isNaN(book.getSpread()));
    }

    @Test
    void shouldReturnNaNSpreadWhenBookEmpty() {
        assertTrue(Double.isNaN(book.getSpread()));
    }

    @Test
    void shouldUpdateSpreadAfterCancel() {
        Order bid1 = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order bid2 = Order.limitBuy (idGen.next(),  99.0, 10.0, scale);
        Order ask  = Order.limitSell(idGen.next(), 101.0, 10.0, scale);
        book.addOrder(bid1);
        book.addOrder(bid2);
        book.addOrder(ask);

        assertEquals(1.0, book.getSpread(), 1e-9);

        book.cancelOrder(bid1.getOrderId());

        assertEquals(2.0, book.getSpread(), 1e-9);
    }

    // ── Depth ─────────────────────────────────────────────────────────────────

    @Test
    void shouldGetBookDepth() {
        book.addOrder(Order.limitBuy(idGen.next(), 100.0, 10.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(), 100.0,  5.0, scale)); // same level
        book.addOrder(Order.limitBuy(idGen.next(),  99.0, 10.0, scale));
        book.addOrder(Order.limitBuy(idGen.next(),  98.0, 10.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 10.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 102.0, 10.0, scale));

        assertEquals(3, book.getDepth(Side.BUY));
        assertEquals(2, book.getDepth(Side.SELL));
    }

    @Test
    void shouldReturnZeroDepthWhenSideEmpty() {
        assertEquals(0, book.getDepth(Side.BUY));
        assertEquals(0, book.getDepth(Side.SELL));
    }

    @Test
    void shouldUpdateDepthAfterCancel() {
        Order o1 = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        Order o2 = Order.limitBuy(idGen.next(),  99.0, 10.0, scale);
        book.addOrder(o1);
        book.addOrder(o2);

        assertEquals(2, book.getDepth(Side.BUY));
        book.cancelOrder(o1.getOrderId());
        assertEquals(1, book.getDepth(Side.BUY));
    }

    // ── Trade count ───────────────────────────────────────────────────────────

    @Test
    void shouldTrackTotalTradeCount() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell1 = Order.limitSell(idGen.next(), 100.0,  5.0, scale);
        Order sell2 = Order.limitSell(idGen.next(), 100.0,  5.0, scale);
        book.addOrder(buy);
        book.addOrder(sell1);
        book.addOrder(sell2);

        assertEquals(2, book.getTradeCount());
    }

    @Test
    void shouldTrackZeroTradesWhenNoMatches() {
        book.addOrder(Order.limitBuy (idGen.next(),  99.0, 10.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 10.0, scale));

        assertEquals(0, book.getTradeCount());
    }

    // ── Instrument association ────────────────────────────────────────────────

    @Test
    void shouldAssociateWithInstrument() {
        Instrument inst = book.getInstrument();
        assertEquals("AAPL", inst.getSymbol());
        assertEquals(100,    inst.getScale());
        assertEquals(1,      inst.getLotSize());
    }

    // ── No trades when prices do not cross ───────────────────────────────────

    @Test
    void shouldReturnEmptyTradesWhenNoMatch() {
        List<Trade> t1 = book.addOrder(Order.limitBuy (idGen.next(),  99.0, 10.0, scale));
        List<Trade> t2 = book.addOrder(Order.limitSell(idGen.next(), 101.0, 10.0, scale));

        assertTrue(t1.isEmpty());
        assertTrue(t2.isEmpty());
    }
}