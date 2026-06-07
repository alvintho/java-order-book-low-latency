package org.example.domain.model;

import java.time.Instant;

/**
 * Internal record of one matched trade produced by the matching engine.
 *
 * <p>This is NOT a FIX message. In FIX 4.4 a match is reported as two
 * separate ExecutionReport (8) messages — one per party. {@code Trade}
 * is the internal object that carries enough information to generate
 * both of those reports.
 *
 * <p>{@code buyOrderId} and {@code sellOrderId} are internal engine IDs
 * (corresponding to FIX Tag 37 OrderID), not FIX wire fields. They exist
 * so that downstream systems (risk, clearing, audit log) can identify
 * both sides of a match from a single record.
 *
 * <p>Immutable value object. Thread-safe.
 */
public final class Trade {

    private final long    tradeId;
    private final long    buyOrderId;
    private final long    sellOrderId;
    private final long    price;      // scaled integer
    private final double  quantity;
    private final Instant executedAt;

    public Trade(long tradeId, long price, double quantity,
                 long buyOrderId, long sellOrderId) {
        this.tradeId     = tradeId;
        this.price       = price;
        this.quantity    = quantity;
        this.buyOrderId  = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.executedAt  = Instant.now();
    }

    public long    getTradeId()     { return tradeId;     }
    public long    getPrice()       { return price;       }
    public double  getQuantity()    { return quantity;    }
    /** Internal engine ID of the buy-side order (not a FIX wire field). */
    public long    getBuyOrderId()  { return buyOrderId;  }
    /** Internal engine ID of the sell-side order (not a FIX wire field). */
    public long    getSellOrderId() { return sellOrderId; }
    public Instant getExecutedAt()  { return executedAt;  }
}