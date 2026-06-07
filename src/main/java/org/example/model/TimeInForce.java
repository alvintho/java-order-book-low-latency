package org.example.model;

/**
 * FIX 4.4 Tag 59 (TimeInForce).
 */
public enum TimeInForce {
    GTC('1'),
    IOC('3'),
    FOK('4');

    private final char fixCode;

    TimeInForce(char fixCode) {
        this.fixCode = fixCode;
    }

    public char getFixCode() {
        return fixCode;
    }

    public static TimeInForce fromFixCode(char code) {
        switch (code) {
            case '1': return GTC;
            case '3': return IOC;
            case '4': return FOK;
            default: throw new IllegalArgumentException("Unknown FIX TimeInForce code: " + code);
        }
    }
}