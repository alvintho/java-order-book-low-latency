package org.example;

public class Price {

    public static long toLong(double price, int scale) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        long converted = Math.round(price * scale);
        double reconstructed = (double) converted / scale;

        if (Math.abs(reconstructed - price) > 1e-9) {
            throw new IllegalArgumentException(
                    "Price " + price + " exceeds scale precision of " + scale
            );
        }

        return converted;
    }

    public static double toDouble(long price, int scale) {
        return (double) price / scale;
    }

    private Price() {}
}