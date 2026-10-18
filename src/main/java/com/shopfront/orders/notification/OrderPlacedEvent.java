package com.shopfront.orders.notification;

import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, String customerId, BigDecimal totalAmount, String currency) {
}
