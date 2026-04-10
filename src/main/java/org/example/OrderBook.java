package org.example;

import java.util.*;

public class OrderBook {
    private double totalVolume; // sum of all Bids + Sum of all Asks
    private final TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
    private final Map<UUID, Order> orderMap = new HashMap<>();

    public OrderBook() {
        this.totalVolume = 0;
    }

    public double getTotalVolume() {
        return this.totalVolume;
    }

    public void addOrder(Order order) {
        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;

        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);

        this.orderMap.put(order.getOrderId(), order);
    }

    public double getBestBid() {
        return bids.isEmpty() ? Double.NaN : bids.firstKey();
    }

    public double getBestAsk() {
        return asks.isEmpty() ? Double.NaN : asks.firstKey();
    }
}
