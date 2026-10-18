package com.shopfront.orders.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the confirmation once the order's transaction has committed, off the request thread,
 * so a slow mail gateway doesn't slow down checkout.
 */
@Component
public class OrderConfirmationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationListener.class);

    private final NotificationClient notificationClient;

    public OrderConfirmationListener(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Async
    @TransactionalEventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        try {
            notificationClient.sendOrderConfirmation(event);
        } catch (RuntimeException e) {
            // Never fail the order for a notification. Support can resend from the admin tool.
            log.warn("Could not send confirmation for order {}", event.orderId(), e);
        }
    }
}
