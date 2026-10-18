package com.shopfront.orders.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// TODO(SHOP-412): replace with the notification-service client once its API is stable.
@Component
public class LoggingNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationClient.class);

    @Override
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        log.info("Order confirmation for order {} (customer {}): {} {}",
                event.orderId(), event.customerId(), event.totalAmount(), event.currency());
    }
}
