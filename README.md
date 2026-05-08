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
