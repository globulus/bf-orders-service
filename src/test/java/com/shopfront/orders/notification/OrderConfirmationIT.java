package com.shopfront.orders.notification;

import com.shopfront.orders.api.dto.CreateOrderRequest;
import com.shopfront.orders.api.dto.OrderLineRequest;
import com.shopfront.orders.api.dto.OrderResponse;
import com.shopfront.orders.catalog.CatalogClient;
import com.shopfront.orders.catalog.CatalogProduct;
import com.shopfront.orders.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OrderConfirmationIT {

    @Autowired
    OrderService orderService;

    @Autowired
    FakeMailGateway mailGateway;

    @MockitoBean
    CatalogClient catalogClient;

    @Test
    void confirmationIsSentOnceOrderIsPlaced() throws InterruptedException {
        when(catalogClient.findProduct(5L)).thenReturn(Optional.of(new CatalogProduct(5L, "MUG-01", "Mug", true,
                new CatalogProduct.Price(new BigDecimal("8.00"), "EUR"))));

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest("cust-mail", List.of(new OrderLineRequest(5L, 1))));

        // The listener runs async after commit; give it a moment.
        Thread.sleep(200);

        assertThat(mailGateway.sentOrderIds()).contains(order.id());
    }

    @TestConfiguration
    static class MailGatewayConfig {

        @Bean
        @Primary
        FakeMailGateway fakeMailGateway() {
            return new FakeMailGateway();
        }
    }

    /** Stands in for the mail gateway, including its latency (the real one answers in 0-250 ms). */
    static class FakeMailGateway implements NotificationClient {

        private final List<Long> sent = new CopyOnWriteArrayList<>();

        @Override
        public void sendOrderConfirmation(OrderPlacedEvent event) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(0, 250));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            sent.add(event.orderId());
        }

        List<Long> sentOrderIds() {
            return sent;
        }
    }
}
