package org.example;

import java.util.UUID;

public class Order {
    private final UUID orderId;;
    private final double price;
    private final double quantity;
    private final long timestamp;
    private final Side side;

    public Order(double price, double quantity, Side side) {
        this.orderId = UUID.randomUUID();

        if (price <= 0.0) {
            throw new IllegalArgumentException("Price must be positive: " + price);
        }

        if (quantity <= 0.0) {
            throw new IllegalArgumentException("Quantity must be positive: " + price);
        }

        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.timestamp = System.nanoTime();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Side getSide() {
        return side;
    }

    public boolean isBid() {
        return this.side == Side.BUY;
    }
}
