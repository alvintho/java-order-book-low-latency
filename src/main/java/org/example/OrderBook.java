package org.example;

import java.util.*;

public class OrderBook {
    private double totalVolume; // sum of all Bids + Sum of all Asks
    private double totalBidVolume;
    private double totalAskVolume;
    private final TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder()); // Buy to the lowest asker
    private final TreeMap<Double, Queue<Order>> asks = new TreeMap<>(); // Sell to the highest bidder
    private final Map<UUID, Order> orderMap = new HashMap<>();

    public OrderBook() {
        this.totalVolume = 0;
    }

    public void addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;
        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        this.orderMap.put(order.getOrderId(), order);

        this.totalVolume += order.getQuantity();

        if (order.isBid()) {
            this.totalBidVolume += order.getQuantity();
        } else {
            this.totalAskVolume += order.getQuantity();
        }
    }

    public void removeOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;
        Queue<Order> orders = book.get(order.getPrice());
        orders.remove(order);
    }

    public double getBestBid() {
        return bids.isEmpty() ? Double.NaN : bids.firstKey();
    }

    public double getBestAsk() {
        return asks.isEmpty() ? Double.NaN : asks.firstKey();
    }

    public int getOrderCountAtPrice(double price, Side side) {
        TreeMap<Double, Queue<Order>> book = side == Side.BUY ? bids : asks;

        Queue<Order> orders =  book.get(price);

        return orders.size();
    }

    public double getTotalVolume() {
        return this.totalVolume;
    }
}
