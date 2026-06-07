package org.example.engine;

import org.example.domain.enums.OrdStatus;
import org.example.domain.enums.OrderType;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.domain.model.Instrument;
import org.example.domain.model.Order;
import org.example.domain.model.Trade;
import org.example.domain.port.BaseOrderBook;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeInForceTest {

    private BaseOrderBook  book;
    private IdGenerator idGen;
    private int        scale;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        scale = instrument.getScale();
        book  = new PriceTimePriorityOrderBook(instrument, new IdGenerator(), r -> {});
        idGen = new IdGenerator();
    }

    private Order iocBuy(double price, double qty) {
        return new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, qty)
                .price(org.example.util.Price.toLong(price, scale))
                .timeInForce(TimeInForce.IOC)
                .build();
    }

    private Order fokBuy(double price, double qty) {
        return new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, qty)
                .price(org.example.util.Price.toLong(price, scale))
                .timeInForce(TimeInForce.FOK)
                .build();
    }

    // ── IOC ───────────────────────────────────────────────────────────────────

    @Test
    void iocShouldFillCompletelyWhenLiquidityAvailable() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        Order ioc = iocBuy(100.0, 10.0);
        List<Trade> trades = book.addOrder(ioc);

        assertEquals(1,    trades.size());
        assertEquals(10.0, trades.getFirst().getQuantity(), 1e-12);
        assertTrue(ioc.isFilled());
        assertEquals(OrdStatus.FILLED, ioc.getOrdStatus());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void iocShouldPartialFillAndCancelRemainder() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));

        Order ioc = iocBuy(100.0, 10.0);
        List<Trade> trades = book.addOrder(ioc);

        assertEquals(1,   trades.size());
        assertEquals(5.0, trades.getFirst().getQuantity(), 1e-12);
        assertNull(book.getOrder(ioc.getOrderId()), "IOC remainder must not rest in book");
        assertEquals(OrdStatus.CANCELED, ioc.getOrdStatus());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void iocShouldCancelImmediatelyWhenNoLiquidity() {
        Order ioc = iocBuy(100.0, 10.0);
        List<Trade> trades = book.addOrder(ioc);

        assertTrue(trades.isEmpty());
        assertNull(book.getOrder(ioc.getOrderId()));
        assertEquals(OrdStatus.CANCELED, ioc.getOrdStatus());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    // ── FOK ───────────────────────────────────────────────────────────────────

    @Test
    void fokShouldFillCompletelyWhenFullLiquidityAvailable() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        Order fok = fokBuy(100.0, 10.0);
        List<Trade> trades = book.addOrder(fok);

        assertEquals(1,    trades.size());
        assertEquals(10.0, trades.getFirst().getQuantity(), 1e-12);
        assertTrue(fok.isFilled());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void fokShouldRejectWhenInsufficientLiquidity() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));

        Order fok = fokBuy(100.0, 10.0);
        List<Trade> trades = book.addOrder(fok);

        assertTrue(trades.isEmpty());
        assertEquals(OrdStatus.CANCELED, fok.getOrdStatus());
        assertEquals(5.0, book.getTotalAskVolume(), 1e-12,
                "Resting sell must be untouched after FOK rejection");
    }

    @Test
    void fokShouldRejectWhenNoLiquidity() {
        Order fok = fokBuy(100.0, 10.0);
        List<Trade> trades = book.addOrder(fok);

        assertTrue(trades.isEmpty());
        assertEquals(OrdStatus.CANCELED, fok.getOrdStatus());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    @Test
    void fokShouldFillAcrossMultiplePriceLevels() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));
        book.addOrder(Order.limitSell(idGen.next(), 101.0, 5.0, scale));

        Order fok = fokBuy(101.0, 10.0);
        List<Trade> trades = book.addOrder(fok);

        assertEquals(2, trades.size());
        assertTrue(fok.isFilled());
        assertEquals(0.0, book.getTotalVolume(), 1e-12);
    }

    // ── GTC (default) ─────────────────────────────────────────────────────────

    @Test
    void gtcShouldRestInBookWhenNotFilled() {
        Order gtc = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        List<Trade> trades = book.addOrder(gtc);

        assertTrue(trades.isEmpty());
        assertNotNull(book.getOrder(gtc.getOrderId()));
        assertEquals(10.0, book.getTotalBidVolume(), 1e-12);
    }

    @Test
    void gtcShouldRemainAfterPartialFill() {
        book.addOrder(Order.limitSell(idGen.next(), 100.0, 3.0, scale));

        Order gtc = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        book.addOrder(gtc);

        assertNotNull(book.getOrder(gtc.getOrderId()),
                "Partially filled GTC must remain in book");
        assertEquals(7.0, gtc.getLeavesQty(), 1e-12);
        assertEquals(7.0, book.getTotalBidVolume(), 1e-12);
    }
}