# hft-low-latency-order-book

# Benchmark Testing
```aiignore
mvn clean package
java -jar target/limit_order_book_engine-1.0-SNAPSHOT.jar OrderBookBenchmark
```

## With Profilers

```aiignore
mvn clean package
java -jar target/limit_order_book_engine-1.0-SNAPSHOT.jar UUIDBenchmark -prof gc
```

# Latency & Memory Performance Optimizations

```aiignore
                             (Baseline)
Benchmark                │ UUID+LinkedList │ Long+ArrayDeque │ +LazyTradeList  │ +MarketOrders   │ vs Baseline
─────────────────────────┼─────────────────┼─────────────────┼─────────────────┼─────────────────┼──────────────────
ingest100KOrders (ns/op) │ 233.8 ± 6.2     │ 113.3 ± 0.7     │ 115.6 ± 0.7     │ 113.5 ± 0.3     │ 51.5% faster
ingest100KOrders (B/op)  │ 716.0           │ 477.2           │ 453.5           │ 469.5           │ -246.5 B/op (34.4%)
ingest100KNoMatch (ns/op)│ 71.1 ± 3.1      │  31.9 ± 0.1     │  28.2 ± 1.0     │  26.9 ± 0.3     │ 62.2% faster
ingest100KNoMatch (B/op) │ 485.1           │ 267.9           │ 243.9           │ 259.9           │ -225.2 B/op (46.4%)
```