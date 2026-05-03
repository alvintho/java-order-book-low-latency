package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InstrumentTest {

    @Test
    void shouldCreateEquityInstrument() {
        Instrument instrument = new Instrument("AAPL", 100, 1);

        assertEquals("AAPL", instrument.getSymbol());
        assertEquals(100, instrument.getScale());
        assertEquals(1, instrument.getLotSize());
    }

    @Test
    void shouldCreateForexInstrument() {
        Instrument instrument = new Instrument("EUR/USD", 100_000, 1000);

        assertEquals("EUR/USD", instrument.getSymbol());
        assertEquals(100_000, instrument.getScale());
        assertEquals(1000, instrument.getLotSize());
    }

    @Test
    void shouldRejectInvalidSymbol() {
        assertThrows(IllegalArgumentException.class, () -> new Instrument(null, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> new Instrument("", 100, 1));
    }

    @Test
    void shouldRejectInvalidScale() {
        assertThrows(IllegalArgumentException.class, () -> new Instrument("AAPL", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Instrument("AAPL", -1, 1));
    }

    @Test
    void shouldRejectInvalidLotSize() {
        assertThrows(IllegalArgumentException.class, () -> new Instrument("AAPL", 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new Instrument("AAPL", 100, -1));
    }
}