package org.example.engine;

import org.example.model.*;
import org.example.util.IdGenerator;
import org.example.util.Price;

import java.util.*;

/**
 * Single-symbol order book with built-in price-time priority matching.
 *
 * Design change
 * Matching is now a private method of OrderBook rather than delegated to a
 * separate {@code MatchingStrategy}.  Rationale:
 *
 *   - The matching algorithm needs deep access to book data structures
 *       (TreeMap, ArrayDeque, orderMap).  Delegating requires exposing
 *       those internals via package-private getters — broken encapsulation
 *   - Price-time priority is the de facto standard.  It is not a "pluggable
 *       policy" that varies independently — it IS the book's core behavior.
 *   - If pro-rata or other allocation rules are needed later, extract a
 *       narrow {@code QuantityAllocator} interface for the allocation step
 *       only — not the entire matching loop.
 *
 *
 * Thread-safety
 * Not thread-safe — designed for single-threaded use per symbol.
 */
public class OrderBook {
    private final Instrument instrument;
    private final IdGenerator tradeIdGen;
    private double totalVolume;
    private double totalBidVolume;
    private double totalAskVolume;
    private int tradeCount;

    // Bids: highest price first (reverse natural order)
    private final TreeMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    // Asks: lowest price first (natural order)
    private final TreeMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
    private final Map<Long, Order> orderMap = new HashMap<>();

    public OrderBook(Instrument instrument, IdGenerator tradeIdGen) {
        this.instrument = Objects.requireNonNull(instrument, "Instrument cannot be null");
        this.tradeIdGen = Objects.requireNonNull(tradeIdGen, "tradeIdGen cannot be null");
    }

    // ── Public API ──────────────────────────────────────────────

    public Instrument getInstrument() { return instrument; }

    /**
     * Submit a new order.  Market orders match immediately and never rest.
     * Limit GTC orders rest if not fully filled.
     * IOC cancels any unfilled remainder after matching.
     * FOK rejects if the full quantity cannot be filled immediately.
     *
     * @return list of trades produced (never null, may be empty)
     */
    public List<Trade> addOrder(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");

        // Market orders: match immediately, never rest
        if (order.isMarket()) {
            List<Trade> trades = matchMarket(order);
            if (!order.isFilled()) {
                order.cancel();
            }
            return trades;
        }

        // Duplicate check
        if (orderMap.containsKey(order.getOrderId())) {
            throw new IllegalStateException("Order " + order.getOrderId() + " already exists");
        }

        TimeInForce tif = order.getTimeInForce() != null ? order.getTimeInForce() : TimeInForce.GTC;

        // FOK: check if full fill is possible before inserting
        if (tif == TimeInForce.FOK && !canFillCompletely(order)) {
            order.cancel();
            return Collections.emptyList();
        }

        // Insert into book
        insertOrder(order);

        // Match
        List<Trade> trades = matchLimit(order);

        // IOC: remove unfilled remainder from book (cancel deferred to caller)
        if (tif == TimeInForce.IOC && !order.isFilled()) {
            removeOrderFromBook(order);
            removeVolume(order.getQuantity(), order.getSide());
        }

        return trades;
    }

    public void cancelOrder(long orderId) {
        if (orderId < 1) throw new IllegalArgumentException("Order ID must be positive");
        Order order = orderMap.get(orderId);
        if (order == null) throw new NoSuchElementException("Order " + orderId + " not found");
        removeOrderFromBook(order);
        removeVolume(order.getQuantity(), order.getSide());
        order.cancel();
    }

    /**
     * Cancel/replace.  Cancels old order (priority reset per business rules)
     * and submits the replacement.
     */
    public List<Trade> replaceOrder(long origOrderId, Order newOrder) {
        if (origOrderId < 1) throw new IllegalArgumentException("Original order ID must be positive");
        Order origOrder = orderMap.get(origOrderId);
        if (origOrder == null) throw new NoSuchElementException("Order " + origOrderId + " not found");
        removeOrderFromBook(origOrder);
        removeVolume(origOrder.getQuantity(), origOrder.getSide());
        origOrder.cancel();
        return addOrder(newOrder);
    }

    // ── Matching (private — the core algorithm) ─────────────────

    /**
     * Match a limit order against the opposite side of the book.
     * Iterates price levels from best to worst, filling at each level
     * via FIFO (queue head first).
     */
    private List<Trade> matchLimit(Order incoming) {
        TreeMap<Long, ArrayDeque<Order>> oppositeBook = incoming.isBid() ? asks : bids;
        List<Trade> trades = new ArrayList<>();

        while (!oppositeBook.isEmpty() && !incoming.isFilled()) {
            long bestPrice = oppositeBook.firstKey();
            if (!priceMatches(incoming, bestPrice)) break;
            fillAtPriceLevel(incoming, oppositeBook, bestPrice, trades);
        }

        // Fully filled incoming limit order: remove from book
        if (incoming.isFilled()) {
            removeOrderFromBook(incoming);
        }
        return trades;
    }

    private List<Trade> matchMarket(Order incoming) {
        TreeMap<Long, ArrayDeque<Order>> oppositeBook = incoming.isBid() ? asks : bids;
        List<Trade> trades = new ArrayList<>();

        while (!oppositeBook.isEmpty() && !incoming.isFilled()) {
            long bestPrice = oppositeBook.firstKey();
            fillAtPriceLevel(incoming, oppositeBook, bestPrice, trades);
        }
        return trades;
    }

    private void fillAtPriceLevel(Order incoming,
                                  TreeMap<Long, ArrayDeque<Order>> oppositeBook,
                                  long priceLevel, List<Trade> trades) {
        ArrayDeque<Order> queue = oppositeBook.get(priceLevel);
        if (queue == null || queue.isEmpty()) {
            oppositeBook.remove(priceLevel);
            return;
        }

        while (!queue.isEmpty() && !incoming.isFilled()) {
            Order resting = queue.peek();
            double tradeQty = Math.min(incoming.getQuantity(), resting.getQuantity());

            // applyFill computes volume-weighted avgPx
            incoming.applyFill(tradeQty, priceLevel);
            resting.applyFill(tradeQty, priceLevel);

            // Volume accounting
            totalVolume -= tradeQty;
            if (resting.getSide() == Side.SELL) totalAskVolume -= tradeQty;
            else                                 totalBidVolume -= tradeQty;
            if (!incoming.isMarket()) {
                totalVolume -= tradeQty;
                if (incoming.getSide() == Side.SELL) totalAskVolume -= tradeQty;
                else                                  totalBidVolume -= tradeQty;
            }

            // Remove fully-filled resting order
            if (resting.isFilled()) {
                queue.poll();  // O(1) — head removal
                orderMap.remove(resting.getOrderId());
            }

            long buyId  = incoming.isBid() ? incoming.getOrderId() : resting.getOrderId();
            long sellId = incoming.isBid() ? resting.getOrderId() : incoming.getOrderId();
            trades.add(new Trade(tradeIdGen.next(), priceLevel, tradeQty, buyId, sellId));
            tradeCount++;
        }

        if (queue.isEmpty()) {
            oppositeBook.remove(priceLevel);
        }
    }

    private static boolean priceMatches(Order incoming, long restingPrice) {
        return incoming.isBid()
                ? incoming.getPrice() >= restingPrice
                : incoming.getPrice() <= restingPrice;
    }

    // ── Book management (private) ───────────────────────────────

    private void insertOrder(Order order) {
        TreeMap<Long, ArrayDeque<Order>> book = order.isBid() ? bids : asks;
        book.computeIfAbsent(order.getPrice(), k -> new ArrayDeque<>()).add(order);
        orderMap.put(order.getOrderId(), order);
        addVolume(order.getQuantity(), order.getSide());
    }

    private void addVolume(double quantity, Side side) {
        totalVolume += quantity;
        if (side == Side.BUY) totalBidVolume += quantity;
        else                  totalAskVolume += quantity;
    }

    private void removeVolume(double quantity, Side side) {
        totalVolume -= quantity;
        if (side == Side.BUY) totalBidVolume -= quantity;
        else                  totalAskVolume -= quantity;
    }

    private void removeOrderFromBook(Order order) {
        TreeMap<Long, ArrayDeque<Order>> book = order.isBid() ? bids : asks;
        ArrayDeque<Order> queue = book.get(order.getPrice());
        if (queue != null) {
            queue.remove(order);  // O(n) — acceptable for cancel (infrequent)
            if (queue.isEmpty()) book.remove(order.getPrice());
        }
        orderMap.remove(order.getOrderId());
    }

    /**
     * Dry-run: can {@code order} be fully filled against current book?
     * Used for FOK pre-flight check.  Does not modify state.
     */
    private boolean canFillCompletely(Order order) {
        TreeMap<Long, ArrayDeque<Order>> oppositeBook = order.isBid() ? asks : bids;
        double remaining = order.getQuantity();

        for (Map.Entry<Long, ArrayDeque<Order>> entry : oppositeBook.entrySet()) {
            if (!order.isMarket() && !priceMatches(order, entry.getKey())) break;
            for (Order resting : entry.getValue()) {
                remaining -= resting.getQuantity();
                if (remaining <= 1e-12) return true;
            }
        }
        return remaining <= 1e-12;
    }

    // ── Query methods ───────────────────────────────────────────

    public Order getOrder(long orderId) { return orderMap.get(orderId); }
    public double getTotalVolume()    { return totalVolume; }
    public double getTotalBidVolume() { return totalBidVolume; }
    public double getTotalAskVolume() { return totalAskVolume; }

    public double getBestBid() {
        return bids.isEmpty() ? Double.NaN
                : Price.toDouble(bids.firstKey(), instrument.getScale());
    }
    public double getBestAsk() {
        return asks.isEmpty() ? Double.NaN
                : Price.toDouble(asks.firstKey(), instrument.getScale());
    }
    public double getSpread() {
        if (bids.isEmpty() || asks.isEmpty()) return Double.NaN;
        return Price.toDouble(asks.firstKey() - bids.firstKey(), instrument.getScale());
    }
    public int getDepth(Side side) { return (side == Side.BUY ? bids : asks).size(); }
    public int getTradeCount()     { return tradeCount; }

    public int getOrderCountAtPrice(double price, Side side) {
        ArrayDeque<Order> q = getOrdersAtPrice(price, side);
        return q == null ? 0 : q.size();
    }
    public double getVolumeAtPrice(double price, Side side) {
        ArrayDeque<Order> q = getOrdersAtPrice(price, side);
        if (q == null) return 0;
        double vol = 0;
        for (Order o : q) vol += o.getQuantity();
        return vol;
    }

    private ArrayDeque<Order> getOrdersAtPrice(double price, Side side) {
        TreeMap<Long, ArrayDeque<Order>> book = side == Side.BUY ? bids : asks;
        return book.get(Price.toLong(price, instrument.getScale()));
    }
}
