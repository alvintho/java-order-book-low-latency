package org.example.domain.model;

import org.example.domain.enums.ExecType;
import org.example.domain.enums.OrdStatus;
import org.example.domain.enums.Side;

import java.time.Instant;

/**
 * FIX 4.4 ExecutionReport (MsgType = '8').
 *
 * Emitted by the order book for every state change:
 * new order acknowledgement, partial fill, full fill, cancel, replace, reject.
 *
 * <p>This is an immutable snapshot — it does NOT hold a live reference
 * to the Order, preventing callers from mutating order state through reports.
 *
 * <p>Pattern: Value Object — equality by content, not identity.
 */
public final class ExecutionReport {

    // FIX Tag 11
    private final String    clOrdId;
    // FIX Tag 37 (internal engine ID used as OrderID)
    private final long      orderId;
    // FIX Tag 55
    private final String    symbol;
    // FIX Tag 54
    private final Side side;
    // FIX Tag 150
    private final ExecType execType;
    // FIX Tag 39
    private final OrdStatus ordStatus;
    // FIX Tag 32 — quantity of this specific fill (0 for non-fill events)
    private final double    lastQty;
    // FIX Tag 31 — price of this specific fill (0 for non-fill events, scaled)
    private final long      lastPx;
    // FIX Tag 151
    private final double    leavesQty;
    // FIX Tag 14
    private final double    cumQty;
    // FIX Tag 6 (scaled integer)
    private final double    avgPx;
    private final Instant   timestamp;

    private ExecutionReport(Builder b) {
        this.clOrdId   = b.clOrdId;
        this.orderId   = b.orderId;
        this.symbol    = b.symbol;
        this.side      = b.side;
        this.execType  = b.execType;
        this.ordStatus = b.ordStatus;
        this.lastQty   = b.lastQty;
        this.lastPx    = b.lastPx;
        this.leavesQty = b.leavesQty;
        this.cumQty    = b.cumQty;
        this.avgPx     = b.avgPx;
        this.timestamp = Instant.now();
    }

    // ── Factories for common event types ──────────────────────────────────────

    /** Acknowledge a new resting order (no fill yet). */
    public static ExecutionReport ack(Order o) {
        return new Builder(o)
                .execType(ExecType.NEW)
                .ordStatus(OrdStatus.NEW)
                .build();
    }

    /** Record a fill (partial or full) against an order. */
    public static ExecutionReport fill(Order o, double lastQty, long lastPx) {
        ExecType execType = o.isFilled() ? ExecType.FILL : ExecType.PARTIAL_FILL;
        return new Builder(o)
                .execType(execType)
                .ordStatus(o.getOrdStatus())
                .lastQty(lastQty)
                .lastPx(lastPx)
                .build();
    }

    /** Record a cancel event. */
    public static ExecutionReport canceled(Order o) {
        return new Builder(o)
                .execType(ExecType.CANCELED)
                .ordStatus(OrdStatus.CANCELED)
                .build();
    }

    /** Record an order-replace acknowledgement. */
    public static ExecutionReport replaced(Order o) {
        return new Builder(o)
                .execType(ExecType.REPLACED)
                .ordStatus(o.getOrdStatus())
                .build();
    }

    /** Record a rejection. */
    public static ExecutionReport rejected(Order o) {
        return new Builder(o)
                .execType(ExecType.REJECTED)
                .ordStatus(OrdStatus.REJECTED)
                .build();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String    getClOrdId()   { return clOrdId;   }
    public long      getOrderId()   { return orderId;   }
    public String    getSymbol()    { return symbol;    }
    public Side      getSide()      { return side;      }
    public ExecType  getExecType()  { return execType;  }
    public OrdStatus getOrdStatus() { return ordStatus; }
    public double    getLastQty()   { return lastQty;   }
    public long      getLastPx()    { return lastPx;    }
    public double    getLeavesQty() { return leavesQty; }
    public double    getCumQty()    { return cumQty;    }
    public double    getAvgPx()     { return avgPx;     }
    public Instant   getTimestamp() { return timestamp; }

    // ── Builder ───────────────────────────────────────────────────────────────

    private static final class Builder {
        private final String clOrdId;
        private final long   orderId;
        private final String symbol;
        private final Side   side;
        private final double leavesQty;
        private final double cumQty;
        private final double avgPx;

        private ExecType  execType;
        private OrdStatus ordStatus;
        private double    lastQty = 0.0;
        private long      lastPx  = 0L;

        Builder(Order o) {
            this.clOrdId   = o.getClOrdId();
            this.orderId   = o.getOrderId();
            this.symbol    = o.getSymbol();
            this.side      = o.getSide();
            this.leavesQty = o.getLeavesQty();
            this.cumQty    = o.getCumQty();
            this.avgPx     = o.getAvgPx();
        }

        Builder execType(ExecType v)  { this.execType  = v; return this; }
        Builder ordStatus(OrdStatus v){ this.ordStatus = v; return this; }
        Builder lastQty(double v)     { this.lastQty   = v; return this; }
        Builder lastPx(long v)        { this.lastPx    = v; return this; }

        ExecutionReport build() { return new ExecutionReport(this); }
    }
}