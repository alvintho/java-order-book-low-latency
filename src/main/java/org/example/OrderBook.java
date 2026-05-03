package org.example;

import java.util.*;

public class OrderBook {
    private final Instrument instrument;
    private double totalVolume; // sum of all Bids + Sum of all Asks
    private double totalBidVolume;
    private double totalAskVolume;
    private int tradeCount;
    private final TreeMap<Long, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder()); // Buy to the lowest ask-er
    private final TreeMap<Long, Queue<Order>> asks = new TreeMap<>(); // Sell to the highest bidder
    private final Map<UUID, Order> orderMap = new HashMap<>();

    public OrderBook(Instrument instrument) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument cannot be null");
        }
        this.instrument = instrument;
        this.totalVolume = 0;
        this.totalBidVolume = 0;
        this.totalAskVolume = 0;
        this.tradeCount = 0;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public List<Trade> addOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (orderMap.containsKey(order.getOrderId())) {
            throw new IllegalStateException("Order " + order.getOrderId() + " already exists");
        }

        TreeMap<Long, Queue<Order>> book = order.isBid() ? bids : asks;
        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        this.orderMap.put(order.getOrderId(), order);
        this.addVolume(order.getQuantity(), order.getSide());

        return this.matchOrder(order);
    }


    public void cancelOrder (UUID orderId) {
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

    public List<Trade> modifyOrder(UUID orderId, Order modifiedOrder) {
        if (modifiedOrder == null || modifiedOrder.getOrderId() == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        Order existingOrder = this.orderMap.get(orderId);
        if (existingOrder == null) {
            throw new IllegalStateException("Order " + orderId + " not found");
        }

        if (existingOrder.getSide() != modifiedOrder.getSide()) {
            throw new IllegalArgumentException("Cannot modify order of different side");
        }

        this.cancelOrder(existingOrder.getOrderId());
        return this.addOrder(modifiedOrder);
    }

    private void removeOrderFromBook(Order order) {
        /*
         * 1. Get the book
         * 2. Get the queue of orders at the price
         * 3. Remove the order from the queue
         * */

        TreeMap<Long, Queue<Order>> book = order.isBid() ? bids : asks;
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

    private List<Trade> matchOrder(Order incomingOrder) {
        TreeMap<Long, Queue<Order>> oppositeBook = incomingOrder.isBid() ? asks : bids;
        List<Trade> trades = new ArrayList<>();

        double incomingOrderPrice = incomingOrder.getPrice();

        while(!oppositeBook.isEmpty() && !incomingOrder.isFilled()) {
            Long bestOppositePrice = oppositeBook.firstKey();

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

            // Handle full-fills
            if (restingOrder.isFilled()) {
                this.removeOrderFromBook(restingOrder);
            }

            if (incomingOrder.isFilled()) {
                this.removeOrderFromBook(incomingOrder);
            }

            UUID buyOrderId = incomingOrder.isBid() ? incomingOrder.getOrderId() : restingOrder.getOrderId();
            UUID sellOrderId = incomingOrder.isBid() ? restingOrder.getOrderId() : incomingOrder.getOrderId();

            trades.add(new Trade(
                    bestOppositePrice,
                    tradeQuantity,
                    buyOrderId,
                    sellOrderId
            ));
            this.tradeCount += 1;
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
        return bids.isEmpty()
                ? Double.NaN
                : Price.toDouble(bids.firstKey(), instrument.getScale());
    }

    public double getBestAsk() {
        return asks.isEmpty()
                ? Double.NaN
                : Price.toDouble(asks.firstKey(), instrument.getScale());
    }

    private Queue<Order> getOrdersAtPrice(double price, Side side) {
        TreeMap<Long, Queue<Order>> book = side == Side.BUY ? bids : asks;
        long scaledPrice = Price.toLong(price, instrument.getScale());
        return book.get(scaledPrice);
    }

    public int getOrderCountAtPrice(double price, Side side) {
        Queue<Order> orders = this.getOrdersAtPrice(price, side);
        return orders == null ? 0 : orders.size();
    }

    public double getVolumeAtPrice(double price, Side side) {
        Queue<Order> orders = this.getOrdersAtPrice(price, side);

        if (orders == null) {
            return 0;
        }

        return orders.parallelStream().mapToDouble(Order::getQuantity).sum();
    }

    public Order getOrder(UUID orderId) {
        return this.orderMap.get(orderId);
    }

    public double getTotalVolume() {
        return this.totalVolume;
    }

    public double getSpread() {
        if (bids.isEmpty() || asks.isEmpty()) {
            return Double.NaN;
        }

        long bestBid = bids.firstKey();
        long bestAsk = asks.firstKey();
        long spreadTicks = bestAsk - bestBid;

        return Price.toDouble(spreadTicks, instrument.getScale());
    }

    public int getDepth(Side side) {
        TreeMap<Long, Queue<Order>> book = side == Side.BUY ? this.bids : this.asks;

        return book.size();
    }

    public int getTradeCounts() {
        return this.tradeCount;
    }
}
