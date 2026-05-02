package org.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OrderBookBenchmarkTest {

    @Test
    void benchmarkOrderBookOperations() {
        Random random = new Random(42);
        int iterations = 100_000;

        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            double price = 100.0 + (random.nextInt(200) - 100) * 0.01;
            double quantity = 1 + random.nextInt(100);
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            orders.add(new Order(price, quantity, side));
        }

        // Warmup
        runBook(orders.subList(0, 10_000));

        // Benchmark: Add + Match
        OrderBook book = new OrderBook();
        long startTime = System.nanoTime();

        for (Order order : orders) {
            book.addOrder(order);
        }

        long endTime = System.nanoTime();
        double avgLatencyNs = (double) (endTime - startTime) / iterations;
        double opsPerSecond = (iterations * 1_000_000_000.0) / (endTime - startTime);

        System.out.println("=== Order Book Benchmark (Single-Threaded) ===");
        System.out.println("Operations:     " + iterations);
        System.out.println("Total time:     " + (endTime - startTime) / 1_000_000 + " ms");
        System.out.println("Avg latency:    " + String.format("%.0f", avgLatencyNs) + " ns");
        System.out.println("Ops/second:     " + String.format("%,.0f", opsPerSecond));
        System.out.println("Trades:         " + book.getTradeCounts());
        System.out.println("Bid depth:      " + book.getDepth(Side.BUY));
        System.out.println("Ask depth:      " + book.getDepth(Side.SELL));
        System.out.println("Spread:         " + book.getSpread());
        System.out.println("Total volume:   " + book.getTotalVolume());
    }

    private void runBook(List<Order> orders) {
        OrderBook book = new OrderBook();
        for (Order order : orders) {
            book.addOrder(order);
        }
    }
}