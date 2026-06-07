package org.example.model;

/**
 * FIX 4.4 Tag 54 (Side).
 * Enum values carry the FIX wire character but the core engine
 * never sees raw FIX tags — the adapter layer translates.
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
        switch (code) {
            case '1': return BUY;
            case '2': return SELL;
            default: throw new IllegalArgumentException("Unknown FIX Side code: " + code);
        }
    }
}