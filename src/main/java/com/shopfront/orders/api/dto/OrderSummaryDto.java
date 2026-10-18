package com.shopfront.orders.api.dto;

import com.shopfront.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** One row in the order list. Lines are left out; fetch the order for them. */
public record OrderSummaryDto(
        Long id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        int itemCount,
        Instant createdAt) {
}
