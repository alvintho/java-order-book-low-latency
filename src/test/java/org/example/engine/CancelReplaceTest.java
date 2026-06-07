package org.example.engine;

import org.example.domain.enums.OrdStatus;
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

class CancelReplaceTest {

    private BaseOrderBook  book;
    private IdGenerator idGen;
    private int        scale;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        scale = instrument.getScale();
        book  = new PriceTimePriorityOrderBook(instrument, new IdGenerator(), r -> {});
        idGen = new IdGenerator();
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    void shouldCancelRestingOrder() {
        Order buy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(buy);

        book.cancelOrder(buy.getOrderId());

        assertNull(book.getOrder(buy.getOrderId()));
        assertEquals(OrdStatus.CANCELED, buy.getOrdStatus());
        assertEquals(0.0, book.getTotalBidVolume(), 1e-12);
        assertEquals(0.0, book.getTotalVolume(),    1e-12);
    }

    @Test
    void shouldThrowWhenCancellingNonExistentOrder() {
        assertThrows(NoSuchElementException.class, () -> book.cancelOrder(999L));
    }

    @Test
    void shouldCancelPartiallyFilledOrder() {
        Order buy  = Order.limitBuy (idGen.next(), 100.0, 10.0, scale);
        Order sell = Order.limitSell(idGen.next(), 100.0,  3.0, scale);
        book.addOrder(buy);
        book.addOrder(sell);

        assertEquals(7.0, buy.getLeavesQty(), 1e-12);

        book.cancelOrder(buy.getOrderId());

        assertNull(book.getOrder(buy.getOrderId()));
        assertEquals(OrdStatus.CANCELED, buy.getOrdStatus());
        assertEquals(0.0, book.getTotalBidVolume(), 1e-12);
    }

    // ── Replace ───────────────────────────────────────────────────────────────

    @Test
    void replaceShouldCancelOldAndInsertNew() {
        Order orig = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(orig);

        Order replacement = Order.limitBuy(idGen.next(), 101.0, 15.0, scale);
        book.replaceOrder(orig.getOrderId(), replacement);

        assertNull(book.getOrder(orig.getOrderId()),
                "Original order must be removed");
        assertEquals(OrdStatus.CANCELED, orig.getOrdStatus());
        assertNotNull(book.getOrder(replacement.getOrderId()));
        assertEquals(101.0, book.getBestBid(), 1e-9);
        assertEquals(15.0,  book.getTotalBidVolume(), 1e-12);
    }

    @Test
    void replaceShouldResetQueuePriority() {
        Order first  = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        Order second = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        book.addOrder(first);
        book.addOrder(second);

        // Replace first — loses priority, goes behind second
        Order replacement = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        book.replaceOrder(first.getOrderId(), replacement);

        // Incoming sell should fill second (queue head), not replacement
        Order sell = Order.limitSell(idGen.next(), 100.0, 5.0, scale);
        List<Trade> trades = book.addOrder(sell);

        assertEquals(1, trades.size());
        assertEquals(second.getOrderId(), trades.getFirst().getBuyOrderId(),
                "second should have priority over the replacement");
    }

    @Test
    void replaceShouldTriggerMatchIfNewPriceCrosses() {
        Order sell = Order.limitSell(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(sell);

        Order origBuy = Order.limitBuy(idGen.next(), 99.0, 10.0, scale); // no cross
        book.addOrder(origBuy);

        // Replace with price that crosses the resting sell
        Order newBuy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        List<Trade> trades = book.replaceOrder(origBuy.getOrderId(), newBuy);

        assertEquals(1, trades.size());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void shouldThrowWhenReplacingNonExistentOrder() {
        Order replacement = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        assertThrows(NoSuchElementException.class,
                () -> book.replaceOrder(999L, replacement));
    }

    @Test
    void replaceWithNewPriceShouldCleanUpOldPriceLevel() {
        Order sell = Order.limitSell(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(sell);

        Order newSell = Order.limitSell(idGen.next(), 99.0, 10.0, scale);
        book.replaceOrder(sell.getOrderId(), newSell);

        assertEquals(0, book.getOrderCountAtPrice(100.0, Side.SELL),
                "Old price level must be empty");
        assertEquals(1, book.getOrderCountAtPrice(99.0, Side.SELL),
                "Replacement must appear at new price level");
    }
}