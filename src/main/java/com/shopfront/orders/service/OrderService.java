package com.shopfront.orders.service;

import com.shopfront.orders.api.dto.CreateOrderRequest;
import com.shopfront.orders.api.dto.OrderLineRequest;
import com.shopfront.orders.api.dto.OrderResponse;
import com.shopfront.orders.api.dto.OrderSummaryDto;
import com.shopfront.orders.api.dto.PageResponse;
import com.shopfront.orders.catalog.CatalogClient;
import com.shopfront.orders.catalog.CatalogProduct;
import com.shopfront.orders.domain.Order;
import com.shopfront.orders.domain.OrderLine;
import com.shopfront.orders.domain.OrderStatus;
import com.shopfront.orders.notification.OrderPlacedEvent;
import com.shopfront.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository,
                        CatalogClient catalogClient,
                        ApplicationEventPublisher events,
                        Clock clock) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // One line per product: the web cart can send the same product twice.
        Map<Long, Integer> quantities = request.lines().stream()
                .collect(Collectors.toMap(
                        OrderLineRequest::productId,
                        OrderLineRequest::quantity,
                        (first, second) -> second,
                        LinkedHashMap::new));

        Order order = new Order();
        order.setCustomerId(request.customerId().trim());

        quantities.forEach((productId, quantity) -> {
            CatalogProduct product = catalogClient.findProduct(productId)
                    .orElseThrow(() -> new ProductUnavailableException(productId, "does not exist"));
            if (!product.active()) {
                throw new ProductUnavailableException(productId, "is not available");
            }
            if (product.price() == null) {
                throw new ProductUnavailableException(productId, "has no current price");
            }
            String currency = product.price().currency();
            if (order.getCurrency() == null) {
                order.setCurrency(currency);
            } else if (!order.getCurrency().equals(currency)) {
                throw new ProductUnavailableException(productId,
                        "is priced in " + currency + " but the order is in " + order.getCurrency());
            }
            order.addLine(new OrderLine(productId, product.name(), quantity, product.price().amount()));
        });

        Order saved = orderRepository.save(order);
        log.info("Placed order {} for customer {} ({} lines, {} {})", saved.getId(), saved.getCustomerId(),
                saved.getLines().size(), saved.getTotalAmount(), saved.getCurrency());
        events.publishEvent(new OrderPlacedEvent(saved.getId(), saved.getCustomerId(),
                saved.getTotalAmount(), saved.getCurrency()));
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> listOrders(String customerId, OrderStatus status, int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        String customerFilter = (customerId == null || customerId.isBlank()) ? null : customerId.trim();
        return PageResponse.of(orderRepository.search(customerFilter, status, pageable), OrderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderMapper.toResponse(find(id));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, String reason) {
        Order order = find(id);
        order.cancel(reason, clock.instant());
        log.info("Cancelled order {}{}", id, reason == null ? "" : ": " + reason);
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse markShipped(Long id) {
        Order order = find(id);
        order.markShipped();
        return OrderMapper.toResponse(order);
    }

    private Order find(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
