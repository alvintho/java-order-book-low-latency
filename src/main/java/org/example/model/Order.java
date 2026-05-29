package org.example.model;

import org.example.util.Price;

// TODO: Native modify --> cancel, reject, priority shift (Done remove modify Order function)
// TODO: Interfaces --> abstract the Orderbook
// TODO: Not using OrderType
// TODO: Re-evaluate buyOrderId and sellOrderId with FIX

public class Order {
    private final long orderId;
    private final long price;
    private double quantity;
    private final long timestamp;
    private final Side side;
    private final OrderType orderType;

    // ── Private constructor — all creation goes through factories
    private Order(long orderId, long price, double quantity,
                  Side side, OrderType orderType) {
        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "Order ID must be positive: " + orderId);
        }
        if (quantity <= 0.0) {
            throw new IllegalArgumentException(
                    "Quantity must be positive: " + quantity);
        }

        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.orderType = orderType;
        this.timestamp = System.nanoTime();
    }

    // ── Static factory methods ──────────────────────────────

    public static Order limitBuy(long orderId, double price,
                                 double quantity, int scale) {
        return new Order(orderId, Price.toLong(price, scale),
                quantity, Side.BUY, OrderType.LIMIT);
    }

    public static Order limitSell(long orderId, double price,
                                  double quantity, int scale) {
        return new Order(orderId, Price.toLong(price, scale),
                quantity, Side.SELL, OrderType.LIMIT);
    }

    public static Order marketBuy(long orderId, double quantity) {
        return new Order(orderId, 0L, quantity,
                Side.BUY, OrderType.MARKET);
    }

    public static Order marketSell(long orderId, double quantity) {
        return new Order(orderId, 0L, quantity,
                Side.SELL, OrderType.MARKET);
    }

    public long getOrderId() {
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

    public OrderType getOrderType() {
        return this.orderType;
    }

    public boolean isBid() {
        return this.side == Side.BUY;
    }

    public boolean isMarket() {
        return this.orderType == OrderType.MARKET;
    }

    public void reduceQuantity(double tradeQuantity) {
        this.quantity -= tradeQuantity;
    }

    public boolean isFilled() {
        return this.quantity == 0;
    }
}