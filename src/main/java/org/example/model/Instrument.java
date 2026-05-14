package org.example.model;

/*
* Instrument defines:
  - Symbol (identifier)
  - Scale (price precision)
  - Lot size (minimum quantity increment)

Order Book just matches orders.
* */

public class Instrument {
    private final String symbol;
    private final int scale;
    private final int lotSize;

    /*
    *  Asset-class agnostic
    *  Equities:   Instrument("AAPL", scale=100)         → tick 0.01
    *  Forex:      Instrument("EUR/USD", scale=100_000)   → tick 0.00001
    *  Crypto:     Instrument("BTC/USD", scale=100)       → tick 0.01
    *  Futures:    Instrument("ES", scale=4)              → tick 0.25
    * */

    public Instrument(String symbol, int scale, int lotSize) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be positive");
        }
        if (lotSize <= 0) {
            throw new IllegalArgumentException("Lot size must be positive");
        }
        this.symbol = symbol;
        this.scale = scale;
        this.lotSize = lotSize;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getScale() {
        return scale;
    }

    public int getLotSize() {
        return lotSize;
    }
}