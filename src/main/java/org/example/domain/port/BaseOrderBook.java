package org.example.domain.port;

import org.example.domain.model.Instrument;
import org.example.domain.model.Order;
import org.example.domain.enums.Side;
import org.example.domain.model.Trade;
import org.example.engine.PriceTimePriorityOrderBook;

import java.util.List;

/**
 * Single-symbol order book: manages price levels and exposes
 * the matching API.
 *
 * The matching algorithm is intentionally kept inside the concrete
 * implementation ({@link PriceTimePriorityOrderBook}) because it
 * requires direct access to the book's internal data structures.
 * Exposing an interface here allows alternative implementations
 * (e.g. pro-rata) to be swapped in without changing callers.
 */
public interface BaseOrderBook {

    Instrument getInstrument();

    /**
     * Submit a new order. Handles LIMIT, MARKET, GTC, IOC, FOK.
     * @return trades produced (never null, may be empty)
     */
    List<Trade> addOrder(Order order);

    /** Cancel a resting order by internal ID. */
    void cancelOrder(long orderId);

    /**
     * Cancel the original order and submit the replacement.
     * @return trades produced by the replacement
     */
    List<Trade> replaceOrder(long origOrderId, Order newOrder);

    Order  getOrder(long orderId);
    double getBestBid();
    double getBestAsk();
    double getSpread();
    int    getDepth(Side side);
    int    getTradeCount();
    double getTotalVolume();
    double getTotalBidVolume();
    double getTotalAskVolume();
    int    getOrderCountAtPrice(double price, Side side);
    double getVolumeAtPrice(double price, Side side);
}