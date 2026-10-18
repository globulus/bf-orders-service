package com.shopfront.orders.service;

import com.shopfront.orders.api.dto.CreateOrderRequest;
import com.shopfront.orders.api.dto.OrderLineRequest;
import com.shopfront.orders.api.dto.OrderResponse;
import com.shopfront.orders.catalog.CatalogClient;
import com.shopfront.orders.catalog.CatalogProduct;
import com.shopfront.orders.domain.Order;
import com.shopfront.orders.domain.OrderStatus;
import com.shopfront.orders.notification.OrderPlacedEvent;
import com.shopfront.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    CatalogClient catalogClient;
    @Mock
    ApplicationEventPublisher events;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:15:30Z"), ZoneOffset.UTC);
        orderService = new OrderService(orderRepository, catalogClient, events, clock);
    }

    @Test
    void createOrder_pricesLinesFromCatalog() {
        givenProduct(10L, "Grinder", "149.00", "EUR");
        givenProduct(11L, "Filter papers", "4.20", "EUR");
        givenSaveAssignsId(501L);

        OrderResponse order = orderService.createOrder(new CreateOrderRequest("cust-1",
                List.of(new OrderLineRequest(10L, 1), new OrderLineRequest(11L, 3))));

        assertThat(order.id()).isEqualTo(501L);
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.currency()).isEqualTo("EUR");
        assertThat(order.totalAmount()).isEqualByComparingTo("161.60");
        assertThat(order.lines()).extracting(OrderResponse.OrderLineResponse::productName)
                .containsExactly("Grinder", "Filter papers");
    }

    @Test
    void createOrder_publishesOrderPlacedEvent() {
        givenProduct(10L, "Grinder", "149.00", "EUR");
        givenSaveAssignsId(502L);

        orderService.createOrder(new CreateOrderRequest("cust-1", List.of(new OrderLineRequest(10L, 1))));

        ArgumentCaptor<OrderPlacedEvent> event = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().orderId()).isEqualTo(502L);
        assertThat(event.getValue().totalAmount()).isEqualByComparingTo("149.00");
    }

    @Test
    void createOrder_trimsCustomerId() {
        givenProduct(10L, "Grinder", "149.00", "EUR");
        givenSaveAssignsId(503L);

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest("  cust-7 ", List.of(new OrderLineRequest(10L, 1))));

        assertThat(order.customerId()).isEqualTo("cust-7");
    }

    // See docs/KNOWN_ISSUES.md, "Duplicate product lines lose quantity".
    @Test
    @Tag("known-bug")
    void createOrder_sumsQuantitiesOfDuplicateProductLines() {
        givenProduct(10L, "Grinder", "149.00", "EUR");
        givenSaveAssignsId(504L);

        OrderResponse order = orderService.createOrder(new CreateOrderRequest("cust-1",
                List.of(new OrderLineRequest(10L, 2), new OrderLineRequest(10L, 3))));

        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().get(0).quantity()).isEqualTo(5);
        assertThat(order.totalAmount()).isEqualByComparingTo("745.00");
    }

    @Test
    void createOrder_rejectsUnknownProduct() {
        when(catalogClient.findProduct(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(
                new CreateOrderRequest("cust-1", List.of(new OrderLineRequest(99L, 1)))))
                .isInstanceOf(ProductUnavailableException.class)
                .hasMessageContaining("does not exist");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_rejectsInactiveProduct() {
        when(catalogClient.findProduct(12L)).thenReturn(Optional.of(new CatalogProduct(12L, "OLD-1", "Old kettle",
                false, new CatalogProduct.Price(new BigDecimal("30.00"), "EUR"))));

        assertThatThrownBy(() -> orderService.createOrder(
                new CreateOrderRequest("cust-1", List.of(new OrderLineRequest(12L, 1)))))
                .isInstanceOf(ProductUnavailableException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createOrder_rejectsMixedCurrencies() {
        givenProduct(10L, "Grinder", "149.00", "EUR");
        givenProduct(20L, "Scale", "39.00", "GBP");

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest("cust-1",
                List.of(new OrderLineRequest(10L, 1), new OrderLineRequest(20L, 1)))))
                .isInstanceOf(ProductUnavailableException.class)
                .hasMessageContaining("GBP");
    }

    @Test
    void cancelOrder_unknownIdThrowsNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(404L, null)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_usesClockForCancelledAt() {
        Order order = new Order();
        order.setId(7L);
        order.setCustomerId("cust-1");
        order.setCurrency("EUR");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelOrder(7L, "Duplicate order");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(Instant.parse("2026-03-02T10:15:30Z"));
    }

    private void givenProduct(Long id, String name, String price, String currency) {
        when(catalogClient.findProduct(id)).thenReturn(Optional.of(
                new CatalogProduct(id, "SKU-" + id, name, true, new CatalogProduct.Price(new BigDecimal(price), currency))));
    }

    private void givenSaveAssignsId(Long id) {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(id);
            return order;
        });
    }
}
