package com.genie.order.checkout.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {
}
