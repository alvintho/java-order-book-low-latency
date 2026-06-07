package org.example.engine;

import org.example.model.*;
import org.example.util.IdGenerator;

import java.util.*;

/**
 * Multi-symbol matching engine.
 *
 * Responsibilities
 *
 *   - Routes orders to the correct per-symbol {@link OrderBook}.
 *   - Owns a shared trade-ID generator so IDs are globally unique.
 *   - Handles IOC order cancellation (book cleanup happens in OrderBook;
 *       the {@code ordStatus} transition to CANCELED happens here).
 *
 *
 * Design
 * Matching is built into {@link OrderBook} — no separate strategy.
 * This class is a thin multi-symbol router.
 */
public class MatchingEngine {

    private final Map<String, OrderBook> books = new HashMap<>();
    private final IdGenerator tradeIdGen = new IdGenerator();

    public void registerInstrument(Instrument instrument) {
        books.computeIfAbsent(instrument.getSymbol(),
                k -> new OrderBook(instrument, tradeIdGen));
    }

    public OrderBook getBook(String symbol) {
        return books.get(symbol);
    }

    /**
     * Submit a new order to the matching engine.
     *
     * @return list of trades produced (never null, may be empty)
     */
    public List<Trade> submitOrder(Order order) {
        OrderBook book = requireBook(order.getSymbol());
        List<Trade> trades = book.addOrder(order);

        // IOC: OrderBook cleaned up the book.  We transition ordStatus here.
        if (order.getTimeInForce() == TimeInForce.IOC && !order.isFilled()) {
            order.cancel();
        }

        return trades;
    }

    public void cancelOrder(String symbol, long orderId) {
        OrderBook book = requireBook(symbol);
        book.cancelOrder(orderId);
    }

    /**
     * Cancel/replace.  Cancels the original order and submits the replacement.
     * Priority is reset per business rules.
     */
    public List<Trade> replaceOrder(String symbol, long origOrderId, Order newOrder) {
        OrderBook book = requireBook(symbol);
        List<Trade> trades = book.replaceOrder(origOrderId, newOrder);

        if (newOrder.getTimeInForce() == TimeInForce.IOC && !newOrder.isFilled()) {
            newOrder.cancel();
        }

        return trades;
    }

    private OrderBook requireBook(String symbol) {
        OrderBook book = books.get(symbol);
        if (book == null) {
            throw new IllegalStateException("No book registered for symbol: " + symbol);
        }
        return book;
    }
}