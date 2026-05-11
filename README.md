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

# Latency Optimization Performance Improvements
```aiignore
Benchmark               │ UUID+LinkedList │ Long+ArrayDeque │ +EmptyList      │ Total Improvement
────────────────────────┼─────────────────┼─────────────────┼─────────────────┼──────────────────
ingest100KOrders (ns)   │ 233.8 ± 6.2     │ 113.3 ± 0.7     │ 115.62 ± 0.67   │ ~50.5% faster
ingest100KNoMatch (ns)  │ N/A             │  31.9 ± 0.1     │  27.0 ± 0.3     │ ~18% faster ✓
NoMatch alloc (B/op)    │ N/A             │ 267.9           │ 243.9           │ -24 B/op ✓
Orders alloc (B/op)     │ N/A             │ 477.2           │ 453.5           │ -24 B/op ✓
```
