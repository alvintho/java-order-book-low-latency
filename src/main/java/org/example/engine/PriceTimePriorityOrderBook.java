package org.example.engine;

import org.example.domain.enums.OrdStatus;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.domain.model.*;
import org.example.domain.port.BaseOrderBook;
import org.example.domain.port.ExecutionReportPublisher;
import org.example.domain.model.*;
import org.example.util.IdGenerator;
import org.example.util.Price;

import java.util.*;

/**
 * Price-time priority (FIFO) order book for a single symbol.
 *
 * <p><b>Matching rules:</b>
 * <ul>
 *   <li>Bids: highest price first; ties broken by arrival time (FIFO queue).</li>
 *   <li>Asks: lowest price first; ties broken by arrival time.</li>
 *   <li>MARKET orders match immediately at any price; cancel remainder if unfillable.</li>
 *   <li>GTC limits rest in the book until filled or canceled.</li>
 *   <li>IOC limits match immediately; unfilled remainder is canceled (never rests).</li>
 *   <li>FOK limits are rejected entirely if the full quantity cannot be filled now.</li>
 * </ul>
 *
 * <p><b>Volume accounting:</b> only resting book volume is tracked.
 * Incoming orders contribute volume only while they rest in the book.
 *
 * <p><b>Thread-safety:</b> not thread-safe. Designed for single-threaded
 * use per symbol. Callers requiring concurrency must synchronize externally.
 *
 * <p><b>Price representation:</b> all prices are scaled integers (long).
 * Use {@link Price} utilities to convert to/from double display prices.
 */
public final class PriceTimePriorityOrderBook implements BaseOrderBook {

    private static final double EPSILON = 1e-12;

    private final Instrument instrument;
    private final IdGenerator             tradeIdGen;
    private final ExecutionReportPublisher publisher;

    private final TreeMap<Long, ArrayDeque<Order>> bids =
            new TreeMap<>(Collections.reverseOrder());     // Bids: highest price first
    private final TreeMap<Long, ArrayDeque<Order>> asks =
            new TreeMap<>();                               // Asks: lowest price first

    private final Map<Long, Order> orderMap = new HashMap<>(); // All resting orders by internal ID

    // Volume trackers (resting book only)
    private double totalVolume    = 0.0;
    private double totalBidVolume = 0.0;
    private double totalAskVolume = 0.0;
    private int    tradeCount     = 0;

    /**
     * @param instrument the instrument this book manages
     * @param tradeIdGen shared trade-ID generator (globally unique IDs)
     * @param publisher  receives an ExecutionReport for every state change;
     *                   pass {@code report -> {}} to discard reports
     */
    public PriceTimePriorityOrderBook(Instrument instrument,
                                      IdGenerator tradeIdGen,
                                      ExecutionReportPublisher publisher) {
        this.instrument = Objects.requireNonNull(instrument, "instrument");
        this.tradeIdGen = Objects.requireNonNull(tradeIdGen,  "tradeIdGen");
        this.publisher  = Objects.requireNonNull(publisher,   "publisher");
    }

    // ── OrderBook API ─────────────────────────────────────────────────────────

    @Override
    public Instrument getInstrument() { return instrument; }

    /**
     * Submit a new order.
     *
     * <p>Decision flow:
     * <ol>
     *   <li>MARKET → match immediately; cancel unfilled remainder.</li>
     *   <li>LIMIT/FOK → pre-flight fill check; reject if insufficient liquidity.</li>
     *   <li>LIMIT/GTC or IOC → insert into book, match, remove IOC remainder.</li>
     * </ol>
     */
    @Override
    public List<Trade> addOrder(Order order) {
        Objects.requireNonNull(order, "order");

        if (order.isMarket()) {
            return handleMarketOrder(order);
        }
        return handleLimitOrder(order);
    }

    @Override
    public void cancelOrder(long orderId) {
        if (orderId < 1) throw new IllegalArgumentException("orderId must be positive");
        Order order = orderMap.get(orderId);
        if (order == null) throw new NoSuchElementException("Order not found: " + orderId);

        removeFromBook(order);
        order.cancel();
        publisher.publish(ExecutionReport.canceled(order));
    }

    /**
     * Cancel the original order and submit the replacement.
     * Priority is reset — the replacement enters the queue as a new order.
     */
    @Override
    public List<Trade> replaceOrder(long origOrderId, Order newOrder) {
        if (origOrderId < 1) throw new IllegalArgumentException("origOrderId must be positive");
        Order orig = orderMap.get(origOrderId);
        if (orig == null) throw new NoSuchElementException("Order not found: " + origOrderId);

        removeFromBook(orig);
        orig.cancel();
        publisher.publish(ExecutionReport.canceled(orig));

        List<Trade> trades = addOrder(newOrder);
        if (!trades.isEmpty()) {
            publisher.publish(ExecutionReport.replaced(newOrder));
        }
        return trades;
    }

    // ── Market order handling ─────────────────────────────────────────────────

    private List<Trade> handleMarketOrder(Order order) {
        // Market orders never rest in the book
        List<Trade> trades = match(order, oppositeBook(order));
        if (!order.isFilled()) {
            // No liquidity — cancel remainder
            order.cancel();
            publisher.publish(ExecutionReport.canceled(order));
        }
        return trades;
    }

    // ── Limit order handling ──────────────────────────────────────────────────

    private List<Trade> handleLimitOrder(Order order) {
        if (orderMap.containsKey(order.getOrderId())) {
            throw new IllegalStateException("Duplicate order ID: " + order.getOrderId());
        }

        TimeInForce tif = order.getTimeInForce() != null
                ? order.getTimeInForce() : TimeInForce.GTC;

        // FOK: all-or-nothing pre-flight check
        if (tif == TimeInForce.FOK && !canFillCompletely(order)) {
            order.cancel();
            publisher.publish(ExecutionReport.rejected(order));
            return Collections.emptyList();
        }

        // Insert into book first (adds to volume trackers)
        insertIntoBook(order);
        publisher.publish(ExecutionReport.ack(order));

        // Match against opposite side
        List<Trade> trades = match(order, oppositeBook(order));

        // IOC: cancel any unfilled remainder and remove from book
        if (tif == TimeInForce.IOC && !order.isFilled()) {
            removeFromBook(order);
            order.cancel();
            publisher.publish(ExecutionReport.canceled(order));
        }

        // Fully filled: already removed from book inside match()
        return trades;
    }

    // ── Core matching loop ────────────────────────────────────────────────────

    /**
     * Match {@code incoming} against {@code opposite} book side.
     * Iterates price levels best→worst; within each level, fills FIFO.
     *
     * @return list of trades produced
     */
    private List<Trade> match(Order incoming,
                              TreeMap<Long, ArrayDeque<Order>> opposite) {
        List<Trade> trades = new ArrayList<>();

        while (!opposite.isEmpty() && !incoming.isFilled()) {
            long bestPrice = opposite.firstKey();

            // Limit orders: stop if no price overlap
            if (!incoming.isMarket() && !priceMatches(incoming, bestPrice)) break;

            fillAtLevel(incoming, opposite, bestPrice, trades);
        }

        // Remove fully-filled incoming limit order from the book
        if (!incoming.isMarket() && incoming.isFilled()) {
            removeFromBook(incoming);
        }

        return trades;
    }

    /**
     * Fill as much of {@code incoming} as possible from the queue at
     * {@code priceLevel}. Removes fully-filled resting orders.
     */
    private void fillAtLevel(Order incoming,
                             TreeMap<Long, ArrayDeque<Order>> opposite,
                             long priceLevel,
                             List<Trade> trades) {
        ArrayDeque<Order> queue = opposite.get(priceLevel);
        if (queue == null || queue.isEmpty()) {
            opposite.remove(priceLevel);
            return;
        }

        while (!queue.isEmpty() && !incoming.isFilled()) {
            Order resting = queue.peek();
            double fillQty = Math.min(incoming.getLeavesQty(), resting.getLeavesQty());

            // Apply fill to both sides — single mutation path
            incoming.applyFill(fillQty, priceLevel);
            resting.applyFill(fillQty, priceLevel);

            // ── Volume accounting (resting book only) ──────────────────────
            // Bug fix: previously double-subtracted for limit incoming orders.
            // We only track resting book volume; incoming orders are not in
            // the book until insertIntoBook() is called — and market orders
            // are never inserted at all.
            // For limit incoming: it WAS inserted, so subtract its fill share.
            // For market incoming: it was never in the book, so don't touch.
            subtractVolume(fillQty, resting.getSide());
            if (!incoming.isMarket()) {
                subtractVolume(fillQty, incoming.getSide());
            }

            // Publish fill reports for both parties
            publisher.publish(ExecutionReport.fill(resting,  fillQty, priceLevel));
            publisher.publish(ExecutionReport.fill(incoming, fillQty, priceLevel));

            // Remove fully-filled resting order
            if (resting.isFilled()) {
                queue.poll();
                orderMap.remove(resting.getOrderId());
            }

            long buyId  = incoming.isBid() ? incoming.getOrderId() : resting.getOrderId();
            long sellId = incoming.isBid() ? resting.getOrderId()  : incoming.getOrderId();
            trades.add(new Trade(tradeIdGen.next(), priceLevel, fillQty, buyId, sellId));
            tradeCount++;
        }

        if (queue.isEmpty()) {
            opposite.remove(priceLevel);
        }
    }

    // ── Price matching predicate ──────────────────────────────────────────────

    /**
     * Returns true when the incoming limit price overlaps with the resting price.
     * Buy: incoming bid >= resting ask (willing to pay at least as much as asked).
     * Sell: incoming ask <= resting bid (willing to accept at most as much as bid).
     */
    private static boolean priceMatches(Order incoming, long restingPrice) {
        return incoming.isBid()
                ? incoming.getPrice() >= restingPrice
                : incoming.getPrice() <= restingPrice;
    }

    // ── FOK pre-flight ────────────────────────────────────────────────────────

    /**
     * Dry-run: can {@code order} be fully filled at current book depth?
     * Does NOT modify state.
     */
    private boolean canFillCompletely(Order order) {
        TreeMap<Long, ArrayDeque<Order>> opposite = oppositeBook(order);
        double remaining = order.getLeavesQty();

        for (Map.Entry<Long, ArrayDeque<Order>> entry : opposite.entrySet()) {
            if (!priceMatches(order, entry.getKey())) break;
            for (Order resting : entry.getValue()) {
                remaining -= resting.getLeavesQty();
                if (remaining <= EPSILON) return true;
            }
        }
        return remaining <= EPSILON;
    }

    // ── Book management ───────────────────────────────────────────────────────

    private void insertIntoBook(Order order) {
        sideBook(order).computeIfAbsent(order.getPrice(), k -> new ArrayDeque<>()).add(order);
        orderMap.put(order.getOrderId(), order);
        addVolume(order.getLeavesQty(), order.getSide());
    }

    /**
     * Remove an order from its price-level queue and the orderMap.
     * Also corrects the volume trackers for whatever quantity remains.
     *
     * <p>Bug fix: previously called with {@code order.getQuantity()} (leavesQty)
     * but volume was added with the original orderQty. Now we remove only
     * leavesQty because partially-filled orders already had their filled
     * portion subtracted during matching.
     */
    private void removeFromBook(Order order) {
        TreeMap<Long, ArrayDeque<Order>> book = sideBook(order);
        ArrayDeque<Order> queue = book.get(order.getPrice());
        if (queue != null) {
            queue.remove(order);          // O(n) — acceptable for cancel
            if (queue.isEmpty()) book.remove(order.getPrice());
        }
        if (orderMap.remove(order.getOrderId()) != null) {
            // Only adjust volume if the order was actually in the map
            subtractVolume(order.getLeavesQty(), order.getSide());
        }
    }

    private void addVolume(double qty, Side side) {
        totalVolume += qty;
        if (side == Side.BUY) totalBidVolume += qty;
        else                  totalAskVolume += qty;
    }

    private void subtractVolume(double qty, Side side) {
        totalVolume -= qty;
        if (side == Side.BUY) totalBidVolume -= qty;
        else                  totalAskVolume -= qty;
    }

    private TreeMap<Long, ArrayDeque<Order>> sideBook(Order order) {
        return order.isBid() ? bids : asks;
    }

    private TreeMap<Long, ArrayDeque<Order>> oppositeBook(Order order) {
        return order.isBid() ? asks : bids;
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    @Override public Order  getOrder(long orderId) { return orderMap.get(orderId); }
    @Override public double getTotalVolume()        { return totalVolume;    }
    @Override public double getTotalBidVolume()     { return totalBidVolume; }
    @Override public double getTotalAskVolume()     { return totalAskVolume; }
    @Override public int    getTradeCount()         { return tradeCount;     }

    @Override
    public double getBestBid() {
        return bids.isEmpty() ? Double.NaN
                : Price.toDouble(bids.firstKey(), instrument.getScale());
    }

    @Override
    public double getBestAsk() {
        return asks.isEmpty() ? Double.NaN
                : Price.toDouble(asks.firstKey(), instrument.getScale());
    }

    @Override
    public double getSpread() {
        if (bids.isEmpty() || asks.isEmpty()) return Double.NaN;
        return Price.toDouble(asks.firstKey() - bids.firstKey(), instrument.getScale());
    }

    @Override
    public int getDepth(Side side) {
        return (side == Side.BUY ? bids : asks).size();
    }

    @Override
    public int getOrderCountAtPrice(double price, Side side) {
        ArrayDeque<Order> q = queueAtPrice(price, side);
        return q == null ? 0 : q.size();
    }

    @Override
    public double getVolumeAtPrice(double price, Side side) {
        ArrayDeque<Order> q = queueAtPrice(price, side);
        if (q == null) return 0.0;
        double vol = 0.0;
        for (Order o : q) vol += o.getLeavesQty();
        return vol;
    }

    private ArrayDeque<Order> queueAtPrice(double price, Side side) {
        TreeMap<Long, ArrayDeque<Order>> book = side == Side.BUY ? bids : asks;
        return book.get(Price.toLong(price, instrument.getScale()));
    }
}