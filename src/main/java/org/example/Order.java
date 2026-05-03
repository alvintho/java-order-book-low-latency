package org.example;

import java.util.UUID;

public class Order {
    private final UUID orderId;
    private final long price;
    private double quantity;
    private final long timestamp;
    private final Side side;

    public Order(double price, double quantity, Side side, int scale) {
        if (quantity <= 0.0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }

        this.orderId = UUID.randomUUID();
        this.price = Price.toLong(price, scale);
        this.quantity = quantity;
        this.side = side;
        this.timestamp = System.nanoTime();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public long getPrice() {
        return this.price;
    }

    public double getQuantity() {
        return this.quantity;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public Side getSide() {
        return this.side;
    }

    public boolean isBid() {
        return this.side == Side.BUY;
    }

    public void reduceQuantity(double tradeQuantity) {
        this.quantity -= tradeQuantity;
    }

    public boolean isFilled() {
        return this.quantity == 0;
    }
}