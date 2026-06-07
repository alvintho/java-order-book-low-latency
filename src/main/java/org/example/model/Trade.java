package org.example.model;

import java.time.Instant;

/**
 * An executed trade between two orders.  Immutable value object.
 *
 * <p><b>Price</b> is a scaled integer ({@code long}).  Use the instrument's
 * scale to convert to a display price: {@code price / scale}.
 */
public class Trade {
    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long price;          // scaled integer
    private final double quantity;
    private final Instant createdAt;

    public Trade(long tradeId, long price, double quantity, long buyOrderId, long sellOrderId) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.createdAt = Instant.now();
    }

    public long   getTradeId()    { return tradeId; }
    public double getQuantity()   { return quantity; }
    /** Scaled-integer execution price. */
    public long   getPrice()      { return price; }
    public long   getBuyOrderId() { return buyOrderId; }
    public long   getSellOrderId(){ return sellOrderId; }
    public Instant getCreatedAt()  { return createdAt; }
}
