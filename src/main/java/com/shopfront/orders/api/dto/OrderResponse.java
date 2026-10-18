package com.shopfront.orders.api.dto;

import com.shopfront.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerId,
        OrderStatus status,
        String currency,
        BigDecimal totalAmount,
        List<OrderLineResponse> lines,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt) {

    public record OrderLineResponse(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }
}
