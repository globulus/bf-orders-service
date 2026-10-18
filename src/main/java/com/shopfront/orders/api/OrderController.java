package com.shopfront.orders.api;

import com.shopfront.orders.api.dto.CancelOrderRequest;
import com.shopfront.orders.api.dto.CreateOrderRequest;
import com.shopfront.orders.api.dto.OrderResponse;
import com.shopfront.orders.api.dto.OrderSummaryDto;
import com.shopfront.orders.api.dto.PageResponse;
import com.shopfront.orders.domain.OrderStatus;
import com.shopfront.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order", description = "Prices every line at the catalog's current price.")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List orders, newest first")
    public PageResponse<OrderSummaryDto> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.listOrders(customerId, status, page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Only orders that have not shipped can be cancelled.")
    public OrderResponse cancelOrder(@PathVariable Long id,
                                     @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return orderService.cancelOrder(id, request == null ? null : request.reason());
    }

    // Called by the warehouse integration when the parcel leaves. Not used by the web or the app.
    @PostMapping("/{id}/ship")
    public OrderResponse markShipped(@PathVariable Long id) {
        return orderService.markShipped(id);
    }
}
