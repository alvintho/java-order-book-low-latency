package org.example.domain.model;

import org.example.domain.enums.OrdStatus;
import org.example.domain.enums.OrderType;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.util.Price;

import java.time.Instant;

/**
 * Domain order aligned to FIX 4.4 NewOrderSingle (D).
 *
 * <p>Mutable only through {@link #applyFill} and {@link #cancel()}.
 * All other state is set at construction via the Builder.
 *
 * <p>Thread-safety: not thread-safe — single-threaded use per book assumed.
 *
 * <p>Bug fix: removed ambiguous {@code getQuantity()} alias.
 * All callers now use {@link #getLeavesQty()} explicitly.
 */
public final class Order {

    private final String      clOrdId;
    private final long        orderId;
    private final String      symbol;
    private final long        price;
    private final double      orderQty;
    private       double      leavesQty;
    private       double      cumQty;
    private       double      avgPx;
    private final Instant     createdAt;
    private final Side        side;
    private final OrderType   orderType;
    private final TimeInForce tif;
    private       OrdStatus   ordStatus;

    private Order(Builder b) {
        if (b.orderId  <= 0)   throw new IllegalArgumentException("orderId must be positive");
        if (b.orderQty <= 0.0) throw new IllegalArgumentException("orderQty must be positive");

        this.clOrdId   = b.clOrdId;
        this.orderId   = b.orderId;
        this.symbol    = b.symbol;
        this.price     = b.price;
        this.orderQty  = b.orderQty;
        this.leavesQty = b.orderQty;
        this.cumQty    = 0.0;
        this.avgPx     = 0.0;
        this.side      = b.side;
        this.orderType = b.orderType;
        this.tif       = b.tif;
        this.ordStatus = OrdStatus.NEW;
        this.createdAt = Instant.now();
    }

    // ── Convenience factories (explicit TIF — preferred) ──────────────────────

    /**
     * Create a limit order with explicit side and time-in-force.
     * Preferred over the side-named overloads for programmatic use.
     */
    public static Order limit(long orderId, Side side, double price,
                              double qty, TimeInForce tif, int scale) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        return new Builder(orderId, side, OrderType.LIMIT, qty)
                .price(Price.toLong(price, scale))
                .timeInForce(tif)
                .build();
    }

    /**
     * Create a market order. Market orders are implicitly IOC —
     * they never rest in the book.
     */
    public static Order market(long orderId, Side side, double qty) {
        return new Builder(orderId, side, OrderType.MARKET, qty)
                .timeInForce(TimeInForce.IOC)
                .build();
    }

    // ── Legacy named factories (kept for backward compatibility) ──────────────

    public static Order limitBuy(long orderId, double price, double qty, int scale) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        return new Builder(orderId, Side.BUY, OrderType.LIMIT, qty)
                .price(Price.toLong(price, scale))
                .timeInForce(TimeInForce.GTC)
                .build();
    }

    public static Order limitSell(long orderId, double price, double qty, int scale) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        return new Builder(orderId, Side.SELL, OrderType.LIMIT, qty)
                .price(Price.toLong(price, scale))
                .timeInForce(TimeInForce.GTC)
                .build();
    }

    public static Order marketBuy(long orderId, double qty) {
        return new Builder(orderId, Side.BUY, OrderType.MARKET, qty)
                .timeInForce(TimeInForce.IOC)
                .build();
    }

    public static Order marketSell(long orderId, double qty) {
        return new Builder(orderId, Side.SELL, OrderType.MARKET, qty)
                .timeInForce(TimeInForce.IOC)
                .build();
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public void applyFill(double fillQty, long fillPrice) {
        if (fillQty <= 0) throw new IllegalArgumentException("fillQty must be positive");
        double newCum  = this.cumQty + fillQty;
        this.avgPx     = ((this.avgPx * this.cumQty) + ((double) fillPrice * fillQty)) / newCum;
        this.cumQty    = newCum;
        this.leavesQty -= fillQty;
        this.ordStatus = isFilled() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
    }

    public void cancel() {
        if (this.ordStatus != OrdStatus.FILLED) {
            this.ordStatus = OrdStatus.CANCELED;
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isFilled() { return this.leavesQty <= 1e-12; }
    public boolean isBid()    { return this.side == Side.BUY;   }
    public boolean isMarket() { return this.orderType == OrderType.MARKET; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String      getClOrdId()     { return clOrdId;            }
    public long        getOrderId()     { return orderId;            }
    public String      getSymbol()      { return symbol;             }
    public long        getPrice()       { return price;              }
    public double      getOrderQty()    { return orderQty;           }
    public double      getLeavesQty()   { return leavesQty;          }
    public double      getCumQty()      { return cumQty;             }
    public double      getAvgPx()       { return avgPx;              }
    public Instant     getCreatedAt()   { return createdAt;          }
    public Side        getSide()        { return side;               }
    public OrderType   getOrderType()   { return orderType;          }
    public TimeInForce getTimeInForce() { return tif;                }
    public OrdStatus   getOrdStatus()   { return ordStatus;          }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {
        private final long      orderId;
        private final Side      side;
        private final OrderType orderType;
        private final double    orderQty;

        private String      clOrdId = "";
        private String      symbol  = "";
        private long        price   = 0L;
        private TimeInForce tif     = TimeInForce.GTC;

        public Builder(long orderId, Side side, OrderType orderType, double orderQty) {
            this.orderId   = orderId;
            this.side      = side;
            this.orderType = orderType;
            this.orderQty  = orderQty;
        }

        public Builder clOrdId(String v)         { this.clOrdId = v; return this; }
        public Builder symbol(String v)           { this.symbol  = v; return this; }
        public Builder price(long v)              { this.price   = v; return this; }
        public Builder timeInForce(TimeInForce v) { this.tif     = v; return this; }

        public Order build() { return new Order(this); }
    }
}