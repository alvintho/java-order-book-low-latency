package org.example.domain.model;

/**
 * Describes a tradeable instrument: symbol, price scale, and lot size.
 *
 * <p>Scale examples:
 * <ul>
 *   <li>Equities (AAPL): scale=100 → tick 0.01</li>
 *   <li>Forex (EUR/USD): scale=100_000 → tick 0.00001</li>
 *   <li>Crypto (BTC/USD): scale=100 → tick 0.01</li>
 * </ul>
 */
public final class Instrument {

    private final String symbol;
    private final int    scale;
    private final int    lotSize;

    public Instrument(String symbol, int scale, int lotSize) {
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        if (scale   <= 0) throw new IllegalArgumentException("Scale must be positive");
        if (lotSize <= 0) throw new IllegalArgumentException("Lot size must be positive");

        this.symbol  = symbol;
        this.scale   = scale;
        this.lotSize = lotSize;
    }

    public String getSymbol()  { return symbol;  }
    public int    getScale()   { return scale;   }
    public int    getLotSize() { return lotSize; }
}