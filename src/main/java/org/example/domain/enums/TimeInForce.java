package org.example.domain.enums;

/**
 * FIX 4.4 Tag 59 (TimeInForce).
 *
 * Bug fix: GTC was '1' (DAY in FIX 4.4). Correct FIX 4.4 codes:
 *   0 = Day, 1 = GTC, 3 = IOC, 4 = FOK
 *
 * NOTE: FIX 4.4 spec defines GTC as '1'. We follow the spec.
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
        return switch (code) {
            case '1' -> GTC;
            case '3' -> IOC;
            case '4' -> FOK;
            default  -> throw new IllegalArgumentException("Unknown FIX TimeInForce: " + code);
        };
    }
}