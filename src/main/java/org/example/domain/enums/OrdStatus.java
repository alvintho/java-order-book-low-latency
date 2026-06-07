package org.example.domain.enums;

/**
 * FIX 4.4 Tag 39 (OrdStatus).
 */
public enum OrdStatus {
    NEW('0'),
    PARTIALLY_FILLED('1'),
    FILLED('2'),
    CANCELED('4'),
    REJECTED('8');

    private final char fixCode;

    OrdStatus(char fixCode) {
        this.fixCode = fixCode;
    }

    public char getFixCode() {
        return fixCode;
    }

    public static OrdStatus fromFixCode(char code) {
        return switch (code) {
            case '0' -> NEW;
            case '1' -> PARTIALLY_FILLED;
            case '2' -> FILLED;
            case '4' -> CANCELED;
            case '8' -> REJECTED;
            default  -> throw new IllegalArgumentException("Unknown FIX OrdStatus: " + code);
        };
    }
}