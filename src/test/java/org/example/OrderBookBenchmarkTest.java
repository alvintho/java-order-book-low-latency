package org.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class OrderBookBenchmarkTest {

    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 10;
    private static final int ITERATIONS = 100_000;

    @Test
    void benchmarkAddOrder() {
        Instrument instrument = new Instrument("AAPL", 100, 1);

        /*
        * Without warmup, your first few runs pollute results with JVM startup costs.
        * The warmup runs exist to let the JIT compiler fully optimize your code before you start measuring.
        * */
        for (int i = 0; i < WARMUP_RUNS; i++) {
            runSingleBenchmark(instrument, i);
        }

        // Measured runs
        long[] durations = new long[MEASURED_RUNS];
        int trades = 0;
        int bidDepth = 0;
        int askDepth = 0;

        for (int i = 0; i < MEASURED_RUNS; i++) {
            BenchmarkResult result = runSingleBenchmark(instrument, WARMUP_RUNS + i);
            durations[i] = result.durationNs;
            trades = result.trades;
            bidDepth = result.bidDepth;
            askDepth = result.askDepth;
        }

        Arrays.sort(durations);

        long min = durations[0];
        long max = durations[MEASURED_RUNS - 1];
        long median = durations[MEASURED_RUNS / 2];
        long avg = Arrays.stream(durations).sum() / MEASURED_RUNS;

        System.out.println("=== Add Order Benchmark (Single-Threaded) ===");
        System.out.println("Iterations/run: " + ITERATIONS);
        System.out.println("Warmup runs:    " + WARMUP_RUNS);
        System.out.println("Measured runs:  " + MEASURED_RUNS);
        System.out.println();
        System.out.println("--- Latency (ns/op) ---");
        System.out.println("Min:            " + String.format("%.0f", (double) min / ITERATIONS));
        System.out.println("Max:            " + String.format("%.0f", (double) max / ITERATIONS));
        System.out.println("Avg:            " + String.format("%.0f", (double) avg / ITERATIONS));
        System.out.println("Median:         " + String.format("%.0f", (double) median / ITERATIONS));
        System.out.println();
        System.out.println("--- Throughput (ops/sec) ---");
        System.out.println("Min:            " + String.format("%,.0f", ITERATIONS * 1_000_000_000.0 / max));
        System.out.println("Max:            " + String.format("%,.0f", ITERATIONS * 1_000_000_000.0 / min));
        System.out.println("Median:         " + String.format("%,.0f", ITERATIONS * 1_000_000_000.0 / median));
        System.out.println();
        System.out.println("--- Book State ---");
        System.out.println("Trades:         " + trades);
        System.out.println("Bid depth:      " + bidDepth);
        System.out.println("Ask depth:      " + askDepth);
    }

    private BenchmarkResult runSingleBenchmark(Instrument instrument, int seed) {
        Random random = new Random(42);
        OrderBook book = new OrderBook(instrument);

        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            double price = 100.0 + (random.nextInt(200) - 100) * 0.01;
            double quantity = 1 + random.nextInt(100);
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            orders.add(new Order(price, quantity, side, instrument.getScale()));
        }

        long startTime = System.nanoTime();

        for (Order order : orders) {
            book.addOrder(order);
        }

        long endTime = System.nanoTime();

        return new BenchmarkResult(
                endTime - startTime,
                book.getTradeCounts(),
                book.getDepth(Side.BUY),
                book.getDepth(Side.SELL)
        );
    }

    private record BenchmarkResult(
            long durationNs,
            int trades,
            int bidDepth,
            int askDepth
    ) {}
}