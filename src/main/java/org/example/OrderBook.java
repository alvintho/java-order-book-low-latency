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
        this.totalBidVolume = 0;
        this.totalAskVolume = 0;
    }

    private Trade matchOrder(Order incomingOrder, TreeMap<Double, Queue<Order>> oppositeBook) {
        if (oppositeBook.isEmpty()) return null;

        double incomingOrderPrice = incomingOrder.getPrice();
        double bestOppositePrice = oppositeBook.firstKey();

        boolean isPriceMatch = incomingOrder.isBid()
                ? incomingOrderPrice >= bestOppositePrice
                : incomingOrderPrice <= bestOppositePrice;

        if (!isPriceMatch) return null;

        boolean isOpposingOrdersEmpty = oppositeBook.firstEntry().getValue().isEmpty();

        if (isOpposingOrdersEmpty) {
            oppositeBook.remove(bestOppositePrice);
            return null;
        }

        // Execute trade
        Order restingOrder = oppositeBook.firstEntry().getValue().poll();
        double tradeQuantity = Math.min(incomingOrder.getQuantity(), restingOrder.getQuantity());

        incomingOrder.reduceQuantity(tradeQuantity);
        restingOrder.reduceQuantity(tradeQuantity);

        if (restingOrder.isFilled()) {
            this.orderMap.remove(restingOrder.getOrderId());
            Queue<Order> ordersAtPrice = oppositeBook.get(restingOrder.getPrice());
            if (ordersAtPrice != null) {
                ordersAtPrice.remove(restingOrder);
                if (ordersAtPrice.isEmpty()) {
                    oppositeBook.remove(restingOrder.getPrice());
                }
            }

            if (restingOrder.isBid()) {
                this.totalBidVolume -= tradeQuantity;
            } else {
                this.totalAskVolume -= tradeQuantity;
            }
        }

        return new Trade(bestOppositePrice, tradeQuantity);
    }

    public Trade addOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        boolean isBidOrder = order.isBid();
        TreeMap<Double, Queue<Order>> book = isBidOrder ? bids : asks;
        TreeMap<Double, Queue<Order>> oppositeBook = isBidOrder ? asks : bids;

        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        this.orderMap.put(order.getOrderId(), order);
        this.totalVolume += order.getQuantity();

        if (order.isBid()) {
            this.totalBidVolume += order.getQuantity();
        } else {
            this.totalAskVolume += order.getQuantity();
        }

        Trade trade = this.matchOrder(order, oppositeBook);

        if (trade != null) {
            this.totalVolume -= trade.getQuantity() * 2;

            if (order.isBid()) {
                this.totalBidVolume -= trade.getQuantity();
            } else {
                this.totalAskVolume -= trade.getQuantity();
            }

            this.orderMap.remove(order.getOrderId());
            book.get(order.getPrice()).remove(order);
            if (book.get(order.getPrice()).isEmpty()) {
                book.remove(order.getPrice());
            }

            return trade;
        }

        return null;
    }

    public void removeOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (!orderMap.containsKey(order.getOrderId())) {
            throw new IllegalStateException("Order " + order.getOrderId() + " not found");
        }

        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;
        Queue<Order> ordersAtPrice = book.get(order.getPrice());

        if (ordersAtPrice == null) {
            throw new IllegalStateException("Order " + order.getOrderId() + " not found at price " + order.getPrice());
        }

        ordersAtPrice.remove(order);

        if (ordersAtPrice.isEmpty()) {
            book.remove(order.getPrice());
        }

        this.totalVolume -= order.getQuantity();
        orderMap.remove(order.getOrderId());
        if (order.isBid()) {
            this.totalBidVolume -= order.getQuantity();
        } else {
            this.totalAskVolume -= order.getQuantity();
        }
    }

    public double getTotalBidVolume() {
        return this.totalBidVolume;
    }

    public double getTotalAskVolume() {
        return this.totalAskVolume;
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

        if (orders == null) {
            return 0;
        }

        return orders.size();
    }

    public Order getOrder(Order order) {
        return this.orderMap.get(order.getOrderId());
    }

    public double getTotalVolume() {
        return this.totalVolume;
    }
}
