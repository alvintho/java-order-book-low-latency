package org.example.engine;

import org.example.model.*;
import org.example.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimeInForceTest {
    private OrderBook orderBook;
    private IdGenerator idGen;
    private int scale;

    @BeforeEach
    void setUp() {
        Instrument instrument = new Instrument("AAPL", 100, 1);
        scale = instrument.getScale();
        orderBook = new OrderBook(instrument);
        idGen = new IdGenerator();
    }

    // ── IOC tests ──

    @Test
    void iocShouldFillCompletelyWhenLiquidityAvailable() {
        orderBook.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        Order iocBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10000L)
                .timeInForce(TimeInForce.IOC)
                .build();
        List<Trade> trades = orderBook.addOrder(iocBuy);

        assertEquals(1, trades.size());
        assertEquals(10.0, trades.getFirst().getQuantity());
        assertTrue(iocBuy.isFilled());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    @Test
    void iocShouldPartialFillAndCancelRemainder() {
        orderBook.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));

        Order iocBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10000L)
                .timeInForce(TimeInForce.IOC)
                .build();
        List<Trade> trades = orderBook.addOrder(iocBuy);

        assertEquals(1, trades.size());
        assertEquals(5.0, trades.getFirst().getQuantity());
        // Remainder cancelled — not resting in book
        assertNull(orderBook.getOrder(iocBuy.getOrderId()));
        assertEquals(OrdStatus.CANCELED, iocBuy.getOrdStatus());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    @Test
    void iocShouldCancelImmediatelyWhenNoLiquidity() {
        Order iocBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10000L)
                .timeInForce(TimeInForce.IOC)
                .build();
        List<Trade> trades = orderBook.addOrder(iocBuy);

        assertTrue(trades.isEmpty());
        assertNull(orderBook.getOrder(iocBuy.getOrderId()));
        assertEquals(OrdStatus.CANCELED, iocBuy.getOrdStatus());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    // ── FOK tests ──

    @Test
    void fokShouldFillCompletelyWhenFullLiquidityAvailable() {
        orderBook.addOrder(Order.limitSell(idGen.next(), 100.0, 10.0, scale));

        Order fokBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10000L)
                .timeInForce(TimeInForce.FOK)
                .build();
        List<Trade> trades = orderBook.addOrder(fokBuy);

        assertEquals(1, trades.size());
        assertEquals(10.0, trades.getFirst().getQuantity());
        assertTrue(fokBuy.isFilled());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    @Test
    void fokShouldRejectWhenInsufficientLiquidity() {
        orderBook.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));

        Order fokBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10000L)
                .timeInForce(TimeInForce.FOK)
                .build();
        List<Trade> trades = orderBook.addOrder(fokBuy);

        assertTrue(trades.isEmpty());
        assertEquals(OrdStatus.CANCELED, fokBuy.getOrdStatus());
        // Original sell should still be resting
        assertEquals(5.0, orderBook.getTotalAskVolume());
    }

    @Test
    void fokShouldRejectWhenNoLiquidity() {
        Order fokBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10000L)
                .timeInForce(TimeInForce.FOK)
                .build();
        List<Trade> trades = orderBook.addOrder(fokBuy);

        assertTrue(trades.isEmpty());
        assertEquals(OrdStatus.CANCELED, fokBuy.getOrdStatus());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    @Test
    void fokShouldFillAcrossMultiplePriceLevels() {
        orderBook.addOrder(Order.limitSell(idGen.next(), 100.0, 5.0, scale));
        orderBook.addOrder(Order.limitSell(idGen.next(), 101.0, 5.0, scale));

        Order fokBuy = new Order.Builder(idGen.next(), Side.BUY, OrderType.LIMIT, 10.0)
                .price(10100L) // willing to pay up to 101
                .timeInForce(TimeInForce.FOK)
                .build();
        List<Trade> trades = orderBook.addOrder(fokBuy);

        assertEquals(2, trades.size());
        assertTrue(fokBuy.isFilled());
        assertEquals(0.0, orderBook.getTotalVolume());
    }

    // ── GTC (default) ──

    @Test
    void gtcShouldRestWhenNotFilled() {
        Order gtcBuy = Order.limitBuy(idGen.next(), 100.0, 10.0, scale);
        List<Trade> trades = orderBook.addOrder(gtcBuy);

        assertTrue(trades.isEmpty());
        assertNotNull(orderBook.getOrder(gtcBuy.getOrderId()));
        assertEquals(10.0, orderBook.getTotalBidVolume());
    }
}
