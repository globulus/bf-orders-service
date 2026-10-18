package com.shopfront.orders.repository;

import com.shopfront.orders.domain.Order;
import com.shopfront.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select o from Order o
            where (:customerId is null or o.customerId = :customerId)
              and (:status is null or o.status = :status)
            """)
    Page<Order> search(@Param("customerId") String customerId,
                       @Param("status") OrderStatus status,
                       Pageable pageable);
}
