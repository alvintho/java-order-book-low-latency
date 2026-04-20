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

    private void removeOrderFromBook(Order order) {
        /*
        * 1. Get the book
        * 2. Get the queue of orders at the price
        * 3. Remove the order from the queue
        * */

        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;
        Queue<Order> ordersAtPrice = book.get(order.getPrice());

        if (ordersAtPrice != null) {
            ordersAtPrice.remove(order);
            if (ordersAtPrice.isEmpty()) {
                book.remove(order.getPrice());
            }
        }

        this.orderMap.remove(order.getOrderId());
    }

    private void addVolume(double quantity, Side side) {
        this.totalVolume += quantity;
        if (side == Side.BUY) {
            this.totalBidVolume += quantity;
        } else {
            this.totalAskVolume += quantity;
        }
    }

    private void removeVolume(double quantity, Side side) {
        this.totalVolume -= quantity;
        if (side == Side.BUY) {
            this.totalBidVolume -= quantity;
        } else {
            this.totalAskVolume -= quantity;
        }
    }

    public Trade addOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;

        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        this.orderMap.put(order.getOrderId(), order);
        this.addVolume(order.getQuantity(), order.getSide());

        return this.matchOrder(order);
    }

    public void removeOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (!orderMap.containsKey(order.getOrderId())) {
            throw new IllegalStateException("Order " + order.getOrderId() + " not found");
        }

        this.removeOrderFromBook(order);
        this.removeVolume(order.getQuantity(), order.getSide());
    }

    private Trade matchOrder(Order incomingOrder) {
        TreeMap<Double, Queue<Order>> oppositeBook = incomingOrder.isBid() ? asks : bids;

        if (oppositeBook.isEmpty()) return null;
        double incomingOrderPrice = incomingOrder.getPrice();
        double totalTradeQuantity = 0;
        double tradePrice = 0; // TODO: revisit as currently using the last price

        while(!oppositeBook.isEmpty() && !incomingOrder.isFilled()) {
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
            Queue<Order> ordersAtBestPrice = oppositeBook.get(bestOppositePrice);
            Order restingOrder = ordersAtBestPrice.peek();
            if (restingOrder == null) return null;

            double tradeQuantity = Math.min(incomingOrder.getQuantity(), restingOrder.getQuantity());

            // Handle partial fills
            incomingOrder.reduceQuantity(tradeQuantity);
            restingOrder.reduceQuantity(tradeQuantity);

            removeVolume(tradeQuantity, restingOrder.getSide()); // Always remove traded volume from resting side
            removeVolume(tradeQuantity, incomingOrder.getSide());

            // Handle full fills
            if (restingOrder.isFilled()) {
                this.removeOrderFromBook(restingOrder);
            }

            if (incomingOrder.isFilled()) {
                this.removeOrderFromBook(incomingOrder);
            }

            tradePrice = bestOppositePrice;
            totalTradeQuantity += tradeQuantity;
        }



        return new Trade(tradePrice, totalTradeQuantity);
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
