package org.example.model;

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
        switch (code) {
            case '0': return NEW;
            case '1': return PARTIALLY_FILLED;
            case '2': return FILLED;
            case '4': return CANCELED;
            case '8': return REJECTED;
            default: throw new IllegalArgumentException("Unknown FIX OrdStatus code: " + code);
        }
    }
}