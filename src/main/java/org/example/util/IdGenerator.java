package org.example.util;

public class IdGenerator {
    private long counter = 0;

    public long next() {
        return ++counter;
    }
}