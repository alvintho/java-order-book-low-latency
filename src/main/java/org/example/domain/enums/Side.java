package org.example.domain.enums;

/**
 * FIX 4.4 Tag 54 (Side).
 */
public enum Side {
    BUY('1'),
    SELL('2');

    private final char fixCode;

    Side(char fixCode) {
        this.fixCode = fixCode;
    }

    public char getFixCode() {
        return fixCode;
    }

    public static Side fromFixCode(char code) {
        return switch (code) {
            case '1' -> BUY;
            case '2' -> SELL;
            default  -> throw new IllegalArgumentException("Unknown FIX Side: " + code);
        };
    }
}