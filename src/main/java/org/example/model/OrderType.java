package org.example.model;

/**
 * FIX 4.4 Tag 40 (OrdType).
 */
public enum OrderType {
    MARKET('1'),
    LIMIT('2');

    private final char fixCode;

    OrderType(char fixCode) {
        this.fixCode = fixCode;
    }

    public char getFixCode() {
        return fixCode;
    }

    public static OrderType fromFixCode(char code) {
        switch (code) {
            case '1': return MARKET;
            case '2': return LIMIT;
            default: throw new IllegalArgumentException("Unknown FIX OrdType code: " + code);
        }
    }
}