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

    public List<Trade> addOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        TreeMap<Double, Queue<Order>> book = order.isBid() ? bids : asks;

        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        this.orderMap.put(order.getOrderId(), order);
        this.addVolume(order.getQuantity(), order.getSide());

        return this.matchOrder(order);
    }

    public void removeOrder(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Order order = this.orderMap.get(orderId);

        if (order == null) {
            throw new IllegalStateException("Order " + orderId + " not found");
        }

        this.removeOrderFromBook(order);
        this.removeVolume(order.getQuantity(), order.getSide());
    }

    private List<Trade> matchOrder(Order incomingOrder) {
        TreeMap<Double, Queue<Order>> oppositeBook = incomingOrder.isBid() ? asks : bids;
        List<Trade> trades = new ArrayList<>();

        double incomingOrderPrice = incomingOrder.getPrice();

        while(!oppositeBook.isEmpty() && !incomingOrder.isFilled()) {
            double bestOppositePrice = oppositeBook.firstKey();

            boolean isPriceMatch = incomingOrder.isBid()
                    ? incomingOrderPrice >= bestOppositePrice
                    : incomingOrderPrice <= bestOppositePrice;

            if (!isPriceMatch) break;

            Queue<Order> ordersAtBestPrice = oppositeBook.get(bestOppositePrice);

            if (ordersAtBestPrice == null || ordersAtBestPrice.isEmpty()) {
                oppositeBook.remove(bestOppositePrice);
                continue;
            }

            Order restingOrder = ordersAtBestPrice.peek();
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

            trades.add(new Trade(bestOppositePrice, tradeQuantity));
        }



        return trades;
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

    public Order getOrder(UUID orderId) {
        return this.orderMap.get(orderId);
    }

    public double getTotalVolume() {
        return this.totalVolume;
    }
}
