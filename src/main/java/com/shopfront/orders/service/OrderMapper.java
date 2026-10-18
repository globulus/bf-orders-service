package com.shopfront.orders.service;

import com.shopfront.orders.api.dto.OrderResponse;
import com.shopfront.orders.api.dto.OrderSummaryDto;
import com.shopfront.orders.domain.Order;
import com.shopfront.orders.domain.OrderLine;

final class OrderMapper {

    private OrderMapper() {
    }

    static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getLines().stream().map(OrderMapper::toLine).toList(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    static OrderSummaryDto toSummary(Order order) {
        int itemCount = order.getLines().stream().mapToInt(OrderLine::getQuantity).sum();
        return new OrderSummaryDto(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                itemCount,
                order.getCreatedAt());
    }

    private static OrderResponse.OrderLineResponse toLine(OrderLine line) {
        return new OrderResponse.OrderLineResponse(
                line.getProductId(),
                line.getProductName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.lineTotal());
    }
}
