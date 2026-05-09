package org.example;

public class Trade {
    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long price;
    private final double quantity;
    private final long timestamp;

    public Trade(long tradeId,long price, double quantity, long buyOrderId, long sellOrderId) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.nanoTime();
    }

    public long getTradeId() {
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

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }
}
