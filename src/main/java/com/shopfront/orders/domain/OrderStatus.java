package com.shopfront.orders.domain;

public enum OrderStatus {
    PLACED,
    SHIPPED,
    CANCELLED;

    public boolean isCancellable() {
        return this == PLACED;
    }
}
