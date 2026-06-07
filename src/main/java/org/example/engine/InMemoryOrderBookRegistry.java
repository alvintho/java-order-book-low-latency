package org.example.engine;

import org.example.domain.model.Instrument;
import org.example.domain.model.Order;
import org.example.domain.model.Trade;
import org.example.domain.port.ExecutionReportPublisher;
import org.example.domain.port.BaseOrderBook;
import org.example.domain.port.OrderBookRegistry;
import org.example.util.IdGenerator;

import java.util.*;

/**
 * In-memory multi-symbol order book registry.
 *
 * <p>Responsibility: route orders/cancels/replaces to the correct
 * per-symbol {@link PriceTimePriorityOrderBook}. Does no matching itself.
 *
 * <p>Thread-safety: not thread-safe. All calls must arrive on the same thread,
 * or callers must synchronize externally (e.g. one thread per symbol is safe;
 * multiple symbols on multiple threads requires external locking on {@code books}).
 */
public final class InMemoryOrderBookRegistry implements OrderBookRegistry {

    private final Map<String, BaseOrderBook>  books      = new HashMap<>();
    private final IdGenerator             tradeIdGen = new IdGenerator();
    private final ExecutionReportPublisher publisher;

    /**
     * @param publisher receives ExecutionReports from all books;
     *                  use {@code report -> {}} to discard
     */
    public InMemoryOrderBookRegistry(ExecutionReportPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public void registerInstrument(Instrument instrument) {
        books.computeIfAbsent(
                instrument.getSymbol(),
                k -> new PriceTimePriorityOrderBook(instrument, tradeIdGen, publisher));
    }

    @Override
    public List<Trade> submitOrder(Order order) {
        return requireBook(order.getSymbol()).addOrder(order);
    }

    @Override
    public void cancelOrder(String symbol, long orderId) {
        requireBook(symbol).cancelOrder(orderId);
    }

    @Override
    public List<Trade> replaceOrder(String symbol, long origOrderId, Order newOrder) {
        return requireBook(symbol).replaceOrder(origOrderId, newOrder);
    }

    @Override
    public BaseOrderBook getBook(String symbol) {
        return books.get(symbol);
    }

    private BaseOrderBook requireBook(String symbol) {
        BaseOrderBook book = books.get(symbol);
        if (book == null)
            throw new IllegalStateException("No book registered for symbol: " + symbol);
        return book;
    }
}