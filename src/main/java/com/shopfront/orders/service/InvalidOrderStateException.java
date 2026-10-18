package com.shopfront.orders.service;

import com.shopfront.orders.domain.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(Long orderId, OrderStatus status, String action) {
        super("Cannot " + action + " order " + orderId + " in status " + status);
    }
}
