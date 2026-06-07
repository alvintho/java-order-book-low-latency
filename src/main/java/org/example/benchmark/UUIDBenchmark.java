//package org.example.benchmark;
//
//import org.openjdk.jmh.annotations.*;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@State(Scope.Thread)
//@Warmup(iterations = 5, time = 1)
//@Measurement(iterations = 10, time = 1)
//@Fork(2)
//public class UUIDBenchmark {
//
//    @Benchmark
//    public UUID baseline_randomUUID() {
//        return UUID.randomUUID();
//    }
//}