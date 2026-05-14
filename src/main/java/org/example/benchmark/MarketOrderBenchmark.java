package org.example.benchmark;

import org.example.engine.OrderBook;
import org.example.model.Instrument;
import org.example.model.Order;
import org.example.model.Side;
import org.example.util.IdGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class MarketOrderBenchmark {

    private static final int BATCH_SIZE = 10_000;

    private Instrument instrument;
    private double[] quantities;
    private Side[] sides;

    private OrderBook orderBook;
    private Order[] marketOrders;
    private IdGenerator orderIdGen;

    @Setup(Level.Trial)
    public void trialSetup() {
        instrument = new Instrument("AAPL", 100, 1);

        quantities = new double[BATCH_SIZE];
        sides      = new Side[BATCH_SIZE];

        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < BATCH_SIZE; i++) {
            quantities[i] = 1 + rng.nextInt(5);
            sides[i] = rng.nextBoolean() ? Side.BUY : Side.SELL;
        }
    }

    @Setup(Level.Invocation)
    public void invocationSetup() {
        orderIdGen = new IdGenerator();
        orderBook = new OrderBook(instrument);

        // Seed deep liquidity — ONLY setup cost for this benchmark
        for (int level = 0; level < 50; level++) {
            double bidPrice = 99.0 - level * 0.01;
            double askPrice = 101.0 + level * 0.01;
            for (int j = 0; j < 100; j++) {
                orderBook.addOrder(Order.limitBuy(
                        orderIdGen.next(), bidPrice,
                        50.0, instrument.getScale()));
                orderBook.addOrder(Order.limitSell(
                        orderIdGen.next(), askPrice,
                        50.0, instrument.getScale()));
            }
        }

        // Create market orders — ONLY these, nothing else
        marketOrders = new Order[BATCH_SIZE];
        for (int i = 0; i < BATCH_SIZE; i++) {
            marketOrders[i] = sides[i] == Side.BUY
                    ? Order.marketBuy(orderIdGen.next(), quantities[i])
                    : Order.marketSell(orderIdGen.next(), quantities[i]);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void ingest10KMarketOrders(Blackhole bh) {
        for (Order order : marketOrders) {
            bh.consume(orderBook.addOrder(order));
        }
        bh.consume(orderBook);
    }
}