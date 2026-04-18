package org.example;

import java.util.UUID;

public class Trade {
    private final UUID tradeId;
    private final double price;
    private final double quantity;
    private final long timestamp;

    public Trade(double price, double quantity) {
        this.tradeId = UUID.randomUUID();
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.nanoTime();
    }

    public UUID getTradeId() {
        return this.tradeId;
    }

    public double getQuantity() {
        return this.quantity;
    }

    public double getPrice() {
        return this.price;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
