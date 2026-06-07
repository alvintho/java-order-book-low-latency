package org.example.util;

/**
 * Utility for converting between scaled-integer prices (long) and
 * double display prices.
 * --
 * Using long internally avoids floating-point accumulation errors
 * in price-level comparisons and TreeMap keys.
 * --
 * Examples (scale = 100):
 * --
 *   toLong(10.05, 100) → 1005L
 *   toDouble(1005L, 100) → 10.05
 *
 */
public final class Price {

    private Price() {}

    /**
     * Convert a display price to a scaled integer.
     *
     * @throws IllegalArgumentException if price is negative or has more decimal
     *                                  places than the scale supports
     */
    public static long toLong(double displayPrice, int scale) {
        if (displayPrice < 0) {
            throw new IllegalArgumentException(
                    "Price cannot be negative: " + displayPrice);
        }
        long scaled = Math.round(displayPrice * scale);
        // Verify no precision was lost beyond what scale allows
        double roundTrip = (double) scaled / scale;
        if (Math.abs(roundTrip - displayPrice) > 0.5 / scale) {
            throw new IllegalArgumentException(
                    "Price " + displayPrice + " exceeds scale precision of " + scale);
        }
        return scaled;
    }

    public static double toDouble(long scaledPrice, int scale) {
        return (double) scaledPrice / scale;
    }
}