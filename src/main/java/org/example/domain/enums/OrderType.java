package org.example.domain.enums;

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
        return switch (code) {
            case '1' -> MARKET;
            case '2' -> LIMIT;
            default  -> throw new IllegalArgumentException("Unknown FIX OrdType: " + code);
        };
    }
}