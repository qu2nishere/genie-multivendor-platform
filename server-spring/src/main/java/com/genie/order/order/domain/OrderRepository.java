package com.genie.order.order.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCheckoutSessionIdOrderByIdAsc(Long checkoutSessionId);
}
