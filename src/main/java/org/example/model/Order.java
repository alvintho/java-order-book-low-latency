package org.example.model;

import org.example.util.Price;

import java.time.Instant;

/**
 * Domain order aligned to FIX 4.4 NewOrderSingle (D) fields.
 * Mutable only via controlled methods (applyFill, cancel).
 *
 * Thread-safety: not thread-safe — single-threaded matching assumed.
 */
public class Order {
    private final String clOrdId;       // FIX Tag 11
    private final long orderId;         // internal unique id
    private final String symbol;        // FIX Tag 55
    private final long price;           // scaled integer price
    private double orderQty;            // FIX Tag 38 — original quantity
    private double leavesQty;           // FIX Tag 151 — remaining
    private double cumQty;              // FIX Tag 14 — total filled
    private double avgPx;               // FIX Tag 6
    private final Instant createdAt;     // FIX 6: wall-clock timestamp (was System.nanoTime())
    private final Side side;            // FIX Tag 54
    private final OrderType orderType;  // FIX Tag 40
    private final TimeInForce tif;      // FIX Tag 59
    private OrdStatus ordStatus;        // FIX Tag 39

    private Order(Builder b) {
        if (b.orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive: " + b.orderId);
        }
        if (b.orderQty <= 0.0) {
            throw new IllegalArgumentException("Quantity must be positive: " + b.orderQty);
        }
        this.clOrdId = b.clOrdId;
        this.orderId = b.orderId;
        this.symbol = b.symbol;
        this.price = b.price;
        this.orderQty = b.orderQty;
        this.leavesQty = b.orderQty;
        this.cumQty = 0.0;
        this.avgPx = 0.0;
        this.side = b.side;
        this.orderType = b.orderType;
        this.tif = b.tif;
        this.ordStatus = OrdStatus.NEW;
        // FIX 6: System.nanoTime() is not wall-clock time per JVM spec.
        // Replaced with Instant.now() for correct audit-trail timestamps.
        this.createdAt = Instant.now();
    }

    // ── Convenience static factories (backward-compatible) ──

    public static Order limitBuy(long orderId, double price, double quantity, int scale) {
        return new Builder(orderId, Side.BUY, OrderType.LIMIT, quantity)
                .price(Price.toLong(price, scale))
                .build();
    }

    public static Order limitSell(long orderId, double price, double quantity, int scale) {
        return new Builder(orderId, Side.SELL, OrderType.LIMIT, quantity)
                .price(Price.toLong(price, scale))
                .build();
    }

    public static Order marketBuy(long orderId, double quantity) {
        return new Builder(orderId, Side.BUY, OrderType.MARKET, quantity).build();
    }

    public static Order marketSell(long orderId, double quantity) {
        return new Builder(orderId, Side.SELL, OrderType.MARKET, quantity).build();
    }

    // ── Fill logic ──

    /**
     * Apply a fill of the given quantity at the given scaled-integer price.
     * Updates leavesQty, cumQty, avgPx, and ordStatus.
     *
     * This is the SINGLE mutation path for fills — all matching must route through here.
     */
    public void applyFill(double fillQty, long fillPrice) {
        double newCumQty = this.cumQty + fillQty;
        // Volume-weighted average price
        this.avgPx = ((this.avgPx * this.cumQty) + ((double) fillPrice * fillQty)) / newCumQty;
        this.cumQty = newCumQty;
        this.leavesQty -= fillQty;
        this.ordStatus = isFilled() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
    }

    public boolean isFilled() {
        return this.leavesQty <= 1e-12;
    }

    public void cancel() {
        this.ordStatus = OrdStatus.CANCELED;
    }

    // ── Getters ──

    public String getClOrdId() { return clOrdId; }
    public long getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public long getPrice() { return price; }
    public double getOrderQty() { return orderQty; }
    /** Remaining (leaves) quantity. */
    public double getQuantity() { return leavesQty; }
    public double getLeavesQty() { return leavesQty; }
    public double getCumQty() { return cumQty; }
    public double getAvgPx() { return avgPx; }
    /** Wall-clock creation timestamp. */
    public Instant getCreatedAt() { return createdAt; }
    /** @deprecated use {@link #getCreatedAt()} for wall-clock time */
    @Deprecated
    public long getTimestamp() { return createdAt.toEpochMilli(); }
    public Side getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public TimeInForce getTimeInForce() { return tif; }
    public OrdStatus getOrdStatus() { return ordStatus; }

    public boolean isBid() { return side == Side.BUY; }
    public boolean isMarket() { return orderType == OrderType.MARKET; }

    // ── Builder ──

    public static class Builder {
        private final long orderId;
        private final Side side;
        private final OrderType orderType;
        private final double orderQty;
        private String clOrdId;
        private String symbol;
        private long price;
        private TimeInForce tif = TimeInForce.GTC;

        public Builder(long orderId, Side side, OrderType orderType, double orderQty) {
            this.orderId = orderId;
            this.side = side;
            this.orderType = orderType;
            this.orderQty = orderQty;
        }

        public Builder clOrdId(String clOrdId) { this.clOrdId = clOrdId; return this; }
        public Builder symbol(String symbol) { this.symbol = symbol; return this; }
        public Builder price(long price) { this.price = price; return this; }
        public Builder timeInForce(TimeInForce tif) { this.tif = tif; return this; }

        public Order build() {
            return new Order(this);
        }
    }
}
