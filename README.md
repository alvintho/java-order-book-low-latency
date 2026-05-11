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
Benchmark               │ UUID+LinkedList │ Long+ArrayDeque │ +LazyTradeList  │ vs Baseline
────────────────────────┼─────────────────┼─────────────────┼─────────────────┼──────────────────
ingest100KOrders (ns)   │ 233.8 ± 6.2     │ 113.3 ± 0.7     │ 115.6 ± 0.7     │ 50.5% faster
ingest100KOrders (B/op) │ 716.0           │ 477.2           │ 453.5           │ -262.5 B/op (36.7%)
ingest100KNoMatch (ns)  │ 71.1 ± 3.1      │ 31.9 ± 0.1      │ 28.2 ± 1.0      │ 60.3% faster
ingest100KNoMatch (B/op)│ 485.1           │ 267.9           │ 243.9           │ -241.2 B/op (49.7%)
```
