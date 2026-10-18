package com.shopfront.orders.domain;

import com.shopfront.orders.service.InvalidOrderStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-03-02T10:15:30Z");

    @Test
    void totalIsSumOfLineTotals() {
        Order order = new Order();
        order.addLine(new OrderLine(1L, "Espresso beans 1kg", 2, new BigDecimal("18.90")));
        order.addLine(new OrderLine(2L, "Milk jug", 1, new BigDecimal("12.50")));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("50.30");
    }

    @Test
    void lineKeepsBackReferenceToOrder() {
        Order order = new Order();
        OrderLine line = new OrderLine(1L, "Tamper", 1, new BigDecimal("29.00"));

        order.addLine(line);

        assertThat(line.getOrder()).isSameAs(order);
    }

    @Test
    void placedOrderCanBeCancelled() {
        Order order = new Order();

        order.cancel("Changed my mind", NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("Changed my mind");
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void shippedOrderCannotBeCancelled() {
        Order order = new Order();
        order.markShipped();

        assertThatThrownBy(() -> order.cancel(null, NOW))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("SHIPPED");
    }

    @Test
    void cancelledOrderCannotBeCancelledAgain() {
        Order order = new Order();
        order.cancel(null, NOW);

        assertThatThrownBy(() -> order.cancel(null, NOW)).isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void cancelledOrderCannotShip() {
        Order order = new Order();
        order.cancel(null, NOW);

        assertThatThrownBy(order::markShipped).isInstanceOf(InvalidOrderStateException.class);
    }
}
