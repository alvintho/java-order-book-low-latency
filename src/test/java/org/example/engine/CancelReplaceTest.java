package org.example.engine;

import org.example.model.*;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CancelReplaceTest {
    private OrderBook orderBook;
    private IdGenerator idGen;
    private int scale;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        scale = instrument.getScale();
        orderBook = new OrderBook(instrument);
        idGen = new IdGenerator();
    }

    // ── Cancel tests ──

    @Test
    void shouldCancelRestingOrder() {
        Order buy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        orderBook.addOrder(buy);

        orderBook.cancelOrder(buy.getOrderId());

        assertNull(orderBook.getOrder(buy.getOrderId()));
        assertEquals(0.0, orderBook.getTotalBidVolume());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldThrowWhenCancellingNonExistentOrder() {
        assertThrows(IllegalStateException.class, () -> orderBook.cancelOrder(999));
    }

    @Test
    void shouldCancelPartiallyFilledOrder() {
        Order buy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        orderBook.addOrder(buy);

        // Partial fill
        Order sell = Order.limitSell(idGen.next(), 100.0, 3.0, scale);
        orderBook.addOrder(sell);

        // Cancel remainder
        orderBook.cancelOrder(buy.getOrderId());

        assertNull(orderBook.getOrder(buy.getOrderId()));
        assertEquals(0.0, orderBook.getTotalBidVolume());
    }

    // ── Replace tests ──

    @Test
    void replaceShouldCancelOldAndInsertNew() {
        Order origBuy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        orderBook.addOrder(origBuy);

        Order newBuy = Order.limitBuy(idGen.next(), 101.0, 15.0, scale);
        orderBook.replaceOrder(origBuy.getOrderId(), newBuy);

        assertNull(orderBook.getOrder(origBuy.getOrderId()));
        assertNotNull(orderBook.getOrder(newBuy.getOrderId()));
        assertEquals(101.0, orderBook.getBestBid());
        assertEquals(15.0, orderBook.getTotalBidVolume());
    }

    @Test
    void replaceShouldResetPriority() {
        // First order at 100
        Order first = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        orderBook.addOrder(first);

        // Second order at 100
        Order second = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        orderBook.addOrder(second);

        // Replace first with same price — should lose priority (go behind second)
        Order replacement = Order.limitBuy(idGen.next(), 100.0, 5.0, scale);
        orderBook.replaceOrder(first.getOrderId(), replacement);

        // Sell should match second (now first in queue), not replacement
        Order sell = Order.limitSell(idGen.next(), 100.0, 5.0, scale);
        List<Trade> trades = orderBook.addOrder(sell);

        assertEquals(1, trades.size());
        assertEquals(second.getOrderId(), trades.getFirst().getBuyOrderId());
    }

    @Test
    void replaceShouldTriggerMatchIfNewPriceCrosses() {
        // Resting sell at 100
        orderBook.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        // Resting buy at 99 (no match)
        Order origBuy = Order.limitBuy(idGen.next(), 99.0, 10.0, scale);
        orderBook.addOrder(origBuy);

        // Replace buy with price 100 — should now match
        Order newBuy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        List<Trade> trades = orderBook.replaceOrder(origBuy.getOrderId(), newBuy);

        assertEquals(1, trades.size());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    @Test
    void shouldThrowWhenReplacingNonExistentOrder() {
        Order newOrder = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        assertThrows(IllegalStateException.class,
                () -> orderBook.replaceOrder(999, newOrder));
    }
}
