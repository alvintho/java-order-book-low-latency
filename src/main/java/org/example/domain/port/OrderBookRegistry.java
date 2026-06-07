package org.example.domain.port;

import org.example.domain.model.Instrument;
import org.example.domain.model.Order;
import org.example.domain.model.Trade;

import java.util.List;

/**
 * Multi-symbol order book registry.
 *
 * Responsibility: route orders to the correct per-symbol book
 * and manage book lifecycle.
 *
 * Replaces the previous {@code MatchingEngine} class, whose name
 * implied it did matching (it didn't — books do). The name now
 * reflects the actual responsibility.
 */
public interface OrderBookRegistry {

    /** Register a new instrument. Idempotent if already registered. */
    void registerInstrument(Instrument instrument);

    /** Submit a new order. Returns all trades produced. */
    List<Trade> submitOrder(Order order);

    /** Cancel an existing resting order. */
    void cancelOrder(String symbol, long orderId);

    /**
     * Cancel-replace: cancel original order and submit replacement.
     * Returns trades produced by the replacement order.
     */
    List<Trade> replaceOrder(String symbol, long origOrderId, Order newOrder);

    /** Look up the book for a symbol, or null if not registered. */
    BaseOrderBook getBook(String symbol);
}