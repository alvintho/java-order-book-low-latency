package org.example.benchmark;

import org.example.*;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class OrderBookBenchmark {

    private static final int BATCH_SIZE = 100_000;

    private Instrument instrument;

    // Pre-generated immutable specs
    private double[] prices;
    private double[] quantities;
    private Side[] sides;

    // Fresh per invocation
    private OrderBook orderBook;
    private Order[] orders;

    /*
    * Pre-create order attributes per trial
    * */

    @Setup(Level.Trial)
    public void trialSetup() {
        instrument = new Instrument("AAPL", 100, 1);

        prices = new double[BATCH_SIZE];
        quantities = new double[BATCH_SIZE];
        sides = new Side[BATCH_SIZE];

        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < BATCH_SIZE; i++) {
            prices[i] = 100.0 + (random.nextInt(200) - 100) * 0.01;
            quantities[i] = 1 + random.nextInt(100);
            sides[i] = random.nextBoolean() ? Side.BUY : Side.SELL;
        }
    }

    /*
    * Pre-create a set of 100K Order objects per invocation
    * Production systems ingests all 100k orders after orders are ready
    * */
    @Setup(Level.Invocation)
    public void invocationSetup() {
        orderBook = new OrderBook(instrument);
        orders = new Order[BATCH_SIZE];

        for (int i = 0; i < BATCH_SIZE; i++) {
            orders[i] = new Order(prices[i], quantities[i], sides[i], instrument.getScale());
        }
    }

    /*
    * Performance benchmark for the order book
    * to ingest 100k pre-created orders
    *
    * It is measuring:
    *   order book insertion
    *   matching logic
    *   queue interaction
    *   TreeMap access
    *   volume bookkeeping
    *   trade creation when matches occur
    *   returned trade list creation as part of addOrder
    * */
    /*
     * Dead Code Elimination (DCE) safe
     * The JIT cannot delete the addOrder calls because the final state of orderBook is returned
     */
    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public OrderBook ingest100KOrders() {

        for (Order order : orders) {
            orderBook.addOrder(order);
        }

        return orderBook;
    }
}