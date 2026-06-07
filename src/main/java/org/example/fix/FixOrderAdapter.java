package org.example.fix;

import org.example.domain.model.Order;
import org.example.domain.enums.OrderType;
import org.example.domain.enums.Side;
import org.example.domain.enums.TimeInForce;
import org.example.domain.model.*;
import org.example.util.Price;

import java.util.Map;

/**
 * Stateless adapter: translates a FIX 4.4 tag-value map
 * (NewOrderSingle, MsgType D) into a domain {@link Order}.
 *
 * <p>Pattern: Adapter — converts FIX wire format to domain model.
 * The engine never sees raw FIX tag integers.
 *
 * <p>Thread-safety: stateless — safe for concurrent use.
 */
public final class FixOrderAdapter {

    private static final int TAG_CL_ORD_ID    = 11;
    private static final int TAG_SYMBOL        = 55;
    private static final int TAG_SIDE          = 54;
    private static final int TAG_ORD_TYPE      = 40;
    private static final int TAG_ORDER_QTY     = 38;
    private static final int TAG_PRICE         = 44;
    private static final int TAG_TIME_IN_FORCE = 59;

    private static final long NO_FIXED_ID = -1L;

    private final int  priceScale;
    private final long fixedOrderId;

    /**
     * Adapter where the caller supplies orderId at parse time.
     *
     * @param priceScale instrument price scale (e.g. 100 for 2 d.p.)
     */
    public FixOrderAdapter(int priceScale) {
        this(NO_FIXED_ID, priceScale);
    }

    /**
     * Adapter where orderId is fixed at construction.
     * Useful when the engine assigns IDs before parsing.
     *
     * @param orderId    engine-assigned internal order ID
     * @param priceScale instrument price scale
     */
    public FixOrderAdapter(long orderId, int priceScale) {
        if (priceScale <= 0)
            throw new IllegalArgumentException("priceScale must be positive");
        this.priceScale   = priceScale;
        this.fixedOrderId = orderId;
    }

    /**
     * Parse a FIX NewOrderSingle using the orderId baked into this adapter.
     *
     * @throws IllegalStateException if this adapter was not constructed with a fixed orderId
     */
    public Order fromNewOrderSingle(Map<Integer, String> fields) {
        if (fixedOrderId == NO_FIXED_ID) {
            throw new IllegalStateException(
                    "No fixed orderId — use fromNewOrderSingle(long, Map) instead");
        }
        return parse(fixedOrderId, fields);
    }

    /**
     * Parse a FIX NewOrderSingle using a caller-supplied orderId.
     */
    public Order fromNewOrderSingle(long orderId, Map<Integer, String> fields) {
        return parse(orderId, fields);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Order parse(long orderId, Map<Integer, String> fields) {
        String    clOrdId  = require(fields, TAG_CL_ORD_ID);
        String    symbol   = require(fields, TAG_SYMBOL);
        Side      side     = Side.fromFixCode(require(fields, TAG_SIDE).charAt(0));
        OrderType ordType  = OrderType.fromFixCode(require(fields, TAG_ORD_TYPE).charAt(0));
        double    orderQty = Double.parseDouble(require(fields, TAG_ORDER_QTY));

        Order.Builder builder = new Order.Builder(orderId, side, ordType, orderQty)
                .clOrdId(clOrdId)
                .symbol(symbol);

        if (ordType == OrderType.LIMIT) {
            double px = Double.parseDouble(require(fields, TAG_PRICE));
            builder.price(Price.toLong(px, priceScale));
        }

        String tifStr = fields.get(TAG_TIME_IN_FORCE);
        if (tifStr != null && !tifStr.isEmpty()) {
            builder.timeInForce(TimeInForce.fromFixCode(tifStr.charAt(0)));
        }

        return builder.build();
    }

    private static String require(Map<Integer, String> fields, int tag) {
        String v = fields.get(tag);
        if (v == null || v.isEmpty())
            throw new IllegalArgumentException("Missing required FIX tag: " + tag);
        return v;
    }
}