package com.shopfront.orders.notification;

public interface NotificationClient {

    void sendOrderConfirmation(OrderPlacedEvent event);
}
