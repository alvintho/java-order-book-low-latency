package org.example;

public class IdGenerator {
    private long counter = 0;

    public long next() {
        return ++counter;
    }
}