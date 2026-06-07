package org.example.domain.model;

import org.example.domain.enums.ExecType;
import org.example.domain.enums.OrdStatus;
import org.example.domain.enums.OrderType;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.util.Price;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExecutionReport factory methods.
 *
 * ExecutionReport is the output contract of the matching engine —
 * wrong ExecType or OrdStatus values would corrupt downstream
 * FIX sessions, risk systems, and OMS state.
 *
 * Each test covers one factory and one specific risk from the table above.
 */
class ExecutionReportTest {

    private static final int    SCALE  = 100;
    private static final String SYMBOL = "AAPL";
    private static final long   PRICE  = Price.toLong(100.00, SCALE); // 10000L

    private long nextId = 1L;

    private long id() { return nextId++; }

    private Order freshLimitBuy(double qty) {
        return Order.limitBuy(id(), 100.00, qty, SCALE);
    }

    // ── ack() ─────────────────────────────────────────────────────────────────

    @Test
    void ackShouldHaveExecTypeNew() {
        Order order = freshLimitBuy(10.0);
        ExecutionReport report = ExecutionReport.ack(order);

        assertEquals(ExecType.NEW, report.getExecType());
    }

    @Test
    void ackShouldHaveOrdStatusNew() {
        Order order = freshLimitBuy(10.0);
        ExecutionReport report = ExecutionReport.ack(order);

        assertEquals(OrdStatus.NEW, report.getOrdStatus());
    }

    @Test
    void ackShouldHaveZeroLastQtyAndLastPx() {
        Order order = freshLimitBuy(10.0);
        ExecutionReport report = ExecutionReport.ack(order);

        assertEquals(0.0, report.getLastQty(), 1e-12,
                "ACK is not a fill — lastQty must be zero");
        assertEquals(0L,  report.getLastPx(),
                "ACK is not a fill — lastPx must be zero");
    }

    @Test
    void ackShouldSnapshotOrderFields() {
        Order order = new Order.Builder(id(), Side.BUY, OrderType.LIMIT, 10.0)
                .clOrdId("ORD-001")
                .symbol(SYMBOL)
                .price(PRICE)
                .timeInForce(TimeInForce.GTC)
                .build();

        ExecutionReport report = ExecutionReport.ack(order);

        assertEquals("ORD-001",        report.getClOrdId());
        assertEquals(order.getOrderId(), report.getOrderId());
        assertEquals(SYMBOL,           report.getSymbol());
        assertEquals(Side.BUY,         report.getSide());
        assertEquals(10.0,             report.getLeavesQty(), 1e-12);
        assertEquals(0.0,              report.getCumQty(),    1e-12);
        assertEquals(0.0,              report.getAvgPx(),     1e-12);
    }

    @Test
    void ackShouldAlwaysReportNewEvenIfCalledOnPartiallyFilledOrder() {
        // Edge case: ack() is always called immediately after insertIntoBook,
        // before any matching. But if someone calls it after a fill, it should
        // still set ExecType=NEW and OrdStatus=NEW as documented.
        Order order = freshLimitBuy(10.0);
        order.applyFill(5.0, PRICE); // partial fill first

        ExecutionReport report = ExecutionReport.ack(order);

        assertEquals(ExecType.NEW,  report.getExecType(),
                "ack() always produces ExecType=NEW regardless of order state");
        assertEquals(OrdStatus.NEW, report.getOrdStatus(),
                "ack() always produces OrdStatus=NEW regardless of order state");
    }

    // ── fill() — partial ──────────────────────────────────────────────────────

    @Test
    void fillShouldHaveExecTypePartialFillWhenOrderNotFullyFilled() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(5.0, PRICE); // 5 filled, 5 remain → PARTIALLY_FILLED

        ExecutionReport report = ExecutionReport.fill(order, 5.0, PRICE);

        assertEquals(ExecType.PARTIAL_FILL, report.getExecType());
    }

    @Test
    void fillShouldHaveOrdStatusPartiallyFilledWhenOrderNotFullyFilled() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(5.0, PRICE);

        ExecutionReport report = ExecutionReport.fill(order, 5.0, PRICE);

        assertEquals(OrdStatus.PARTIALLY_FILLED, report.getOrdStatus());
    }

    @Test
    void fillShouldCaptureLastQtyAndLastPx() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(5.0, PRICE);

        ExecutionReport report = ExecutionReport.fill(order, 5.0, PRICE);

        assertEquals(5.0,  report.getLastQty(), 1e-12);
        assertEquals(PRICE, report.getLastPx());
    }

    @Test
    void fillShouldSnapshotLeavesQtyAndCumQtyAtMomentOfCall() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(5.0, PRICE);

        ExecutionReport report = ExecutionReport.fill(order, 5.0, PRICE);

        assertEquals(5.0, report.getLeavesQty(), 1e-12,
                "leavesQty must reflect post-fill state");
        assertEquals(5.0, report.getCumQty(), 1e-12,
                "cumQty must reflect post-fill state");
    }

    // ── fill() — full ─────────────────────────────────────────────────────────

    @Test
    void fillShouldHaveExecTypeFillWhenOrderFullyFilled() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(10.0, PRICE); // fully filled

        ExecutionReport report = ExecutionReport.fill(order, 10.0, PRICE);

        assertEquals(ExecType.FILL, report.getExecType(),
                "A fully filled order must produce ExecType=FILL not PARTIAL_FILL");
    }

    @Test
    void fillShouldHaveOrdStatusFilledWhenOrderFullyFilled() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(10.0, PRICE);

        ExecutionReport report = ExecutionReport.fill(order, 10.0, PRICE);

        assertEquals(OrdStatus.FILLED, report.getOrdStatus());
    }

    @Test
    void fillShouldHaveZeroLeavesQtyOnFullFill() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(10.0, PRICE);

        ExecutionReport report = ExecutionReport.fill(order, 10.0, PRICE);

        assertEquals(0.0,  report.getLeavesQty(), 1e-12);
        assertEquals(10.0, report.getCumQty(),    1e-12);
    }

    @Test
    void fillAvgPxShouldMatchOrderAvgPxSnapshot() {
        Order order = freshLimitBuy(10.0);
        long px1 = Price.toLong(100.00, SCALE);
        long px2 = Price.toLong(101.00, SCALE);
        order.applyFill(5.0, px1);
        order.applyFill(5.0, px2);

        // avgPx = (5*10000 + 5*10100) / 10 = 10050
        ExecutionReport report = ExecutionReport.fill(order, 5.0, px2);

        double expected = (5.0 * px1 + 5.0 * px2) / 10.0;
        assertEquals(expected, report.getAvgPx(), 1e-6,
                "avgPx must be a snapshot of the order's VWAP at report time");
    }

    // ── canceled() ───────────────────────────────────────────────────────────

    @Test
    void canceledShouldHaveExecTypeCanceled() {
        Order order = freshLimitBuy(10.0);
        order.cancel();

        ExecutionReport report = ExecutionReport.canceled(order);

        assertEquals(ExecType.CANCELED, report.getExecType());
    }

    @Test
    void canceledShouldAlwaysHaveOrdStatusCanceled() {
        Order order = freshLimitBuy(10.0);
        order.cancel();

        ExecutionReport report = ExecutionReport.canceled(order);

        assertEquals(OrdStatus.CANCELED, report.getOrdStatus(),
                "canceled() must always report CANCELED regardless of order state");
    }

    @Test
    void canceledShouldHaveZeroLastQtyAndLastPx() {
        Order order = freshLimitBuy(10.0);
        order.cancel();

        ExecutionReport report = ExecutionReport.canceled(order);

        assertEquals(0.0, report.getLastQty(), 1e-12,
                "A cancel is not a fill — lastQty must be zero");
        assertEquals(0L,  report.getLastPx(),
                "A cancel is not a fill — lastPx must be zero");
    }

    @Test
    void canceledShouldSnapshotRemainingLeavesQty() {
        Order order = freshLimitBuy(10.0);
        order.applyFill(3.0, PRICE); // partial fill before cancel
        order.cancel();

        ExecutionReport report = ExecutionReport.canceled(order);

        assertEquals(7.0, report.getLeavesQty(), 1e-12,
                "leavesQty must reflect how much was still open at cancel time");
        assertEquals(3.0, report.getCumQty(), 1e-12);
    }

    // ── replaced() ───────────────────────────────────────────────────────────

    @Test
    void replacedShouldHaveExecTypeReplaced() {
        Order replacement = freshLimitBuy(8.0);

        ExecutionReport report = ExecutionReport.replaced(replacement);

        assertEquals(ExecType.REPLACED, report.getExecType());
    }

    @Test
    void replacedShouldReflectReplacementOrderState() {
        Order replacement = freshLimitBuy(8.0);

        ExecutionReport report = ExecutionReport.replaced(replacement);

        assertEquals(replacement.getOrderId(), report.getOrderId());
        assertEquals(8.0, report.getLeavesQty(), 1e-12);
        assertEquals(0.0, report.getCumQty(),    1e-12);
    }

    @Test
    void replacedShouldHaveZeroLastQtyAndLastPx() {
        Order replacement = freshLimitBuy(8.0);

        ExecutionReport report = ExecutionReport.replaced(replacement);

        assertEquals(0.0, report.getLastQty(), 1e-12,
                "A replace ack is not a fill — lastQty must be zero");
        assertEquals(0L,  report.getLastPx(),
                "A replace ack is not a fill — lastPx must be zero");
    }

    // ── rejected() ───────────────────────────────────────────────────────────

    @Test
    void rejectedShouldHaveExecTypeRejected() {
        Order fok = freshLimitBuy(10.0);
        fok.cancel(); // FOK sets CANCELED before rejected() is called

        ExecutionReport report = ExecutionReport.rejected(fok);

        assertEquals(ExecType.REJECTED, report.getExecType());
    }

    @Test
    void rejectedShouldHaveOrdStatusRejected() {
        Order fok = freshLimitBuy(10.0);
        fok.cancel();

        ExecutionReport report = ExecutionReport.rejected(fok);

        assertEquals(OrdStatus.REJECTED, report.getOrdStatus(),
                "rejected() must always set OrdStatus=REJECTED");
    }

    @Test
    void rejectedShouldHaveZeroLastQtyAndLastPx() {
        Order fok = freshLimitBuy(10.0);
        fok.cancel();

        ExecutionReport report = ExecutionReport.rejected(fok);

        assertEquals(0.0, report.getLastQty(), 1e-12);
        assertEquals(0L,  report.getLastPx());
    }

    // ── Immutability / snapshot isolation ────────────────────────────────────

    @Test
    void reportShouldNotReflectOrderMutationsAfterCreation() {
        Order order = freshLimitBuy(10.0);
        ExecutionReport report = ExecutionReport.ack(order);

        // Mutate the order after the report was created
        order.applyFill(10.0, PRICE);

        // Report must still reflect the state at the moment ack() was called
        assertEquals(10.0, report.getLeavesQty(), 1e-12,
                "Report is an immutable snapshot — mutations after creation must not affect it");
        assertEquals(0.0, report.getCumQty(), 1e-12);
        assertEquals(ExecType.NEW, report.getExecType());
    }

    @Test
    void twoReportsFromSameOrderShouldBeIndependent() {
        Order order = freshLimitBuy(10.0);

        ExecutionReport ack = ExecutionReport.ack(order);
        order.applyFill(5.0, PRICE);
        ExecutionReport partialFill = ExecutionReport.fill(order, 5.0, PRICE);

        // ack snapshot: leavesQty=10, cumQty=0
        assertEquals(10.0, ack.getLeavesQty(), 1e-12);
        assertEquals(0.0,  ack.getCumQty(),    1e-12);

        // partialFill snapshot: leavesQty=5, cumQty=5
        assertEquals(5.0, partialFill.getLeavesQty(), 1e-12);
        assertEquals(5.0, partialFill.getCumQty(),    1e-12);
    }

    // ── Timestamp ────────────────────────────────────────────────────────────

    @Test
    void reportShouldHaveNonNullTimestamp() {
        ExecutionReport report = ExecutionReport.ack(freshLimitBuy(10.0));
        assertNotNull(report.getTimestamp());
    }
}