package org.example.benchmark;

import org.example.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class LimitOrderBenchmark {

    private static final int BATCH_SIZE = 100_000;
    private IdGenerator orderIdGen;

    private Instrument instrument;
    private double[] prices;
    private double[] noMatchBidPrices;  // all bids well below any ask
    private double[] noMatchAskPrices;  // all asks well above any bid
    private double[] quantities;
    private Side[] sides;


    private OrderBook orderBook;
    private Order[] orders;
    private Order[] noMatchOrders;

    /*
    * Pre-create order attributes per trial
    * */
    @Setup(Level.Trial)
    public void trialSetup() {
        instrument = new Instrument("AAPL", 100, 1);

        prices      = new double[BATCH_SIZE];
        quantities  = new double[BATCH_SIZE];
        sides       = new Side[BATCH_SIZE];
        noMatchBidPrices = new double[BATCH_SIZE];
        noMatchAskPrices = new double[BATCH_SIZE];

        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < BATCH_SIZE; i++) {
            prices[i] = 100.0 + (random.nextInt(200) - 100) * 0.01;
            quantities[i] = 1 + random.nextInt(100);
            sides[i] = random.nextBoolean() ? Side.BUY : Side.SELL;
        }

        // Bids at 50-99, asks at 150-199 → guaranteed no crossing
        for (int i = 0; i < BATCH_SIZE; i++) {
            noMatchBidPrices[i] = 50.0 + (i % 50) * 0.01;   // 50.00 – 50.49
            noMatchAskPrices[i] = 150.0 + (i % 50) * 0.01;  // 150.00 – 150.49
        }
    }

    /*
    * Pre-create a set of 100K Order objects per invocation
    * Production systems ingests all 100k orders after orders are ready
    * */
    @Setup(Level.Invocation)
    public void invocationSetup() {
        orderBook = new OrderBook(instrument);
        orderIdGen = new IdGenerator();
        orders = new Order[BATCH_SIZE];
        noMatchOrders = new Order[BATCH_SIZE];

        for (int i = 0; i < BATCH_SIZE; i++) {
            orders[i] = sides[i] == Side.BUY
                    ? Order.limitBuy(
                    orderIdGen.next(), prices[i],
                    quantities[i], instrument.getScale())
                    : Order.limitSell(
                    orderIdGen.next(), prices[i],
                    quantities[i], instrument.getScale());
        }

        for (int i = 0; i < BATCH_SIZE; i++) {
            noMatchOrders[i] = (i % 2 == 0)
                    ? Order.limitBuy(
                    orderIdGen.next(), noMatchBidPrices[i / 2 % 50],
                    10.0, instrument.getScale())
                    : Order.limitSell(
                    orderIdGen.next(), noMatchAskPrices[i / 2 % 50],
                    10.0, instrument.getScale());
        }
    }

    /**
     * Measures: insertion + matching + trade creation + volume bookkeeping.
     * The JIT cannot delete the addOrder calls because returned trades are consumed
     */
    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void ingest100KOrders(Blackhole bh) {
        for (Order order : orders) {
            List<Trade> trades = orderBook.addOrder(order);
            bh.consume(trades);
        }
        bh.consume(orderBook);
    }

    // Measures: pure insertion cost (TreeMap + HashMap)
    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void ingest100KNoMatchOrders(Blackhole bh) {
        for (Order order : noMatchOrders) {
            List<Trade> trades = orderBook.addOrder(order);
            bh.consume(trades);
        }
        bh.consume(orderBook);
    }

}